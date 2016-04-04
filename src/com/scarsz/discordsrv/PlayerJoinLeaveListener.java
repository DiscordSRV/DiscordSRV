package com.scarsz.discordsrv;

import java.util.HashMap;
import java.util.Map;

import net.dv8tion.jda.JDA;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PlayerJoinLeaveListener implements Listener {
	JDA api;
	Plugin plugin;
	public PlayerJoinLeaveListener(JDA api, Plugin plugin) {
		this.api = api;
		this.plugin = plugin;
	}
	Map<Player, Boolean> playerStatusIsOnline = new HashMap<Player, Boolean>();
	
	@EventHandler
	public void PlayerJoinEvent(PlayerJoinEvent event) {
		// Make sure join messages enabled
		if (!plugin.getConfig().getBoolean("MinecraftPlayerJoinMessageEnabled")) return;
		
		// Check if player has permission to not have join messages
		if (Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket") && (event.getPlayer().hasPermission("vanish.silentjoin") || event.getPlayer().hasPermission("vanish.joinwithoutannounce"))) return;
		
		// Assign player's status to online since they don't have silent join permissions
		playerStatusIsOnline.put(event.getPlayer(), true);
		
		// Player doesn't have silent join permission, send join message
		DiscordSRV.sendMessage(DiscordSRV.chatChannel, plugin.getConfig().getString("MinecraftPlayerJoinMessageFormat")
    			.replace("%username%", event.getPlayer().getName())
    			.replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
    	);
	}
	@EventHandler
	public void PlayerQuitEvent(PlayerQuitEvent event) {
		// Make sure quit messages enabled
		if (!plugin.getConfig().getBoolean("MinecraftPlayerLeaveMessageEnabled")) return;
		
		// No quit message, user shouldn't have one from permission
		if (Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket") && event.getPlayer().hasPermission("vanish.silentquit")) return;
		
		// Remove player from status map to help with memory management
		playerStatusIsOnline.remove(event.getPlayer());
		
		// Player doesn't have silent quit, show quit message
		DiscordSRV.sendMessage(DiscordSRV.chatChannel, plugin.getConfig().getString("MinecraftPlayerLeaveMessageFormat")
    			.replace("%username%", event.getPlayer().getName())
    			.replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
    	);
	}
	@EventHandler
	public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		if (isFakeJoin(event.getMessage()) && event.getPlayer().hasPermission("vanish.fakeannounce") && plugin.getConfig().getBoolean("MinecraftPlayerJoinMessageEnabled")) {
			// Player has permission to fake join messages
			
			// Set player's status if they don't already have one
			if (!playerStatusIsOnline.containsKey(event.getPlayer())) playerStatusIsOnline.put(event.getPlayer(), false);
			
			// Make sure player's status isn't already "online" and isn't a forced command
			// (player is already online AND command is not forced)
			if (playerStatusIsOnline.get(event.getPlayer()) && !isForceFakeJoin(event.getMessage())) return;
			
			// Set status as online
			playerStatusIsOnline.put(event.getPlayer(), true);
			
			// Send fake join message
			DiscordSRV.sendMessage(DiscordSRV.chatChannel, plugin.getConfig().getString("MinecraftPlayerJoinMessageFormat")
		    		.replace("%username%", event.getPlayer().getName())
		    		.replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
		    );
		} else if (isFakeQuit(event.getMessage()) && event.getPlayer().hasPermission("vanish.fakeannounce") && plugin.getConfig().getBoolean("MinecraftPlayerLeaveMessageEnabled")) {
			// Player has permission to fake quit messages
			
			// Set player's status if they don't already have one
			if (!playerStatusIsOnline.containsKey(event.getPlayer())) playerStatusIsOnline.put(event.getPlayer(), true);
			
			// Make sure player's status isn't already "offline" and isn't a forced command
			// (player is already offline AND command is not forced)
			if (!playerStatusIsOnline.get(event.getPlayer()) && !isForceFakeQuit(event.getMessage())) return;
			
			// Set status as online
			playerStatusIsOnline.put(event.getPlayer(), false);
			
			// Send fake quit message
			DiscordSRV.sendMessage(DiscordSRV.chatChannel, plugin.getConfig().getString("MinecraftPlayerLeaveMessageFormat")
					.replace("%username%", event.getPlayer().getName())
	    			.replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
	    	);
	    }
	}
	
	private Boolean isFakeJoin(String message) {
		return message.startsWith("/v fj") || message.startsWith("/vanish fj") || message.startsWith("/v fakejoin") || message.startsWith("/vanish fakejoin");
	}
	private Boolean isFakeQuit(String message) {
		return message.startsWith("/v fq") || message.startsWith("/vanish fq") || message.startsWith("/v fakequit") || message.startsWith("/vanish fakequit");
	}
	private Boolean isForceFakeJoin(String message) {
		return isFakeJoin(message) && (message.endsWith(" f") || message.endsWith(" force"));
	}
	private Boolean isForceFakeQuit(String message) {
		return isFakeQuit(message) && (message.endsWith(" f") || message.endsWith(" force"));
	}
}
