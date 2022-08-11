package github.scarsz.discordsrv.api.commands;

import lombok.Value;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * {@link CommandData} wrapper that includes the originating {@link Plugin}
 */
@Value
public class PluginCommandData extends CommandData {

    Plugin plugin;
    CommandData commandData;

    public PluginCommandData(Plugin plugin, CommandData commandData) {
        super(commandData.getName(), commandData.getDescription());
        this.plugin = plugin;
        this.commandData = commandData;
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
