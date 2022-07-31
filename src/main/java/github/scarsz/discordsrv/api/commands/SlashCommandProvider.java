package github.scarsz.discordsrv.api.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.Set;

/**
 * Interface to be added to {@link org.bukkit.plugin.java.JavaPlugin}'s to provide Discord slash command data
 */
public interface SlashCommandProvider {

    Set<CommandData> getSlashCommandData();

    void handleSlashCommand(SlashCommandEvent event, String commandPath);

}
