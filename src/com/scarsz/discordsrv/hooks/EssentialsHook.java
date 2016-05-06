package com.scarsz.discordsrv.hooks;

import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;

public class EssentialsHook {

    public static boolean isVanished(String player) {
        try {
            Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("EssentialsHook");
            return ess.getUser(player).isVanished();
        } catch (Exception e) {
            return false;
        }
    }

}
