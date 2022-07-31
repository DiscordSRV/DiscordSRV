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
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        Set<RegistrationError> errors = Collections.synchronizedSet(new HashSet<>());
        List<RestAction<List<Command>>> actions = new ArrayList<>();
        for (Guild guild : DiscordSRV.getPlugin().getJda().getGuilds()) {
            actions.add(guild.updateCommands().addCommands(commands).onErrorMap(r -> {
                errors.add(new RegistrationError(guild, RegistrationResult.getResult(r)));
                return null;
            }));
        }
        RestAction.allOf(actions).queue(s -> {
            if (commands.isEmpty()) return; //nobody cares if it fails to register nothing
            printSlashRegistrationError(errors, commandsMap.keySet());
        });
    }

    /**
     * Sets the slash commands for a specific plugin, overrides old ones. this method does not register them, see {@link SlashCommandManager#reloadSlashCommands()}
     * @param plugin Plugin that is registering the commands, whenever discordsrv is registering the commands it will not register the commands for that plugin if it is disabled
     * @param commandData Commands to add
     */
    public void setCommands(@NotNull Plugin plugin, @NotNull CommandData... commandData) {
        commandsMap.put(plugin, new PluginCommands(plugin, new HashSet<>(Arrays.asList(commandData))));
    }

    private void printSlashRegistrationError(Set<RegistrationError> errors, Set<Plugin> plugins) {
        if (errors.isEmpty) return;
        DiscordSRV.getPlugin().getJda().setRequiredScopes("applications.commands");
        String invite = DiscordSRV.getPlugin().getJda().getInviteUrl();
        Logger logger = DiscordSRV.getPlugin().getLogger();
        logger.warning("==============================================================");
        logger.warning("DiscordSRV could not register slash commands to some discord servers!");
        for (RegistrationError error : errors) {
            RegistrationResult result = error.getResult();
            Guild guild = error.getGuild();
            switch (result) {
                case MISSING_SCOPE:
                    logger.warning("Missing scopes in " + guild.getName() + " (" + guild.getId() + ")");
                case UNKNOWN_ERROR:
                    logger.warning("Unknown error in " + guild.getName() + " (" + guild.getId() + ")");
            }
        }
        if (errors.stream().anyMatch(r -> r.result == RegistrationResult.MISSING_SCOPE)) logger.warning("Use " + invite + " to re-invite the bot to guilds with missing scope!");
        logger.warning(" ");
        logger.warning("Slash Commands for the following plugins may not be registered: " + plugins.stream().filter(Plugin::isEnabled).map(Plugin::getName).collect(Collectors.joining(", ")));
        logger.warning("==============================================================");
    }

    @RequiredArgsConstructor
    private static class RegistrationError {

        @Getter
        private final Guild guild;
        @Getter
        private final RegistrationResult result;

    }

    private enum RegistrationResult {
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
            }
            return UNKNOWN_ERROR;
        }
    }

    @AllArgsConstructor
    public static class PluginCommands {

        @Getter
        private final Plugin plugin;
        @Getter
        private final Set<CommandData> data;

    }
}
