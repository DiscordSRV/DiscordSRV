package github.scarsz.discordsrv.hooks.world;

import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.Bukkit;

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

            com.onarandombox.MultiverseCore.MultiverseCore multiversePlugin = (com.onarandombox.MultiverseCore.MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            return multiversePlugin.getMVWorldManager().getMVWorld(world).getAlias();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return world;
    }

}
