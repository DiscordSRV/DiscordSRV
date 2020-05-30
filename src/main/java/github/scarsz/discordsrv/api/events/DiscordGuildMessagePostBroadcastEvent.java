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

/**
 * <p>Called directly after a Discord message was processed and was broadcasted to the server</p>
 *
 * <p>At the time this event is called, {@link #getProcessedMessage()} would return what the final message
 * would look like in-game, including text like the author before the actual message</p>
 */
public class DiscordGuildMessagePostBroadcastEvent extends Event {

    @Getter private final String channel;
    @Getter private final String processedMessage;

    public DiscordGuildMessagePostBroadcastEvent(String channel, String processedMessage) {
        this.channel = channel;
        this.processedMessage = processedMessage;
    }

}
