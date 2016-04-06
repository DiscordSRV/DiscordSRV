package com.scarsz.discordsrv;

import org.bukkit.Bukkit;
import com.scarsz.discordsrv.hooks.PremiumVanish;
import com.scarsz.discordsrv.hooks.SuperVanish;
import com.scarsz.discordsrv.hooks.VanishNoPacket;

public class VanishedPlayerCheck {
	
	public static boolean checkPlayerIsVanished(String player) {		
		if (Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) return PremiumVanish.isVanished(player);
		if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish")) return SuperVanish.isVanished(player);
		if (Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket")) return VanishNoPacket.isVanished(player);
		
		return false;
	}
	
}