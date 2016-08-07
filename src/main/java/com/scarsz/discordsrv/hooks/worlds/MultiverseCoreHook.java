package com.scarsz.discordsrv.hooks.worlds;

import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class MultiverseCoreHook {

    public static String getWorldAlias(String world) {
        // MVWorldManager manager = ((MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core")).getMVWorldManager();
        // String alias = manager.getMVWorld("world").getAlias();

        try {
            if (!DiscordSRV.checkIfPluginEnabled("Multiverse-Core")) return world;

            Plugin multiversePlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            Object MVWorldManager = multiversePlugin.getClass().getDeclaredMethod("getMVWorldManager").invoke(multiversePlugin);
            Object MVWorld = MVWorldManager.getClass().getDeclaredMethod("getMVWorld", String.class).invoke(MVWorldManager, world);
            Object alias = MVWorld.getClass().getDeclaredMethod("getAlias").invoke(MVWorld);
            return (String) alias;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return world;
    }

}
