/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.objects.proxy;

import dev.vankka.dynamicproxy.processor.Original;
import dev.vankka.dynamicproxy.processor.Proxy;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;
import java.util.UUID;

@Proxy(CommandSender.class)
public abstract class CommandSenderDynamic implements CommandSender {

    @Original
    private final CommandSender original;
    private final GuildMessageReceivedEvent event;

    public CommandSenderDynamic(CommandSender original, GuildMessageReceivedEvent event) {
        this.original = original;
        this.event = event;
    }

    private boolean alreadyQueuedDelete = false;

    private StringJoiner messageBuffer = new StringJoiner("\n");
    private boolean bufferCollecting = false;

    private void doSend(String message) {
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

    private void doSend(ComponentLike componentLike) {
        doSend(BukkitComponentSerializer.legacy().serialize(componentLike.asComponent()));
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        original.sendMessage(message);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull ComponentLike message) {
        original.sendMessage(message);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull Identity source, @NotNull Component message) {
        original.sendMessage(source, message);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull Component message, @NotNull MessageType type) {
        original.sendMessage(message, type);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull Component message) {
        original.sendMessage(source, message);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull Identity source, @NotNull ComponentLike message) {
        original.sendMessage(source, message);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull ComponentLike message, @NotNull MessageType type) {
        original.sendMessage(message, type);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull ComponentLike message) {
        original.sendMessage(source, message);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull Component message, @NotNull MessageType type) {
        original.sendMessage(source, message, type);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull Identity source, @NotNull ComponentLike message, @NotNull MessageType type) {
        original.sendMessage(source, message, type);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull Identified source, @NotNull ComponentLike message, @NotNull MessageType type) {
        original.sendMessage(source, message, type);
        doSend(message);
    }

    @Override
    public void sendMessage(@NotNull String s) {
        original.sendMessage(s);
        doSend(s);
    }

    @Override
    public void sendMessage(@NotNull String[] strings) {
        original.sendMessage(strings);
        for (String string : strings) {
            doSend(string);
        }
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {
        original.sendMessage(s);
        doSend(s);
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String[] strings) {
        original.sendMessage(strings);
        for (String string : strings) {
            doSend(string);
        }
    }

    @Override
    public void sendMessage(@NotNull Identity identity, @NotNull Component message, @NotNull MessageType type) {
        original.sendMessage(identity, message, type);
        doSend(message);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(@NotNull BaseComponent... components) {
        original.sendMessage(components);
        doSend(BungeeComponentSerializer.get().deserialize(components));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(@NotNull BaseComponent component) {
        original.sendMessage(component);
        doSend(BungeeComponentSerializer.get().deserialize(new BaseComponent[] {component}));
    }
}
