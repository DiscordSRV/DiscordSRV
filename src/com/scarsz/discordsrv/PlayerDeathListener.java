package com.scarsz.discordsrv;

import net.dv8tion.jda.JDA;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
	JDA api;
	Plugin plugin;
	public PlayerDeathListener(JDA api, Plugin plugin){
		this.api = api;
		this.plugin = plugin;
	}
	
	@EventHandler
	public void PlayerDeathEvent(PlayerDeathEvent event){
		// return if death messages are disabled
		if (!plugin.getConfig().getBoolean("MinecraftPlayerDeathMessageEnabled")) return;
		
		DiscordSRV.sendMessage(DiscordSRV.chatChannel, ChatColor.stripColor(event.getDeathMessage()));
	}
}