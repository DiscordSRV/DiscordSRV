/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.StringJoiner;

public class DiscordChatChannelCommandFeedbackForwarder {

    private final GuildMessageReceivedEvent event;

    private StringJoiner messageBuffer = new StringJoiner("\n");

    private boolean bufferCollecting = false;

    public DiscordChatChannelCommandFeedbackForwarder(GuildMessageReceivedEvent event) {
        this.event = event;

        // expire request message after specified time
        if (DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") > 0 && DiscordSRV.config().getBoolean("DiscordChatChannelConsoleCommandExpirationDeleteRequest")) {
            SchedulerUtil.runTaskLaterAsynchronously(DiscordSRV.getPlugin(), () -> {
                event.getMessage().delete().queue();
            }, DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") * 20L); // Seconds to ticks
        }
    }

    public void send(String message) {
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
            SchedulerUtil.runTaskLater(DiscordSRV.getPlugin(), () -> { // Collect messages for 3 ticks, then send
                this.bufferCollecting = false;
                if (this.messageBuffer.length() == 0) return; // There's nothing in the buffer to send, leave it
                DiscordUtil.sendMessage(event.getChannel(), DiscordUtil.escapeMarkdown(this.messageBuffer.toString()), DiscordSRV.config().getInt("DiscordChatChannelConsoleCommandExpiration") * 1000);
                this.messageBuffer = new StringJoiner("\n");
            }, 3L);
        }
    }

}
