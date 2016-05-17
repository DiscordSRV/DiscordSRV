package com.scarsz.discordsrv.hooks;

public class PremiumVanishHook {

    public static boolean isVanished(String player) {
        return SuperVanishHook.isVanished(player);
    }

}
