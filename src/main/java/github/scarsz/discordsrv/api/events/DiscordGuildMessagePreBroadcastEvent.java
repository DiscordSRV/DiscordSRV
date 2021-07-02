/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
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
 * END
 */

package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * <p>If there are no chat hooks found this event is called directly before a message from Discord is processed
 * and broadcasted to the Minecraft server</p>
 *
 * <p>At the time this event is called, the message, channel, and recipients can still be changed</p>
 */
public class DiscordGuildMessagePreBroadcastEvent extends Event {

    @Getter @Setter private String channel;
    @Getter @Setter private Component message;
    @Getter private final List<? extends CommandSender> recipients;

    public DiscordGuildMessagePreBroadcastEvent(String channel, Component message, List<? extends CommandSender> recipients) {
        this.channel = channel;
        this.message = message;
        this.recipients = recipients;
    }
}
