package github.scarsz.discordsrv.hooks.world;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class MultiverseCoreV4Hook implements WorldHook {

    @Override
    public String getWorldAlias(String world) {
        MultiverseCore mvPlugin = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (mvPlugin == null) return world;

        MultiverseWorld mvWorld = mvPlugin.getMVWorldManager().getMVWorld(world);
        if (mvWorld == null) return world;

        String alias = mvWorld.getAlias();
        return StringUtils.isNotBlank(alias) ? alias : world;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Multiverse-Core");
    }

    @Override
    public boolean isEnabled() {
        boolean enabled = getPlugin() != null && getPlugin().isEnabled() && PluginUtil.pluginHookIsEnabled(getPlugin().getName());
        if (!enabled) return false;

        try {
            Class.forName("com.onarandombox.MultiverseCore.MultiverseCore");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}