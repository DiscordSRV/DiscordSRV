package github.scarsz.discordsrv.hooks.world;

import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/25/2017
 * @at 6:44 PM
 */
public class MultiverseCoreHook {

    public static String getWorldAlias(String world) {
        try {
            if (!PluginUtil.checkIfPluginEnabled("Multiverse-Core")) return world;

            Plugin multiversePlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            Object MVWorldManager = multiversePlugin.getClass().getMethod("getMVWorldManager").invoke(multiversePlugin);
            Object MVWorld = MVWorldManager.getClass().getMethod("getMVWorld", String.class).invoke(MVWorldManager, world);
            Object alias = MVWorld.getClass().getMethod("getAlias").invoke(MVWorld);
            return (String) alias;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return world;
    }

}
