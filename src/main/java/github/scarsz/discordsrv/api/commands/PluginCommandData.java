package github.scarsz.discordsrv.api.commands;

import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * {@link CommandData} wrapper that includes the originating {@link Plugin}
 */
@Value
public class PluginCommandData extends CommandData {

    Plugin plugin;
    CommandData commandData;
    Set<String> guilds = new HashSet<>();

    /**
     * Construct data for a new plugin-originating slash command
     * @param plugin the owning plugin
     * @param commandData the command data
     * @param guildIds the applicable guild IDs for this command. if not provided, command will be applicable to all guilds
     */
    public PluginCommandData(Plugin plugin, CommandData commandData, String... guildIds) {
        super(commandData.getName(), commandData.getDescription());
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
     * Add the given guild to the list of applicable guilds for this command.
     * @param guild the guild to apply this command to
     * @return the {@link PluginCommandData} instance for chaining
     */
    public PluginCommandData addGuildFilter(Guild guild) {
        return addGuildFilter(guild.getId());
    }
    /**
     * Add the given guild ID to the list of applicable guilds for this command.
     * @param guildId the guild ID to apply this command to
     * @return the {@link PluginCommandData} instance for chaining
     */
    public PluginCommandData addGuildFilter(String guildId) {
        this.guilds.add(guildId);
        return this;
    }

    /**
     * Checks whether this given PluginCommandData loosely matches the supplied {@link CommandData}
     * or strictly matches the supplied {@link PluginCommandData}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (o instanceof PluginCommandData) {
            PluginCommandData that = (PluginCommandData) o;
            return Objects.equals(plugin, that.plugin) && Objects.equals(commandData, that.commandData);
        } else if (o instanceof CommandData) {
            return Objects.equals(commandData, o);
        } else {
            return false;
        }
    }

}
