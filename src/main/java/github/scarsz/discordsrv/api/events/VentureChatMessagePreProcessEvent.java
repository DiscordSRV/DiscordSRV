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

import org.bukkit.event.Cancellable;

import github.scarsz.discordsrv.util.MessageUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import venture.Aust1n46.chat.api.events.VentureChatEvent;
import org.bukkit.event.Cancellable;


/**
 * <p>
 * Called before DiscordSRV has processed a VentureChat message from Bungee
 * (when the VentureChatBungee config option is enabled), modifications may be
 * overwritten by DiscordSRV's processing.
 * </p>
 *
 * <p>
 * At the time this event is called, {@link #getMessage()} would return what the
 * person <i>said</i>, not the final message. You could change what they said
 * using the {@link #setMessage(String)} method or use
 * {@link #setCancelled(boolean)} to cancel it from being processed altogether
 * </p>
 */
@Getter
@Setter
public class VentureChatMessagePreProcessEvent extends VentureChatMessageEvent implements Cancellable {
	private boolean cancelled;
	private String channel;
	private Component messageComponent;

	public VentureChatMessagePreProcessEvent(String channel, Component message, VentureChatEvent ventureChatEvent) {
		super(ventureChatEvent);
		this.channel = channel;
		this.messageComponent = message;
	}

	@Deprecated
	public VentureChatMessagePreProcessEvent(String channel, String message, VentureChatEvent ventureChatEvent) {
		this(channel, MessageUtil.toComponent(message, true), ventureChatEvent);
	}

	@Deprecated
	public String getMessage() {
		return MessageUtil.toLegacy(messageComponent);
	}

	@Deprecated
	public void setMessage(String legacy) {
		this.messageComponent = MessageUtil.toComponent(legacy, true);
	}
}
