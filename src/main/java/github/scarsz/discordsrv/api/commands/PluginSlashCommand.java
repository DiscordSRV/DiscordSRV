/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
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

import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link CommandData} wrapper that includes the originating {@link Plugin}
 */
@Value
public class PluginSlashCommand {

    Plugin plugin;
    CommandData commandData;
    Set<String> guilds = new HashSet<>();

    /**
     * Construct data for a new plugin-originating slash command
     * @param plugin the owning plugin
     * @param commandData the built command data
     */
    public PluginSlashCommand(Plugin plugin, CommandData commandData) {
        this(plugin, commandData, (String[]) null);
    }
    /**
     * Construct data for a new plugin-originating slash command
     * @param plugin the owning plugin
     * @param commandData the built command data
     * @param guildIds the applicable guild IDs for this command. if not provided, command will be applicable to all guilds
     */
    public PluginSlashCommand(Plugin plugin, CommandData commandData, String... guildIds) {
        this.plugin = plugin;
        this.commandData = commandData;
        if (guildIds != null) Collections.addAll(guilds, guildIds);
    }

    /**
     * Whether this plugin command is applicable to the given guild
     * @param guild the guild to check for
     * @return whether the guild is applicable for this command
     */
    public boolean isApplicable(Guild guild) {
        return this.guilds.isEmpty() || this.guilds.contains(guild.getId());
    }
    /**
     * Add the given {@link Guild} to the list of guilds this command will be registered to, 
     * when none are provided the command will be registered to all guilds.
     * @param guild the guild to apply this command to
     * @return the {@link PluginSlashCommand} instance for chaining
     */
    public PluginSlashCommand addGuildFilter(Guild guild) {
        return addGuildFilter(guild.getId());
    }
    /**
     * Add the given {@link Guild} id to the list of guilds this command will be registered to, 
     * when none are provided the command will be registered to all guilds.
     * @param guildId the guild ID to apply this command to
     * @return the {@link PluginSlashCommand} instance for chaining
     */
    public PluginSlashCommand addGuildFilter(String guildId) {
        this.guilds.add(guildId);
        return this;
    }

}
