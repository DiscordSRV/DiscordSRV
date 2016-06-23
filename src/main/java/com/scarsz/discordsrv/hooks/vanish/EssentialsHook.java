package com.scarsz.discordsrv.hooks.vanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class EssentialsHook {

    public static boolean isVanished(Player player) {
        try {
            Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
        	Method getUser = ess.getClass().getDeclaredMethod("getUser", String.class);
        	Object essentialsPlayer = getUser.invoke(ess, player.getName());
        	if (essentialsPlayer != null) {
        		Method isVanished = essentialsPlayer.getClass().getDeclaredMethod("isVanished");
        		return (boolean) isVanished.invoke(essentialsPlayer);
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
