package github.scarsz.discordsrv.api.commands;

import github.scarsz.discordsrv.api.ApiManager;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * Provides {@link PluginCommandData} datum to DiscordSRV's {@link ApiManager}.
 * {@link Plugin}s implementing this interface are automatically registered.
 * To manually register a provider, use {@link ApiManager#addSlashCommandProvider(SlashCommandProvider)}.
 * To handle commands, create a method in a {@link SlashCommandProvider} with the {@link SlashCommand} annotation.
 * @see SlashCommand
 * @see ApiManager#addSlashCommandProvider(SlashCommandProvider)
 * @see CommandInteraction
 * @see InteractionHook
 */
public interface SlashCommandProvider {

    Set<PluginCommandData> getSlashCommandData();

}
