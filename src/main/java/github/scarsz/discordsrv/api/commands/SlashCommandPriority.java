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

package github.scarsz.discordsrv.api.commands;

import lombok.Getter;

/**
 * <p>Used to indicate the registration priority of slash commands and the invocation order of slash command handlers</p>
 * <p>Defaults to {@link #NORMAL} in {@link SlashCommand} annotations where it's not specifically set</p>
 * <p>Defaults to {@link #NORMAL} in {@link PluginSlashCommand} where it's not specifically set</p>
 */
public enum SlashCommandPriority {

    /**
     * Slash command should be prioritized before every other
     */
    FIRST(0),
    /**
     * Slash command is important to not be overridden by others
     */
    EARLY(1),
    /**
     * Slash command is neither important nor unimportant, and may be prioritized
     * normally
     */
    NORMAL(2),
    /**
     * Slash command is not important and may be overridden by others
     */
    LATE(3),
    /**
     * Slash command should be overridden by any other if other plugin so chooses
     */
    LAST(4);

    @Getter private final int slot;

    SlashCommandPriority(int slot) {
        this.slot = slot;
    }

}
