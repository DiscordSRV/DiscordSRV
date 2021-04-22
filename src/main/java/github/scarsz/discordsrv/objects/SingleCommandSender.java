/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.objects;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
public class SingleCommandSender implements ConsoleCommandSender {

    private GuildMessageReceivedEvent event;
    private ConsoleCommandSender sender;

    public SingleCommandSender(GuildMessageReceivedEvent event, ConsoleCommandSender consoleCommandSender) {
        this.event = event;
        this.sender = consoleCommandSender;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0) {
        return sender.addAttachment(arg0);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, int arg1) {
        return sender.addAttachment(arg0, arg1);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2) {
        return sender.addAttachment(arg0, arg1, arg2);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2, int arg3) {
        return sender.addAttachment(arg0, arg1, arg2, arg3);
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return sender.getEffectivePermissions();
    }

    @Override
    public boolean hasPermission(String arg0) {
        return sender.hasPermission(arg0);
    }

    @Override
    public boolean hasPermission(Permission arg0) {
        return sender.hasPermission(arg0);
    }

    @Override
    public boolean isPermissionSet(String arg0) {
        return sender.isPermissionSet(arg0);
    }

    @Override
    public boolean isPermissionSet(Permission arg0) {
        return sender.isPermissionSet(arg0);
    }

    @Override
    public void recalculatePermissions() {
        sender.recalculatePermissions();
    }

    @Override
    public void removeAttachment(PermissionAttachment arg0) {
        sender.removeAttachment(arg0);
    }

    @Override
    public boolean isOp() {
        return sender.isOp();
    }

    @Override
    public void setOp(boolean arg0) {
        sender.setOp(arg0);
    }

    @Override
    public String getName() {
        return sender.getName();
    }

    @Override
    public Server getServer() {
        return sender.getServer();
    }

    private boolean alreadyQueuedDelete = false;

    private StringJoiner messageBuffer = new StringJoiner("\n");
    private boolean bufferCollecting = false;

    // To prevent spam and potential rate-limiting when getting multi-line command responses, responses will be grouped together and sent in as few messages as is practical.
    @Override
    public void sendMessage(String message) {
        if (this.bufferCollecting) { // If the buffer has started collecting messages, we should just add this one to it.
            if (DiscordUtil.escapeMarkdown(this.messageBuffer + "\n" + message).length() > 1998) { // If the message will be too long (allowing for markdown escaping and the newline)
                // Send the message, then clear the buffer and add this message to the empty buffer
                DiscordUtil.sendMessage(event.getChannel(), DiscordUtil.escapeMarkdown(this.messageBuffer.toString()), DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") * 1000);
                this.messageBuffer = new StringJoiner("\n");
                this.messageBuffer.add(message);
            } else { // If adding this message to the buffer won't send it over the 2000 character limit
                this.messageBuffer.add(message);
            }
        } else { // Messages aren't currently being collected, let's start doing that
            this.bufferCollecting = true;
            this.messageBuffer.add(message); // This message is the first one in the buffer
            Bukkit.getScheduler().runTaskLater(DiscordSRV.getPlugin(), () -> { // Collect messages for 3 ticks, then send
                this.bufferCollecting = false;
                if (this.messageBuffer.length() == 0) return; // There's nothing in the buffer to send, leave it
                DiscordUtil.sendMessage(event.getChannel(), DiscordUtil.escapeMarkdown(this.messageBuffer.toString()), DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") * 1000);
                this.messageBuffer = new StringJoiner("\n");
            }, 3L);
        }


        // expire request message after specified time
        if (!alreadyQueuedDelete && DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") > 0 && DiscordSRV.config().getBoolean("DiscordChatChannelConsoleCommandExpirationDeleteRequest")) {
            Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
                try { Thread.sleep(DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") * 1000L); } catch (InterruptedException ignored) {}
                event.getMessage().delete().queue();
                alreadyQueuedDelete = true;
            });
        }
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String msg : messages)
            sendMessage(msg);
    }

    // Paper
    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {
        sendMessage(s);
    }

    // Paper
    public void sendMessage(@Nullable UUID uuid, @NotNull String[] strings) {
        sendMessage(strings);
    }

    @Override
    public void abandonConversation(Conversation arg0) {
        sender.abandonConversation(arg0);
    }

    @Override
    public void abandonConversation(Conversation arg0, ConversationAbandonedEvent arg1) {
        sender.abandonConversation(arg0, arg1);
    }

    @Override
    public void acceptConversationInput(String arg0) {
        sender.acceptConversationInput(arg0);
    }

    @Override
    public boolean beginConversation(Conversation arg0) {
        return sender.beginConversation(arg0);
    }

    @Override
    public boolean isConversing() {
        return sender.isConversing();
    }

    public void sendRawMessage(String arg0) {
        sender.sendRawMessage(arg0);
    }

    // Paper
    public void sendRawMessage(@Nullable UUID uuid, @NotNull String s) {
        sender.sendRawMessage(uuid, s);
    }

    @SuppressWarnings("ConstantConditions")
    public org.bukkit.command.ConsoleCommandSender.Spigot spigot() {
        try {
            return (org.bukkit.command.ConsoleCommandSender.Spigot) CommandSender.class.getMethod("spigot").invoke(sender);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            DiscordSRV.error(e);
            return null;
        }
    }
}
