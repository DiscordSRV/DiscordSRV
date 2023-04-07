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

import github.scarsz.discordsrv.util.MessageUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;

/**
 * <p>Called directly after a message from Discord was processed and was broadcasted to the Minecraft server</p>
 *
 * <p>At the time this event is called, {@link #getMessage()} would return what the final message
 * would look like in-game, including text like the author before the actual message</p>
 */
public class DiscordGuildMessagePostBroadcastEvent extends Event {

    @Getter private final String channel;
    @Getter private final Component message;

    @Deprecated
    public DiscordGuildMessagePostBroadcastEvent(String channel, String processedMessage) {
        this.channel = channel;
        this.message = MessageUtil.toComponent(processedMessage);
    }

    public DiscordGuildMessagePostBroadcastEvent(String channel, Component message) {
        this.channel = channel;
        this.message = message;
    }

    @Deprecated
    public String getProcessedMessage() {
        return MessageUtil.toLegacy(message);
    }

}
