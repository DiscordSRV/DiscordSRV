package com.scarsz.discordsrv.hooks;

import de.myzelyam.api.vanish.VanishAPI;

public class PremiumVanishHook {

    public static boolean isVanished(String player) {
        return VanishAPI.getInvisiblePlayers().contains(player);
    }

}
