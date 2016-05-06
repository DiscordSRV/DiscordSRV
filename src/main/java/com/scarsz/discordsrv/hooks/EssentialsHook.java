package com.scarsz.discordsrv.hooks;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class EssentialsHook {

    public static boolean isVanished(String player) {
        Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        
        try {
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
