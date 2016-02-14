package com.scarsz.discordsrv;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.TextChannel;

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
		// Super long one-liner to check for vanished players
		//for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) if (plugin.getName().contains("VanishNoPacket")) try { if (VanishNoPacket.isVanished(event.getEntity().getName())) return; } catch (VanishNotLoadedException e) { e.printStackTrace(); }
		
		// return if death messages are disabled
		if (!plugin.getConfig().getBoolean("MinecraftPlayerDeathMessageEnabled")) return;
		
		TextChannel channel = DiscordSRV.getChannel(plugin.getConfig().getString("DiscordChatChannelName"));
		DiscordSRV.sendMessage(channel, ChatColor.stripColor(event.getDeathMessage()));
	}
}