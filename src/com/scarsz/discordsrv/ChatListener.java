package com.scarsz.discordsrv;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.TextChannel;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class ChatListener implements Listener {
	JDA api;
	Plugin plugin;
    public ChatListener(JDA api, Plugin plugin){
        this.api = api;
        this.plugin = plugin;
    }
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event)
    {
		// ReportCanceledChatEvents debug message
		if (plugin.getConfig().getBoolean("ReportCanceledChatEvents")) plugin.getLogger().info("Chat message received, canceled: " + event.isCancelled());
		
		// return if event canceled
		if (plugin.getConfig().getBoolean("DontSendCanceledChatEvents") && event.isCancelled()) return;
		
		// return if should not send in-game chat
		if (!plugin.getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) return;
		
		// return if user is unsubscribed from Discord and config says don't send those peoples' messages
		if (!DiscordSRV.getSubscribed(event.getPlayer().getUniqueId()) && !plugin.getConfig().getBoolean("MinecraftUnsubscribedMessageForwarding")) return;
		
		TextChannel channel = DiscordSRV.getChannel(plugin.getConfig().getString("DiscordChatChannelName"));
        String message = plugin.getConfig().getString("MinecraftChatToDiscordMessageFormat")
    			.replace("%message%", ChatColor.stripColor(event.getMessage()))
    			.replace("%primarygroup%", DiscordSRV.getPrimaryGroup(event.getPlayer()))
    			.replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
    			.replace("%username%", ChatColor.stripColor(event.getPlayer().getName()));
        
        DiscordSRV.sendMessage(channel, message);
    }
}