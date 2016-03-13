package com.scarsz.discordsrv;

import org.bukkit.plugin.Plugin;
import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

@SuppressWarnings("deprecation")
public class VanishedPlayerCheck {
	public static boolean checkPlayerIsVanished(String player, Plugin plugin) {
		try {
			boolean result = VanishNoPacket.isVanished(player);
			if (plugin.getConfig().getBoolean("PlayerVanishLookupReporting")) plugin.getLogger().info("Looking up vanish status for " + player + ": " + result);
			return result;
		} catch (VanishNotLoadedException e) {
			e.printStackTrace();
			return false;
		}
	}
}