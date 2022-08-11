package github.scarsz.discordsrv.api.commands;

import java.util.Set;

/**
 * Interface to be added to {@link org.bukkit.plugin.java.JavaPlugin}'s to provide Discord slash command data
 */
public interface SlashCommandProvider {

    Set<PluginCommandData> getSlashCommandData();

}
