package com.scarsz.discordsrv;

import net.dv8tion.jda.JDA;

import java.util.Calendar;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class ChatListener implements Listener {
	JDA api;
	Plugin plugin;
    public ChatListener(JDA api, Plugin plugin) {
        this.api = api;
        this.plugin = plugin;
    }
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
		// increment stats
		if (event.isCancelled()) DiscordSRV.DebugCancelledMinecraftChatEventsCount++;
		else DiscordSRV.DebugMinecraftChatEventsCount++;
		
		// ReportCanceledChatEvents debug message
		if (plugin.getConfig().getBoolean("ReportCanceledChatEvents")) plugin.getLogger().info("Chat message received, canceled: " + event.isCancelled());
		
		// return if event canceled
		if (plugin.getConfig().getBoolean("DontSendCanceledChatEvents") && event.isCancelled()) return;
		
		// return if should not send in-game chat
		if (!plugin.getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) return;
		
		// return if user is unsubscribed from Discord and config says don't send those peoples' messages
		if (!DiscordSRV.getSubscribed(event.getPlayer().getUniqueId()) && !plugin.getConfig().getBoolean("MinecraftUnsubscribedMessageForwarding")) return;
		
        String message = plugin.getConfig().getString("MinecraftChatToDiscordMessageFormat")
    			.replace("%message%", ChatColor.stripColor(event.getMessage()))
    			.replace("%primarygroup%", DiscordSRV.getPrimaryGroup(event.getPlayer()))
    			.replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
    			.replace("%username%", ChatColor.stripColor(event.getPlayer().getName()))
    			.replace("%time%", new Date().toString());
        
        message = DiscordSRV.convertMentionsFromNames(message);
        
        DiscordSRV.sendMessage(DiscordSRV.chatChannel, message);
    }
}