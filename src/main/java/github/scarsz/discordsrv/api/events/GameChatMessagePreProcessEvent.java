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
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * <p>Called directly after a Discord message was processed but before being broadcasted to the server</p>
 *
 * <p>At the time this event is called, {@link #getMessage()} would return what the person <i>said</i>, not
 * the final message. You could change what they said using the {@link #setMessage(String)} method or use
 * {@link #setCancelled(boolean)} to cancel it from being processed altogether</p>
 */
public class GameChatMessagePreProcessEvent extends GameEvent implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter @Setter private String channel;
    @Getter @Setter private String message;

    public GameChatMessagePreProcessEvent(String channel, String message, Player player) {
        super(player);
        this.channel = channel;
        this.message = message;
    }

}
