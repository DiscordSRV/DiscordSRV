package com.scarsz.discordsrv.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class EssentialsHook {

    public static boolean isVanished(String player) {
        try {
            Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        	Method getUser = ess.getClass().getDeclaredMethod("getUser", String.class);
        	Object essentialsPlayer = getUser.invoke(ess, player);
        	Method isVanished = essentialsPlayer.getClass().getDeclaredMethod("isVanished");
        
        	return (boolean) isVanished.invoke(essentialsPlayer);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
