package github.scarsz.discordsrv.hooks;

import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public interface PluginHook extends Listener {

    Plugin getPlugin();

    default boolean isEnabled() {
        return getPlugin() != null && getPlugin().isEnabled() && PluginUtil.pluginHookIsEnabled(getPlugin().getName());
    }

}
