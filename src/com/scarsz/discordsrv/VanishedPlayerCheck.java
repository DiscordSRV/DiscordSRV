package com.scarsz.discordsrv;

import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

@SuppressWarnings("deprecation")
public class VanishedPlayerCheck {
	public static boolean checkPlayerIsVanished(String player) {
		try {
			return VanishNoPacket.isVanished(player);
		} catch (VanishNotLoadedException e) {
			e.printStackTrace();
			return false;
		}
	}
}