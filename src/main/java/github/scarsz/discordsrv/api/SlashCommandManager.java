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
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SlashCommandManager {
    @Getter
    private boolean updated = false;
    private final Map<Plugin, PluginCommands> commandsMap = new HashMap<>();

    public void reloadSlashCommands() {
        CommandListUpdateAction action = DiscordSRV.getPlugin().getMainGuild().updateCommands();
        boolean added = false;
        for (PluginCommands value : commandsMap.values()) {
            if (value.plugin.isEnabled()) {
                added = true;
                action.addCommands(value.data);
            }
        }
        if (!added) return; //no need to register if no commands will be registered
        action.queue(s -> updated = true,er -> printSlashRegistrationError(RegistrationResult.getResult(er), commandsMap.keySet()));
    }

    /**
     * Sets the slash commands for a specific plugin, overrides old ones. this method does not register them, see {@link SlashCommandManager#reloadSlashCommands()}
     * @param plugin Plugin that is registering the commands, whenever discordsrv is registering the commands it will not register the commands for that plugin if it is disabled
     * @param commandData Commands to add
     */
    public void setCommandsForPlugin(@NotNull Plugin plugin, @NotNull CommandData... commandData) {
        PluginCommands pluginCommands = commandsMap.get(plugin);
        if (pluginCommands == null) {
            pluginCommands = new PluginCommands(plugin, new HashSet<>(Arrays.asList(commandData)));
            commandsMap.put(plugin, pluginCommands);
        } else pluginCommands.data = new HashSet<>(Arrays.asList(commandData));
        updated = false;
    }

    private void printSlashRegistrationError(RegistrationResult result, Set<Plugin> plugins) {
        if (result == RegistrationResult.RATE_LIMIT) {
            DiscordSRV.getPlugin().getLogger().warning("Rate Limited! Are you restarting your server or reloading this plugin alot?");
            return;
        }
        DiscordSRV.getPlugin().getLogger().warning("==============================================================");
        DiscordSRV.getPlugin().getLogger().warning("DiscordSRV could not register slash commands to your discord server!");
        switch (result) {
            case SUCCESS:
                break;
            case MISSING_SCOPE:
                DiscordSRV.getPlugin().getJda().setRequiredScopes("applications.commands");
                DiscordSRV.getPlugin().getLogger().warning("Your bot is missing some required scopes, re-invite the bot using this invite: " + DiscordSRV.getPlugin().getJda().getInviteUrl() + " and reload DiscordSRV");
                break;
            case UNKNOWN_ERROR:
            default:
                DiscordSRV.getPlugin().getLogger().warning("Unknown slash commands registration error");
        }
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
    private static class PluginCommands {
        private final Plugin plugin;
        private Set<CommandData> data;
    }
}
