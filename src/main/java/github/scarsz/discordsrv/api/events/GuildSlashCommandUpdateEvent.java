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

import github.scarsz.discordsrv.api.Cancellable;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Set;

/**
 * <p>Called before slash command updates are sent to discord when
 * {@link github.scarsz.discordsrv.api.ApiManager#updateSlashCommands()} is called.
 * This event is called once for each {@link Guild}</p>
 *
 * <p>You could change what commands are included by modifying the {@link Set} of slash commands with
 * {@link #getCommands()} or use {@link #setCancelled(boolean)} to prevent all slash commands from
 * registering to this {@link Guild} altogether. Cancelling also leave slash commands originally registered in a
 * {@link Guild} untouched.</p>
 */
public class GuildSlashCommandUpdateEvent extends Event implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter private final Guild guild;
    @Getter private final Set<CommandData> commands;

    public GuildSlashCommandUpdateEvent(Guild guild, Set<CommandData> commands) {
        this.guild = guild;
        this.commands = commands;
    }

}
