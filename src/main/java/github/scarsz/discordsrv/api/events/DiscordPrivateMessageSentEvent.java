/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;

/**
 * <p>Called directly after a message is sent to a {@link PrivateChannel} by the bot</p>
 */
public class DiscordPrivateMessageSentEvent extends DiscordEvent {

    @Getter private final PrivateChannel channel;
    @Getter private final Message message;
    @Getter private final User recipient;

    public DiscordPrivateMessageSentEvent(JDA jda, Message message) {
        super(jda);
        this.channel = message.getPrivateChannel();
        this.message = message;
        this.recipient = channel.getUser();
    }

}
