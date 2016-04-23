package com.scarsz.discordsrv.hooks;

import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

import com.scarsz.discordsrv.DiscordSRV;

@SuppressWarnings("deprecation")
public class VanishNoPacket {

    public static boolean isVanished(String player) {
        try {
            return org.kitteh.vanish.staticaccess.VanishNoPacket.isVanished(player);
        } catch (VanishNotLoadedException e) {
            e.printStackTrace();
            return false;
        }
    }

}