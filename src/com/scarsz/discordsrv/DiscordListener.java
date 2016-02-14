package com.scarsz.discordsrv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("unchecked")
public class DiscordListener extends ListenerAdapter{
    Server server;
    Plugin plugin;
    public DiscordListener(Server server, Plugin plugin){
        this.server = server;
        this.plugin = plugin;
    }
    String lastMessageSent = "";
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
    	final String chatChannel = plugin.getConfig().getString("DiscordChatChannelName");
    	final String consoleChannel = plugin.getConfig().getString("DiscordConsoleChannelName");
    	
    	if (event != null && event.getAuthor().getId() != null && event.getJDA().getSelfInfo().getId() != null && event.getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) return;
    	for (String phrase : (List<String>) plugin.getConfig().getList("DiscordChatChannelBlockedPhrases"))
    		if (event.getMessage().getContent().contains(phrase)) return;
    	
		if (plugin.getConfig().getBoolean("DiscordChatChannelEnabled"))
			if (event.getTextChannel().getName().equals(chatChannel))
				handleChat(event);
		if (plugin.getConfig().getBoolean("DiscordConsoleChannelEnabled"))
			if (event.getTextChannel().getName().equals(consoleChannel))
				handleConsole(event);
    }
	private void handleChat(MessageReceivedEvent event) {
		synchronized (lastMessageSent){
			if (lastMessageSent == event.getMessage().getId()) return;
			else lastMessageSent = event.getMessage().getId();
		}
		String message = event.getMessage().getStrippedContent();
		if (plugin.getConfig().getBoolean("DiscordChatChannelListCommandEnabled") && message.startsWith(plugin.getConfig().getString("DiscordChatChannelListCommandMessage"))){
			String playerlistMessage = plugin.getConfig().getString("DiscordChatChannelListCommandFormatOnlinePlayers").replace("%playercount%", Integer.toString(Bukkit.getOnlinePlayers().size()) + "/" + Integer.toString(Bukkit.getMaxPlayers())) + "\n";
			if (Bukkit.getOnlinePlayers().size() == 0){
				event.getTextChannel().sendMessage(plugin.getConfig().getString("DiscordChatChannelListCommandFormatNoOnlinePlayers"));
				return;
			}
			for (Player player : Bukkit.getOnlinePlayers()) if (playerlistMessage.length() < 2000) playerlistMessage += ChatColor.stripColor(player.getDisplayName()) + ", ";
			playerlistMessage = playerlistMessage.substring(0, playerlistMessage.length() - 2);
			if (playerlistMessage.length() < 2000) {
				event.getTextChannel().sendMessage(playerlistMessage);
			}
			return;
		}
		if (message.length() == 0) return;
    	if (message.length() > plugin.getConfig().getInt("DiscordChatChannelTruncateLength")) message = message.substring(0, plugin.getConfig().getInt("DiscordChatChannelTruncateLength"));
    	server.broadcastMessage(plugin.getConfig().getString("DiscordToMinecraftChatMessageFormat")
    			.replaceAll("&([0-9a-z])", "\u00A7$1") // color coding
    			.replace("%message%", event.getMessage().getContent())
    			.replace("%username%", event.getMessage().getAuthor().getUsername())
    	);
	}
	private void handleConsole(MessageReceivedEvent event) {
		// old code to disable the "?" command
		//if (plugin.getConfig().getBoolean("DiscordConsoleChannelDisableHelpCommand") && event.getMessage().getContent().startsWith("?")) return;
		
		Boolean allowed = false;
		Boolean DiscordConsoleChannelBlacklistActsAsWhitelist = plugin.getConfig().getBoolean("DiscordConsoleChannelBlacklistActsAsWhitelist");
		List<String> DiscordConsoleChannelBlacklistedCommands = (List<String>) plugin.getConfig().getList("DiscordConsoleChannelBlacklistedCommands");
		String requestedCommand = event.getMessage().getContent();
		while (requestedCommand.substring(0, 1) == " ") requestedCommand = requestedCommand.substring(1);
		requestedCommand = requestedCommand.split(" ")[0]; // *op* person
		if (DiscordConsoleChannelBlacklistActsAsWhitelist && DiscordConsoleChannelBlacklistedCommands.contains(requestedCommand)) allowed = true; else allowed = false;
		if (!DiscordConsoleChannelBlacklistActsAsWhitelist && DiscordConsoleChannelBlacklistedCommands.contains(requestedCommand)) allowed = false; else allowed = true;
		if (!allowed) return;
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(new File(".").getAbsolutePath() + "/./" + plugin.getConfig().getString("DiscordConsoleChannelUsageLog")).getAbsolutePath(), true)))) {
			out.println("[" + new Date() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getUsername() + ": " + event.getMessage().getContent());
		}catch (IOException e) {plugin.getLogger().warning("Error logging console action to " + plugin.getConfig().getString("DiscordConsoleChannelUsageLog")); return;}
		server.dispatchCommand(server.getConsoleSender(), event.getMessage().getContent());
	}
}