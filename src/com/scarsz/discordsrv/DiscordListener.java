package com.scarsz.discordsrv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import net.dv8tion.jda.entities.TextChannel;
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
    	if (event.isPrivate()) return;
    	if (event != null && event.getAuthor().getId() != null && event.getJDA().getSelfInfo().getId() != null && event.getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) return;
    	for (String phrase : (List<String>) plugin.getConfig().getList("DiscordChatChannelBlockedPhrases")) if (event.getMessage().getContent().contains(phrase)) return;
    	
		if (plugin.getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft") || plugin.getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord"))
			if (event.getTextChannel().getName().equals(plugin.getConfig().getString("DiscordChatChannelName")))
				handleChat(event);
		if (plugin.getConfig().getBoolean("DiscordConsoleChannelEnabled"))
			if (event.getTextChannel().getName().equals(plugin.getConfig().getString("DiscordConsoleChannelName")))
				handleConsole(event);
    }
	private void handleChat(MessageReceivedEvent event) {
		synchronized (lastMessageSent){
			if (lastMessageSent == event.getMessage().getId()) return;
			else lastMessageSent = event.getMessage().getId();
		}
		
		// return if should not send discord chat
		if (!plugin.getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft")) return;
		
		String message = event.getMessage().getStrippedContent();
		if (plugin.getConfig().getBoolean("DiscordChatChannelListCommandEnabled") && message.startsWith(plugin.getConfig().getString("DiscordChatChannelListCommandMessage"))){
			String playerlistMessage = plugin.getConfig().getString("DiscordChatChannelListCommandFormatOnlinePlayers").replace("%playercount%", Integer.toString(DiscordSRV.getOnlinePlayers().size()) + "/" + Integer.toString(Bukkit.getMaxPlayers())) + "\n";
			if (DiscordSRV.getOnlinePlayers().size() == 0){
				event.getChannel().sendMessage(plugin.getConfig().getString("DiscordChatChannelListCommandFormatNoOnlinePlayers"));
				return;
			}
			if (!Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket"))
				for (Player playerNoVanish : Bukkit.getOnlinePlayers()) if (playerlistMessage.length() < 2000) playerlistMessage += ChatColor.stripColor(playerNoVanish.getDisplayName()) + ", ";
			else
				for (Player playerVanish : DiscordSRV.getOnlinePlayers()) if (playerlistMessage.length() < 2000) playerlistMessage += ChatColor.stripColor(playerVanish.getDisplayName()) + ", ";
			playerlistMessage = playerlistMessage.substring(0, playerlistMessage.length() - 2);
			if (playerlistMessage.length() > 2000) playerlistMessage = playerlistMessage.substring(0, 2000);
			DiscordSRV.sendMessage((TextChannel) event.getChannel(), playerlistMessage);
			return;
		}
		if (message.length() == 0) return;
    	if (message.length() > plugin.getConfig().getInt("DiscordChatChannelTruncateLength")) message = message.substring(0, plugin.getConfig().getInt("DiscordChatChannelTruncateLength"));
    	
    	String formatMessage = event.getGuild().getRolesForUser(event.getAuthor()).isEmpty()
    			? plugin.getConfig().getString("DiscordToMinecraftChatMessageFormatNoRole")
    			: plugin.getConfig().getString("DiscordToMinecraftChatMessageFormat");
    	
    	DiscordSRV.broadcastMessage(formatMessage
    			.replaceAll("&([0-9a-z])", "\u00A7$1") // color coding
    			.replace("%message%", message)
    			.replace("%username%", event.getMessage().getAuthor().getUsername())
    			.replace("%toprole%", DiscordSRV.getTopRole(event))
    			.replace("%allroles%", DiscordSRV.getAllRoles(event))
    	);
	}
	private void handleConsole(MessageReceivedEvent event) {
		// general boolean for if command should be allowed
		Boolean allowed = false;
		// get if blacklist acts as whitelist
		Boolean DiscordConsoleChannelBlacklistActsAsWhitelist = plugin.getConfig().getBoolean("DiscordConsoleChannelBlacklistActsAsWhitelist");
		// get banned commands
		List<String> DiscordConsoleChannelBlacklistedCommands = (List<String>) plugin.getConfig().getList("DiscordConsoleChannelBlacklistedCommands");
		// convert to all lower case
		for (int i = 0; i < DiscordConsoleChannelBlacklistedCommands.size(); i++) DiscordConsoleChannelBlacklistedCommands.set(i, DiscordConsoleChannelBlacklistedCommands.get(i).toLowerCase());
		// get message for manipulation
		String requestedCommand = event.getMessage().getContent();
		// remove all spaces at the beginning of the requested command to handle pricks trying to cheat the system
		while (requestedCommand.substring(0, 1) == " ") requestedCommand = requestedCommand.substring(1);
		// select the first part of the requested command, being the main part of it we care about
		requestedCommand = requestedCommand.split(" ")[0].toLowerCase(); // *op* person
		// command is on whitelist, allow
		if (DiscordConsoleChannelBlacklistActsAsWhitelist && DiscordConsoleChannelBlacklistedCommands.contains(requestedCommand)) allowed = true; else allowed = false;
		// command is on blacklist, deny
		if (!DiscordConsoleChannelBlacklistActsAsWhitelist && DiscordConsoleChannelBlacklistedCommands.contains(requestedCommand)) allowed = false; else allowed = true;
		// return if command not allowed
		if (!allowed) return;
		
		// log command to console log file, if this fails the command is not executed for safety reasons unless this is turned off
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(new File(".").getAbsolutePath() + "/./" + plugin.getConfig().getString("DiscordConsoleChannelUsageLog")).getAbsolutePath(), true)))) {
			out.println("[" + new Date() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getUsername() + ": " + event.getMessage().getContent());
		}catch (IOException e) {plugin.getLogger().warning("Error logging console action to " + plugin.getConfig().getString("DiscordConsoleChannelUsageLog")); if (plugin.getConfig().getBoolean("CancelConsoleCommandIfLoggingFailed")) return;}
		
		// if server is running paper spigot it has to have it's own little section of code because it whines about timing issues
		if (Bukkit.getVersion().toLowerCase().contains("paperspigot")) Bukkit.getScheduler().runTask(plugin, new Runnable() {@Override public void run() { server.dispatchCommand(server.getConsoleSender(), event.getMessage().getContent()); }});
		// decent server software
		else server.dispatchCommand(server.getConsoleSender(), event.getMessage().getContent());
	}
}