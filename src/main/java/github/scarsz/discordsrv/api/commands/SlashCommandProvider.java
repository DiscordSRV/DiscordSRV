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

import github.scarsz.discordsrv.api.ApiManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * Provides sets of {@link PluginCommandData} to DiscordSRV's {@link ApiManager}.
 * To handle commands, create a {@link SlashCommand}-annotated method in the {@link SlashCommandProvider} with one parameter of type {@link SlashCommandEvent}.
 * {@link Plugin}s implementing this interface are automatically registered.
 * To manually register a provider, use {@link ApiManager#addSlashCommandProvider(SlashCommandProvider)}.
 * @see SlashCommand
 * @see ApiManager#addSlashCommandProvider(SlashCommandProvider)
 * @see CommandInteraction
 * @see InteractionHook
 */
public interface SlashCommandProvider {

    Set<PluginCommandData> getSlashCommandData();

}
