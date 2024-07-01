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

package github.scarsz.discordsrv.api.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link CommandData} wrapper that includes the originating {@link Plugin}
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public final class PluginSlashCommand {

    private final Plugin plugin;
    private final CommandData commandData;
    private final Set<String> guilds = new HashSet<>();
    private SlashCommandPriority priority;

    /**
     * Construct data for a new plugin-originating slash command
     *
     * @param plugin      the owning plugin
     * @param commandData the built command data
     */
    public PluginSlashCommand(Plugin plugin, CommandData commandData) {
        this(plugin, commandData, SlashCommandPriority.NORMAL);
    }

    /**
     * Construct data for a new plugin-originating slash command
     *
     * @param plugin      the owning plugin
     * @param commandData the built command data
     * @param priority    the priority of this slash command
     */
    public PluginSlashCommand(Plugin plugin, CommandData commandData, SlashCommandPriority priority) {
        this(plugin, commandData, priority, (String[]) null);
    }

    /**
     * Construct data for a new plugin-originating slash command
     *
     * @param plugin      the owning plugin
     * @param commandData the built command data
     * @param guildIds    the applicable guild IDs for this command. if not provided, command will be applicable to all guilds
     */
    public PluginSlashCommand(Plugin plugin, CommandData commandData, String... guildIds) {
        this(plugin, commandData, SlashCommandPriority.NORMAL, guildIds);
    }

    /**
     * Construct data for a new plugin-originating slash command
     *
     * @param plugin      the owning plugin
     * @param commandData the built command data
     * @param priority    the priority of this slash command
     * @param guildIds    the applicable guild IDs for this command. if not provided, command will be applicable to all guilds
     */
    public PluginSlashCommand(Plugin plugin, CommandData commandData, SlashCommandPriority priority, String... guildIds) {
        this.plugin = plugin;
        this.commandData = commandData;
        this.priority = priority;
        if (guildIds != null) Collections.addAll(guilds, guildIds);
    }

    /**
     * Whether this plugin command is applicable to the given guild
     *
     * @param guild the guild to check for
     * @return whether the guild is applicable for this command
     */
    public boolean isApplicable(Guild guild) {
        if (guild == null) return false;
        return this.guilds.isEmpty() || this.guilds.contains(guild.getId());
    }

    /**
     * Add the given {@link Guild} to the list of guilds this command will be registered to,
     * when none are provided the command will be registered to all guilds.
     *
     * @param guild the guild to apply this command to
     * @return the {@link PluginSlashCommand} instance for chaining
     */
    public PluginSlashCommand addGuildFilter(Guild guild) {
        return addGuildFilter(guild.getId());
    }

    /**
     * Add the given {@link Guild} id to the list of guilds this command will be registered to,
     * when none are provided the command will be registered to all guilds.
     *
     * @param guildId the guild ID to apply this command to
     * @return the {@link PluginSlashCommand} instance for chaining
     */
    public PluginSlashCommand addGuildFilter(String guildId) {
        this.guilds.add(guildId);
        return this;
    }

    /**
     * Remove the given {@link Guild} from the list of guilds this command will be registered to,
     * when none are provided the command will be registered to all guilds.
     *
     * @param guild the guild to be removed
     * @return the {@link PluginSlashCommand} instance for chaining
     */
    public PluginSlashCommand removeGuildFilter(Guild guild) {
        return removeGuildFilter(guild.getId());
    }

    /**
     * Remove the given {@link Guild} id from the list of guilds this command will be registered to,
     * when none are provided the command will be registered to all guilds.
     *
     * @param guildId the guild ID of the guild to be removed
     * @return the {@link PluginSlashCommand} instance for chaining
     */
    public PluginSlashCommand removeGuildFilter(String guildId) {
        this.guilds.remove(guildId);
        return this;
    }

    /**
     * Set the priority of this slash command when handling registration conflicts
     *
     * @param priority the priority of this slash command
     * @return the {@link PluginSlashCommand} instance for chaining
     */
    public PluginSlashCommand setPriority(SlashCommandPriority priority) {
        this.priority = priority;
        return this;
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public CommandData getCommandData() {
        return this.commandData;
    }

    public Set<String> getGuilds() {
        return this.guilds;
    }

    public SlashCommandPriority getPriority() {
        return this.priority;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PluginSlashCommand)) return false;
        final PluginSlashCommand other = (PluginSlashCommand) o;
        final Object this$plugin = this.getPlugin();
        final Object other$plugin = other.getPlugin();
        if (this$plugin == null ? other$plugin != null : !this$plugin.equals(other$plugin)) return false;
        final Object this$commandData = this.getCommandData();
        final Object other$commandData = other.getCommandData();
        if (this$commandData == null ? other$commandData != null : !this$commandData.equals(other$commandData))
            return false;
        final Object this$guilds = this.getGuilds();
        final Object other$guilds = other.getGuilds();
        if (this$guilds == null ? other$guilds != null : !this$guilds.equals(other$guilds)) return false;
        final Object this$priority = this.getPriority();
        final Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $plugin = this.getPlugin();
        result = result * PRIME + ($plugin == null ? 43 : $plugin.hashCode());
        final Object $commandData = this.getCommandData();
        result = result * PRIME + ($commandData == null ? 43 : $commandData.hashCode());
        final Object $guilds = this.getGuilds();
        result = result * PRIME + ($guilds == null ? 43 : $guilds.hashCode());
        final Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        return result;
    }

    public String toString() {
        return "PluginSlashCommand(plugin=" + this.getPlugin() + ", commandData=" + this.getCommandData() + ", guilds=" + this.getGuilds() + ", priority=" + this.getPriority() + ")";
    }
}
