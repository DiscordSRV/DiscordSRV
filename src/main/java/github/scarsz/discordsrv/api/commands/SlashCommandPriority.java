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
 * <p>Defaults to {@link #NORMAL} in {@link PluginSlashCommand} where it's not specifically set</p>
 * <p>Defaults to {@link #NORMAL} in {@link SlashCommand} annotations where it's not specifically set</p>
 */
public enum SlashCommandPriority {

    /**
     * <p>When in {@link PluginSlashCommand} - Slash command should always be prioritized before every other</p>
     * <p>When in {@link SlashCommand} - Slash command handler should always be fired first, taking precedent over others</p>
     */
    FIRST(0),
    /**
     * <p>When in {@link PluginSlashCommand} - Slash command is important and should not be overridden by others</p>
     * <p>When in {@link SlashCommand} - Slash command handler is important and should not be overtaken by others</p>
     */
    EARLY(1),
    /**
     * <p>When in {@link PluginSlashCommand} - Slash command is neither important nor unimportant, and may be prioritized normally</p>
     * <p>When in {@link SlashCommand} - Slash command handler is neither important nor unimportant, and may be prioritized normally</p>
     */
    NORMAL(2),
    /**
     * <p>When in {@link PluginSlashCommand} - Slash command is not important and may be overridden by others</p>
     * <p>When in {@link SlashCommand} - Slash command handler is not important and may give way to other handlers who wishes to overtake</p>
     */
    LATE(3),
    /**
     * <p>When in {@link PluginSlashCommand} - Slash command should be overridden by any other if other conflicting commands exist</p>
     * <p>When in {@link SlashCommand} - Slash command handler should always be overtaken if any other handlers wishes to </p>
     */
    LAST(4);

    @Getter private final int slot;

    SlashCommandPriority(int slot) {
        this.slot = slot;
    }

}
