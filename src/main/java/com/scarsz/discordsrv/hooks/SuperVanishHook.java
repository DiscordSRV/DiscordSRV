package com.scarsz.discordsrv.hooks;

import java.lang.reflect.Method;

public class SuperVanishHook {

    public static boolean isVanished(String player) {
        try {
            Class<?> VanishAPI = Class.forName("de.myzelyam.api.vanish.VanishAPI");
            Method getInvisiblePlayers = VanishAPI.getDeclaredMethod("getInvisiblePlayers");
            List<Player> invisiblePlayers = (List<Player>) getInvisiblePlayers.invoke(VanishAPI);
            if (invisiblePlayers == null) return false;
            
            return (boolean) invisiblePlayers.getClass().getDeclaredMethod("contains", String.class).invoke(invisiblePlayers, player);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
