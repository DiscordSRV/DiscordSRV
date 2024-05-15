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

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/**
 * <p>Called after DiscordSRV has processed a Minecraft chat message but before being sent to Discord.
 * Modification is allow and will effect the message sent to Discord.</p>
 *
 * <p>If a messages is coming from VentureChat over Bungee then {@link VentureChatMessagePostProcessEvent} would be called instead, due to the lack of the Player object</p>
 */
public class GameChatMessagePostProcessEvent extends GameEvent<Event> implements Cancellable {

    private boolean cancelled;

    private String channel;
    private String processedMessage;

    public GameChatMessagePostProcessEvent(String channel, String processedMessage, Player player, boolean cancelled, Event event) {
        super(player, event);
        this.channel = channel;
        this.processedMessage = processedMessage;
        setCancelled(cancelled);
    }

    @Deprecated
    public GameChatMessagePostProcessEvent(String channel, String processedMessage, Player player, boolean cancelled) {
        this(channel, processedMessage, player, cancelled, null);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public String getChannel() {
        return this.channel;
    }

    public String getProcessedMessage() {
        return this.processedMessage;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setProcessedMessage(String processedMessage) {
        this.processedMessage = processedMessage;
    }
}
