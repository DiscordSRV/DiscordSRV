package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * This event is called before registering slash commands on discord, this event is always fired on startup and reload. The event will do nothing
 * if there are no commands added, this will be changed when link account slash command is added
 */
public class SlashCommandsPreRegistrationEvent extends Event{
    private final Set<CommandData> commands = new HashSet<>();
    @Getter
    private final Set<Plugin> plugins = new HashSet<>();

    /**
     * adds commands to the event (this means discordsrv will register it to discord)
     * this does nothing if plugin is disabled
     * @param plugin plugin that is trying to add the commands
     * @param commands commands to add
     */
    public void addCommands(@NotNull Plugin plugin, CommandData... commands) {
        if (!plugin.isEnabled()) return;
        plugins.add(plugin);
        this.commands.addAll(Arrays.asList(commands));
    }

    public boolean shouldRegister() {
        return commands.isEmpty();
    }

    /**
     * current commands added here
     * @return current commands, unmodifiable
     */
    public Set<CommandData> getCurrentCommands() {
        //unmodifiable so people don't accidentally add their commands here
        return Collections.unmodifiableSet(commands);
    }
}
