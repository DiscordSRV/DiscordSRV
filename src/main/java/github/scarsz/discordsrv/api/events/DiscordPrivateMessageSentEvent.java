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

package github.scarsz.discordsrv.api.events;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * <p>Called directly after a message is sent to a {@link PrivateChannel} by the bot</p>
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public class DiscordPrivateMessageSentEvent extends DiscordEvent {

    private final PrivateChannel channel;
    private final Message message;
    private final User recipient;

    public DiscordPrivateMessageSentEvent(JDA jda, Message message) {
        super(jda);
        this.channel = message.getPrivateChannel();
        this.message = message;
        this.recipient = channel.getUser();
    }

    public PrivateChannel getChannel() {
        return this.channel;
    }

    public Message getMessage() {
        return this.message;
    }

    public User getRecipient() {
        return this.recipient;
    }
}
