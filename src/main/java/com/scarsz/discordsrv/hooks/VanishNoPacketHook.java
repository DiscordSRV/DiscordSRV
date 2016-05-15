package com.scarsz.discordsrv.hooks;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;

public class VanishNoPacketHook {

    public static boolean isVanished(String player) {
        try {
            Object vanishPlugin = Bukkit.getPluginManager().getPlugin("VanishNoPacket");
            Object vanishManager = vanishPlugin.getClass().getDeclaredMethod("getManager").invoke(vanishPlugin);
            return (boolean) vanishManager.getClass().getDeclaredMethod("isVanished", String.class).invoke(vanishManager, player);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
