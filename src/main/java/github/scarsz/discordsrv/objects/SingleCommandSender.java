/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.objects;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Set;

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

    @Override
    public void sendMessage(String message) {
        DiscordUtil.sendMessage(event.getChannel(), message, DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") * 1000, false);

        // expire request message after specified time
        if (!alreadyQueuedDelete && DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") > 0 && DiscordSRV.config().getBoolean("DiscordChatChannelConsoleCommandExpirationDeleteRequest")) {
            Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
                try { Thread.sleep(DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") * 1000); } catch (InterruptedException e) { e.printStackTrace(); }
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

    @Override
    public void sendRawMessage(String arg0) {
        sender.sendRawMessage(arg0);
    }

}
