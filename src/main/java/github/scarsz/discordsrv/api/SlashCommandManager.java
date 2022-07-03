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

package github.scarsz.discordsrv.api;

import github.scarsz.discordsrv.DiscordSRV;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SlashCommandManager {
    private final Map<Plugin, PluginCommands> commandsMap = new HashMap<>();

    /**
     * Gets the Plugin Commands for a plugin
     * @param plugin plugin to get commands of
     * @return null if this plugin never added their commands here, or else the {@link PluginCommands}
     */
    public PluginCommands getCommands(Plugin plugin) {
        return commandsMap.get(plugin);
    }

    public void reloadSlashCommands() {
        Set<CommandData> commands = new HashSet<>();
        for (PluginCommands value : commandsMap.values()) {
            if (value.plugin.isEnabled()) {
                commands.addAll(value.data);
            }
        }
        if (!commands.isEmpty()) return; //no need to register if no commands will be registered
        Map<Guild, RegistrationResult> errors = new HashMap<>();
        for (Guild guild : DiscordSRV.getPlugin().getJda().getGuilds()) {
            guild.updateCommands().addCommands(commands).queue(null, f -> errors.put(guild, RegistrationResult.getResult(f)));
        }
        printSlashRegistrationError(errors, commandsMap.keySet());
    }

    /**
     * Sets the slash commands for a specific plugin, overrides old ones. this method does not register them, see {@link SlashCommandManager#reloadSlashCommands()}
     * @param plugin Plugin that is registering the commands, whenever discordsrv is registering the commands it will not register the commands for that plugin if it is disabled
     * @param commandData Commands to add
     */
    public void setCommands(@NotNull Plugin plugin, @NotNull CommandData... commandData) {
        PluginCommands pluginCommands = commandsMap.get(plugin);
        if (pluginCommands == null) {
            pluginCommands = new PluginCommands(plugin, new HashSet<>(Arrays.asList(commandData)));
            commandsMap.put(plugin, pluginCommands);
        } else pluginCommands.data = new HashSet<>(Arrays.asList(commandData));
    }

    private void printSlashRegistrationError(Map<Guild, RegistrationResult> errors, Set<Plugin> plugins) {
        if (errors.size() == (int) errors.values().stream().filter(r -> r == RegistrationResult.RATE_LIMIT).count()) {
            DiscordSRV.getPlugin().getLogger().warning("Rate limited while registering in the following guilds: " + errors.keySet().stream().map(Guild::getName).collect(Collectors.joining(", ")));
            return;
        }
        DiscordSRV.getPlugin().getJda().setRequiredScopes("applications.commands");
        String invite = DiscordSRV.getPlugin().getJda().getInviteUrl();
        DiscordSRV.getPlugin().getLogger().warning("==============================================================");
        DiscordSRV.getPlugin().getLogger().warning("DiscordSRV could not register slash commands to some discord servers!");
        for (Guild guild : errors.keySet()) {
            RegistrationResult result = errors.get(guild);
            switch (result) {
                case MISSING_SCOPE:
                    DiscordSRV.getPlugin().getLogger().warning("Missing scopes in " + guild.getName() + " (" + guild.getId() + ")");
                case UNKNOWN_ERROR:
                    DiscordSRV.getPlugin().getLogger().warning("Unknown error in " + guild.getName() + " (" + guild.getId() + ")");
                case RATE_LIMIT:
                    DiscordSRV.getPlugin().getLogger().warning("Rate limited in " + guild.getName() + " (" + guild.getId() + ")");
            }
        }
        if (errors.values().stream().anyMatch(r -> r == RegistrationResult.MISSING_SCOPE)) DiscordSRV.getPlugin().getLogger().warning("Use " + invite + " to re-invite the bot to guilds with missing scope!");
        DiscordSRV.getPlugin().getLogger().warning(" ");
        DiscordSRV.getPlugin().getLogger().warning(plugins.size() == 1 && plugins.toArray(new Plugin[0])[0].equals(DiscordSRV.getPlugin()) ?
                "DiscordSRV's Slash commands may not be registered" : //if discordsrv add commands in the future (like linkaccount command as far a
                "Slash Commands for the following plugins may not be registered: " + plugins.stream().filter(Plugin::isEnabled).map(Plugin::getName).collect(Collectors.joining(", ")));
        DiscordSRV.getPlugin().getLogger().warning("==============================================================");
    }
    enum RegistrationResult {
        /**
         * Successfully registered the commands, nothing wrong happened
         */
        SUCCESS,
        /**
         * Rate limited
         */
        RATE_LIMIT,
        /**
         * Missing the required scope to register the commands
         */
        MISSING_SCOPE,
        /**
         * Unknown Error
         */
        UNKNOWN_ERROR;

        public static RegistrationResult getResult(Throwable t) {
            if (t instanceof ErrorResponseException) {
                ErrorResponseException ex = (ErrorResponseException) t;
                if (ex.getErrorResponse() == ErrorResponse.MISSING_ACCESS) return MISSING_SCOPE;
            } else if (t instanceof RateLimitedException) return RATE_LIMIT;
            return UNKNOWN_ERROR;
        }
    }

    @AllArgsConstructor
    public static class PluginCommands {
        @Getter
        private final Plugin plugin;
        @Getter
        private Set<CommandData> data;
    }
}
