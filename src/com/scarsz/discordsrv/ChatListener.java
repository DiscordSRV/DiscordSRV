package com.scarsz.discordsrv;

import java.util.concurrent.Executors;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.TextChannel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import com.gmail.nossr50.api.ChatAPI;

public class ChatListener implements Listener {
    JDA api;
    Plugin plugin;
    Boolean usingMcMMO = false;
    public ChatListener(JDA api, Plugin plugin){
        this.api = api;
        this.plugin = plugin;
        for (Plugin activePlugin : Bukkit.getPluginManager().getPlugins()) if (activePlugin.getName().toLowerCase().contains("mcmmo")) usingMcMMO = true;
    }
    
    @EventHandler
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event)
    {
        // Super long one-liner to check for vanished players
        //for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) if (plugin.getName().contains("VanishNoPacket")) try { if (VanishNoPacket.isVanished(event.getPlayer().getName())) return; } catch (VanishNotLoadedException e) { e.printStackTrace(); }
        
        TextChannel channel = DiscordSRV.getChannel(plugin.getConfig().getString("DiscordChatChannelName"));
        /*channel.sendMessage((event.getPlayer().getDisplayName() + " [" + event.getPlayer().getName() + "]: " + event.getMessage())
                .replaceAll("@", "")
                .replaceAll("(§[0-9])|(§[a-z])", "")
                .replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "")
                .replaceAll("\\[[0-9]{1,3}m", ""));*/
        String message = "";
        message = plugin.getConfig().getString("MinecraftChatToDiscordMessageFormat")
                .replace("%message%", ChatColor.stripColor(event.getMessage()))
                .replace("%displayname%", ChatColor.stripColor(event.getPlayer().getDisplayName()))
                .replace("%username%", ChatColor.stripColor(event.getPlayer().getName()));
        
        // if the server has mcMMO, check if the player is using the staff/party chat
        Boolean mcMMOStaffChatEnabled = false;
        if (usingMcMMO && ChatAPI.isUsingAdminChat(event.getPlayer())) mcMMOStaffChatEnabled = true;
        Boolean mcMMOPartyChatEnabled = false;
        if (usingMcMMO && ChatAPI.isUsingPartyChat(event.getPlayer())) mcMMOPartyChatEnabled = true;
        
        if (!event.isCancelled() && !mcMMOStaffChatEnabled && !mcMMOPartyChatEnabled) {
            final String finalMessage = message;
            Executors.newSingleThreadExecutor().submit(() -> {
                DiscordSRV.sendMessage(channel, finalMessage);
            });
        }
    }
}
