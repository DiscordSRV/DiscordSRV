package com.scarsz.discordsrv.hooks;

import java.lang.reflect.Method;

public class VanishNoPacket {

    public static boolean isVanished(String player) {
        try {
        	Object VanishNoPacket = Class.forName("org.kitteh.vanish.staticaccess.VanishNoPacket");
        	Method isVanished = VanishNoPacket.getClass().getDeclaredMethod("isVanished", String.class);
            return (boolean) isVanished.invoke(VanishNoPacket, player);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}