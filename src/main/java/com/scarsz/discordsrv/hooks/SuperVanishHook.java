package com.scarsz.discordsrv.hooks;

import java.lang.reflect.Method;

public class SuperVanishHook {

    public static boolean isVanished(String player) {    	
    	try {
        	Object VanishAPI = Class.forName("de.myzelyam.api.vanish.VanishAPI");
			Method getInvisiblePlayers = VanishAPI.getClass().getDeclaredMethod("getInvisiblePlayers");
	        Object invisiblePlayers = getInvisiblePlayers.invoke(VanishAPI);

	        return (boolean) invisiblePlayers.getClass().getDeclaredMethod("contains", String.class).invoke(invisiblePlayers, player);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
    }
}
