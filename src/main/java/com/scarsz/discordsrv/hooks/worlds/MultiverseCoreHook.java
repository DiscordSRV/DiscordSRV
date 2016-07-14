package com.scarsz.discordsrv.hooks.worlds;

import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class MultiverseCoreHook {

    public static String getWorldAlias(String world) {
        // MVWorldManager manager = ((MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core")).getMVWorldManager();
        // String alias = manager.getMVWorld("world").getAlias();

        try {
            if (DiscordSRV.checkIfPluginEnabled("Multiverse-Core")) return world;

            Plugin multiversePlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            Method getMVWorldManager = multiversePlugin.getClass().getDeclaredMethod("getMVWorldManager");
            Method getMVWorld = getMVWorldManager.getClass().getDeclaredMethod("getMVWorld", String.class);
            Method getAlias = getMVWorld.getClass().getDeclaredMethod("getAlias");
            return (String) getAlias.invoke(world);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return world;
    }

}
