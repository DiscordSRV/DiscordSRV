package com.scarsz.discordsrv.hooks;

import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

import com.scarsz.discordsrv.DiscordSRV;

@SuppressWarnings("deprecation")
public class VanishNoPacket {
	public static boolean isVanished(String player) {
		try {
			boolean result = org.kitteh.vanish.staticaccess.VanishNoPacket.isVanished(player);
			if (DiscordSRV.plugin.getConfig().getBoolean("PlayerVanishLookupReporting")) DiscordSRV.plugin.getLogger().info("Looking up vanish status for " + player + ": " + result);
			return result;
		} catch (VanishNotLoadedException e) {
			e.printStackTrace();
			return false;
		}
	}
}