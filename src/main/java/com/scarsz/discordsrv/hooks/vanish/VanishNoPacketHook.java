package com.scarsz.discordsrv.hooks.vanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VanishNoPacketHook {

    public static boolean isVanished(Player player) {
        try {
            Object vanishPlugin = Bukkit.getPluginManager().getPlugin("VanishNoPacket");
            Object vanishManager = vanishPlugin.getClass().getDeclaredMethod("getManager").invoke(vanishPlugin);

            return (boolean) vanishManager.getClass().getDeclaredMethod("isVanished", String.class).invoke(vanishManager, player.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
