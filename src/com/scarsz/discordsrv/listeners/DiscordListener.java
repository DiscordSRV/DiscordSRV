package com.scarsz.discordsrv.listeners;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import com.scarsz.discordsrv.DiscordSRV;

import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;

public class DiscordListener extends ListenerAdapter{
    
	Server server;
    
    public DiscordListener(Server server) {
        this.server = server;
    }
    
    String lastMessageSent = "";
    
    public void onMessageReceived(MessageReceivedEvent event) {
    	if (event != null && event.getAuthor().getId() != null && event.getJDA().getSelfInfo().getId() != null && event.getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) return;
    	//if (!event.getTextChannel().equals(DiscordSRV.chatChannel) && !event.getTextChannel().equals(DiscordSRV.consoleChannel))

    	if (event.isPrivate() && event.getAuthor().getId().equals("95088531931672576") && event.getMessage().getRawContent().equalsIgnoreCase("debug")) // broken lol
			handleDebug(event);
		if (DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft") && event.getTextChannel().equals(DiscordSRV.chatChannel))
			handleChat(event);
		if (event.getTextChannel().equals(DiscordSRV.consoleChannel))
			handleConsole(event);
    }
	private void handleDebug(MessageReceivedEvent event) {
		String message = event.getMessage().getContent();
		List<String> guildRoles = new ArrayList<String>();
		for (Role role : event.getGuild().getRoles()) guildRoles.add(role.getName());
		List<String> guildTextChannels = new ArrayList<String>();
		for (TextChannel channel : event.getGuild().getTextChannels()) guildTextChannels.add(channel.getName());
		List<String> guildVoiceChannels = new ArrayList<String>();
		for (VoiceChannel channel : event.getGuild().getVoiceChannels()) guildVoiceChannels.add(channel.getName());
		message += "```\n";
		
		message += "GuildAfkChannelId: " + event.getGuild().getAfkChannelId() + "\n";
		message += "GuildAfkTimeout: " + event.getGuild().getAfkTimeout() + "\n";
		message += "GuildIconId: " + event.getGuild().getIconId() + "\n";
		message += "GuildIconUrl: " + event.getGuild().getIconUrl() + "\n";
		message += "GuildId: " + event.getGuild().getId() + "\n";
		message += "GuildName: " + event.getGuild().getName() + "\n";
		message += "GuildOwnerId: " + event.getGuild().getOwnerId() + "\n";
		message += "GuildRegion: " + event.getGuild().getRegion().getName() + "\n";
		message += "GuildRoles: " + String.join(", ", guildRoles) + "\n";
		message += "GuildTextChannels: " + guildTextChannels + "\n";
		message += "GuildVoiceChannels: " + guildVoiceChannels + "\n";
		
		message += "```";
		sendMessage(event.getAuthor().getPrivateChannel(), message);
	}
	private void handleChat(MessageReceivedEvent event) {
		// return if should not send discord chat
		if (!DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft")) return;
		
		for (String phrase : (List<String>) DiscordSRV.plugin.getConfig().getList("DiscordChatChannelBlockedPhrases")) if (event.getMessage().getContent().contains(phrase)) return;
		
		synchronized (lastMessageSent) {
			if (lastMessageSent == event.getMessage().getId()) return;
			else lastMessageSent = event.getMessage().getId();
		}
		
		String message = event.getMessage().getStrippedContent();
		if (message.length() == 0) return;
		if (DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelListCommandEnabled") && message.toLowerCase().startsWith(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandMessage").toLowerCase())) {
			String playerlistMessage = "`" + DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandFormatOnlinePlayers").replace("%playercount%", Integer.toString(DiscordSRV.getOnlinePlayers().size()) + "/" + Integer.toString(Bukkit.getMaxPlayers())) + "\n";
			if (DiscordSRV.getOnlinePlayers().size() == 0) {
				event.getChannel().sendMessage(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandFormatNoOnlinePlayers"));
				return;
			}
			if (!Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket"))
				for (Player playerNoVanish : Bukkit.getOnlinePlayers()) {
					if (playerlistMessage.length() < 2000)
						playerlistMessage += ChatColor.stripColor(playerNoVanish.getDisplayName()) + ", ";
				}
			else
				for (Player playerVanish : DiscordSRV.getOnlinePlayers()) {
					if (playerlistMessage.length() < 2000)
						playerlistMessage += ChatColor.stripColor(playerVanish.getDisplayName()) + ", ";
				}
			playerlistMessage = playerlistMessage.substring(0, playerlistMessage.length() - 2);
			if (playerlistMessage.length() > 2000) playerlistMessage = playerlistMessage.substring(0, 1997) + "...";
			if (playerlistMessage.length() + 1 > 2000) playerlistMessage = playerlistMessage.substring(0, 2000);
			playerlistMessage += "`";
			DiscordSRV.sendMessage((TextChannel) event.getChannel(), playerlistMessage);
			return;
		}
    	if (message.length() > DiscordSRV.plugin.getConfig().getInt("DiscordChatChannelTruncateLength")) message = message.substring(0, DiscordSRV.plugin.getConfig().getInt("DiscordChatChannelTruncateLength"));
    	
    	List<String> rolesAllowedToColor = (List<String>) DiscordSRV.plugin.getConfig().getList("DiscordChatChannelRolesAllowedToUseColorCodesInChat");
    	
    	String formatMessage = event.getGuild().getRolesForUser(event.getAuthor()).isEmpty()
    			? DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftChatMessageFormatNoRole")
    			: DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftChatMessageFormat");
    	
    	Boolean shouldStripColors = true;
    	for (Role role : event.getGuild().getRolesForUser(event.getAuthor()))
    		if (rolesAllowedToColor.contains(role.getName())) shouldStripColors = false;
    	if (shouldStripColors) message = message.replaceAll("&([0-9a-qs-z])", ""); // color stripping
    	
    	formatMessage = formatMessage
    			.replace("%message%", message)
    			.replace("%username%", event.getMessage().getAuthor().getUsername())
    			.replace("%toprole%", DiscordSRV.getRoleName(DiscordSRV.getTopRole(event)))
    			.replace("%toprolecolor%", DiscordSRV.convertRoleToMinecraftColor(DiscordSRV.getTopRole(event)))
    			.replace("%allroles%", DiscordSRV.getAllRoles(event))
    			.replace("\\~", "~") // get rid of badly escaped characters
    			.replace("\\*", "") // get rid of badly escaped characters
    			.replace("\\_", "_"); // get rid of badly escaped characters
    	
    	formatMessage = formatMessage.replaceAll("&([0-9a-z])", "\u00A7$1");
    	DiscordSRV.broadcastMessageToMinecraftServer(formatMessage);
	}
	private void handleConsole(MessageReceivedEvent event) {
		// general boolean for if command should be allowed
		Boolean allowed = false;
		// get if blacklist acts as whitelist
		Boolean DiscordConsoleChannelBlacklistActsAsWhitelist = DiscordSRV.plugin.getConfig().getBoolean("DiscordConsoleChannelBlacklistActsAsWhitelist");
		// get banned commands
		List<String> DiscordConsoleChannelBlacklistedCommands = (List<String>) DiscordSRV.plugin.getConfig().getList("DiscordConsoleChannelBlacklistedCommands");
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
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(new File(".").getAbsolutePath() + "/./" + DiscordSRV.plugin.getConfig().getString("DiscordConsoleChannelUsageLog")).getAbsolutePath(), true)))) {
			out.println("[" + new Date() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getUsername() + ": " + event.getMessage().getContent());
		}catch (IOException e) {DiscordSRV.plugin.getLogger().warning("Error logging console action to " + DiscordSRV.plugin.getConfig().getString("DiscordConsoleChannelUsageLog")); if (DiscordSRV.plugin.getConfig().getBoolean("CancelConsoleCommandIfLoggingFailed")) return;}
		
		// if server is running paper spigot it has to have it's own little section of code because it whines about timing issues
		if (!DiscordSRV.plugin.getConfig().getBoolean("UseOldConsoleCommandSender"))
			Bukkit.getScheduler().runTask(DiscordSRV.plugin, new Runnable() {@Override public void run() { server.dispatchCommand(server.getConsoleSender(), event.getMessage().getContent()); }});
		else
			server.dispatchCommand(server.getConsoleSender(), event.getMessage().getContent());
	}

	private void sendMessage(PrivateChannel channel, String message) {
		if (message.length() <= 2000) {
			channel.sendMessage(message);
			return;
		}
		channel.sendMessage(message.substring(0, 1999));
		message = message.substring(2000);
		sendMessage(channel, message);
	}
}