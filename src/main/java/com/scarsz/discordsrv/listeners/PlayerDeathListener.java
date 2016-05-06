package com.scarsz.discordsrv.listeners;

import net.dv8tion.jda.JDA;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.scarsz.discordsrv.DiscordSRV;

import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
	JDA api;
	public PlayerDeathListener(JDA api) {
		this.api = api;
	}
	
	@EventHandler
	public void PlayerDeathEvent(PlayerDeathEvent event) {
		// return if death messages are disabled
		if (!DiscordSRV.plugin.getConfig().getBoolean("MinecraftPlayerDeathMessageEnabled")) return;
		
		DiscordSRV.sendMessage(DiscordSRV.chatChannel, ChatColor.stripColor(event.getDeathMessage()));
	}
}