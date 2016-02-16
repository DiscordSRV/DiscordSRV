package com.scarsz.discordsrv;

import java.util.concurrent.Executors;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.TextChannel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class ChatListener implements Listener {
	JDA api;
	Plugin plugin;
	Boolean usingMcMMO = false;
    public ChatListener(JDA api, Plugin plugin){
        this.api = api;
        this.plugin = plugin;
        for (Plugin activePlugin : Bukkit.getPluginManager().getPlugins()) if (activePlugin.getName().toLowerCase().contains("mcmmo")) usingMcMMO = true;
    }
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event)
    {
		// return if event canceled
		if (event.isCancelled()) return;
		
		// return if should not send in-game chat
		if (!plugin.getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) return;
		
		// super long one-liner to check for vanished players
		//for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) if (plugin.getName().contains("VanishNoPacket")) try { if (VanishNoPacket.isVanished(event.getPlayer().getName())) return; } catch (VanishNotLoadedException e) { e.printStackTrace(); }
		
		TextChannel channel = DiscordSRV.getChannel(plugin.getConfig().getString("DiscordChatChannelName"));
        String message = plugin.getConfig().getString("MinecraftChatToDiscordMessageFormat")
    			.replace("%message%", ChatColor.stripColor(event.getMessage()))
    			.replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
    			.replace("%username%", ChatColor.stripColor(event.getPlayer().getName()));
        
        //// probably not needed anymore since 4.1 because plugin now *correctly* checks if an event is canceled
        // if the server has mcMMO, check if the player is using the staff/party chat
        //Boolean mcMMOStaffChatEnabled = false;
        //if (usingMcMMO && ChatAPI.isUsingAdminChat(event.getPlayer())) mcMMOStaffChatEnabled = true;
        //Boolean mcMMOPartyChatEnabled = false;
        //if (usingMcMMO && ChatAPI.isUsingPartyChat(event.getPlayer())) mcMMOPartyChatEnabled = true;
        //if (!event.isCancelled() && !mcMMOStaffChatEnabled && !mcMMOPartyChatEnabled) {
        
        final String finalMessage = message;
        Executors.newSingleThreadExecutor().submit(() -> {
        	DiscordSRV.sendMessage(channel, finalMessage);
        });
    }
}