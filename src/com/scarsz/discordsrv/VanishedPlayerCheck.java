package com.scarsz.discordsrv;

import com.scarsz.discordsrv.hooks.Essentials;
import org.bukkit.Bukkit;
import com.scarsz.discordsrv.hooks.PremiumVanish;
import com.scarsz.discordsrv.hooks.SuperVanish;
import com.scarsz.discordsrv.hooks.VanishNoPacket;

public class VanishedPlayerCheck {
	
	public static boolean checkPlayerIsVanished(String player) {
        Boolean isVanished = false;

        if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) isVanished = Essentials.isVanished(player) ? true : isVanished;
		if (Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) isVanished = PremiumVanish.isVanished(player) ? true : isVanished;
		if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish")) isVanished = SuperVanish.isVanished(player) ? true : isVanished;
		if (Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket")) isVanished = VanishNoPacket.isVanished(player) ? true : isVanished;

        if (DiscordSRV.plugin.getConfig().getBoolean("PlayerVanishLookupReporting")) DiscordSRV.plugin.getLogger().info("Looking up vanish status for " + player + ": " + isVanished);
		return isVanished;
	}
	
}