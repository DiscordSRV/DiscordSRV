package com.scarsz.discordsrv.listeners;

import java.util.Date;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.scarsz.discordsrv.DiscordSRV;

import net.dv8tion.jda.JDA;

public class ChatListener implements Listener {
	
	JDA api;
    
	public ChatListener(JDA api) {
        this.api = api;
    }
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
		// ReportCanceledChatEvents debug message
		if (DiscordSRV.plugin.getConfig().getBoolean("ReportCanceledChatEvents")) DiscordSRV.plugin.getLogger().info("Chat message received, canceled: " + event.isCancelled());
		
		// return if event canceled
		if (DiscordSRV.plugin.getConfig().getBoolean("DontSendCanceledChatEvents") && event.isCancelled()) return;
		
		// return if should not send in-game chat
		if (!DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) return;
		
		// return if user is unsubscribed from Discord and config says don't send those peoples' messages
		if (!DiscordSRV.getSubscribed(event.getPlayer().getUniqueId()) && !DiscordSRV.plugin.getConfig().getBoolean("MinecraftUnsubscribedMessageForwarding")) return;
		
		String message = DiscordSRV.plugin.getConfig().getString("MinecraftChatToDiscordMessageFormat")
        		.replaceAll("&([0-9a-qs-z])", "")
    			.replace("%message%", ChatColor.stripColor(event.getMessage()))
    			.replace("%primarygroup%", DiscordSRV.getPrimaryGroup(event.getPlayer()))
    			.replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
    			.replace("%username%", ChatColor.stripColor(event.getPlayer().getName()))
    			.replace("%time%", new Date().toString());
        
        message = DiscordSRV.convertMentionsFromNames(message);
        
		DiscordSRV.sendMessage(DiscordSRV.chatChannel, message);
    }
}