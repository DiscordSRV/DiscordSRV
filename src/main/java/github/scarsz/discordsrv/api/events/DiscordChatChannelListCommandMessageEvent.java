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

package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * <p>Called before a playerlist command message response is sent to {@link TextChannel} by the bot</p>
 *
 * <p>To stop the command response from expiring, set it to 0 with {@link #setExpiration(int)}</p>
 * <p>You can modify the result of this event with {@link #setResult(Result)}</p>
 *
 * @see Result#SEND_RESPONSE
 * @see Result#NO_ACTION
 * @see Result#TREAT_AS_REGULAR_MESSAGE
 */
public class DiscordChatChannelListCommandMessageEvent extends Event {

    @Getter @Setter private Result result;

    @Getter private final TextChannel channel;
    @Getter private final Guild guild;
    @Getter private final String message;
    @Getter private final MessageReceivedEvent triggeringJDAEvent;

    @Getter @Setter private String playerListMessage;
    @Getter @Setter private int expiration;

    public DiscordChatChannelListCommandMessageEvent(TextChannel channel, Guild guild, String message, MessageReceivedEvent triggeringJDAEvent, String playerListMessage, int expiration, Result result) {
        this.channel = channel;
        this.guild = guild;
        this.message = message;
        this.triggeringJDAEvent = triggeringJDAEvent;
        this.playerListMessage = playerListMessage;
        this.expiration = expiration;
        this.result = result;
    }

    public enum Result {
        /**
         * <p>Send the player list response like normal.</p>
         */
        SEND_RESPONSE,

        /**
         * <p>Cancel the event, do not perform any action afterwards.</p>
         */
        NO_ACTION,

        /**
         * <p>Cancel the event, treat message as regular discord message for further processing.</p>
         * <p>(For example, be broadcasted in game)</p>
         */
        TREAT_AS_REGULAR_MESSAGE

    }

}
