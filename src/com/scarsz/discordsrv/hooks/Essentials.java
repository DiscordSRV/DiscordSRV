package com.scarsz.discordsrv.hooks;

import org.bukkit.Bukkit;

public class Essentials {

    public static boolean isVanished(String player) {
        com.earth2me.essentials.Essentials ess = (com.earth2me.essentials.Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        return ess.getUser(player).isVanished();
    }

}
