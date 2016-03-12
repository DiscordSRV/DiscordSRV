package com.scarsz.discordsrv;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.bukkit.plugin.Plugin;

import net.dv8tion.jda.JDA;

public class AchievementListener implements Listener {
	
	JDA api;
	Plugin plugin;
	
	public AchievementListener(JDA api, Plugin plugin) {
		this.api = api;
		this.plugin = plugin;
	}
	
	@EventHandler
	public void PlayerAchievementAwardedEvent(PlayerAchievementAwardedEvent event) {
		// return if achievement messages are disabled
		if (!plugin.getConfig().getBoolean("MinecraftPlayerAchievementMessagesEnabled")) return;
		
		DiscordSRV.sendMessage(DiscordSRV.chatChannel, ChatColor.stripColor(plugin.getConfig().getString("MinecraftPlayerAchievementMessagesFormat")
			.replace("%username%", event.getPlayer().getName())
			.replace("%displayname%", event.getPlayer().getDisplayName())
			.replace("%world%", event.getPlayer().getWorld().getName())
			.replace("%achievement%", event.getAchievement().toString())
		));
	}
}
