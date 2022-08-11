package github.scarsz.discordsrv.api.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.Set;

/**
 * Interface to be added to {@link org.bukkit.plugin.java.JavaPlugin}'s to provide Discord slash command data
 */
public interface SlashCommandProvider {

    Set<PluginCommandData> getSlashCommandData();

    void handleSlashCommand(SlashCommandEvent event, String commandPath);

}
