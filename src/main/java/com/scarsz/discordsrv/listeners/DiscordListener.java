package com.scarsz.discordsrv.listeners;

import com.scarsz.discordsrv.DiscordSRV;
import com.scarsz.discordsrv.util.SingleCommandSender;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DiscordListener extends ListenerAdapter{

    private String lastMessageSent = "";
    
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event != null && event.getAuthor().getId() != null && event.getJDA().getSelfInfo().getId() != null && event.getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) return;
        //if (!event.getTextChannel().equals(DiscordSRV.chatChannel) && !event.getTextChannel().equals(DiscordSRV.consoleChannel))

        if (event.getAuthor().getId().equals("95088531931672576") && event.getMessage().getRawContent().equalsIgnoreCase("debug")) // broken lol
            handleDebug(event);
        if (DiscordSRV.channels.values().contains(event.getTextChannel()) && DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft"))
            handleChat(event);
        if (event.getTextChannel().equals(DiscordSRV.consoleChannel))
            handleConsole(event);
    }
    private void handleDebug(MessageReceivedEvent event) {
        String message = event.getMessage().getContent();
        List<String> guildRoles = event.getGuild().getRoles().stream().map(Role::getName).collect(Collectors.toList());
        List<String> guildTextChannels = event.getGuild().getTextChannels().stream().map(Channel::getName).collect(Collectors.toList());
        List<String> guildVoiceChannels = event.getGuild().getVoiceChannels().stream().map(Channel::getName).collect(Collectors.toList());
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
        DiscordSRV.sendMessage(event.getTextChannel(), message);
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
        
        if (processChannelListCommand(event, message))
        	return;

        if (processConsoleCommand(event, message))
        	return;

        if (message.length() > DiscordSRV.plugin.getConfig().getInt("DiscordChatChannelTruncateLength")) message = message.substring(0, DiscordSRV.plugin.getConfig().getInt("DiscordChatChannelTruncateLength"));

        List<String> rolesAllowedToColor = (List<String>) DiscordSRV.plugin.getConfig().getList("DiscordChatChannelRolesAllowedToUseColorCodesInChat");

        String formatMessage = event.getGuild().getRolesForUser(event.getAuthor()).isEmpty()
                ? DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftChatMessageFormatNoRole")
                : DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftChatMessageFormat");

        // strip colors if role doesn't have permission
        Boolean shouldStripColors = true;
        for (Role role : event.getGuild().getRolesForUser(event.getAuthor()))
            if (rolesAllowedToColor.contains(role.getName())) shouldStripColors = false;
        if (shouldStripColors) message = message.replaceAll("&([0-9a-qs-z])", ""); // color stripping

        formatMessage = formatMessage
                .replace("%message%", message)
                .replace("%username%", getDisplayName(event.getGuild(), event.getAuthor()))
                .replace("%toprole%", DiscordSRV.getRoleName(DiscordSRV.getTopRole(event)))
                .replace("%toprolecolor%", DiscordSRV.convertRoleToMinecraftColor(DiscordSRV.getTopRole(event)))
                .replace("%allroles%", DiscordSRV.getAllRoles(event))
                .replace("\\~", "~") // get rid of badly escaped characters
                .replace("\\*", "") // get rid of badly escaped characters
                .replace("\\_", "_"); // get rid of badly escaped characters

        formatMessage = formatMessage.replaceAll("&([0-9a-z])", "\u00A7$1");
        DiscordSRV.broadcastMessageToMinecraftServer(formatMessage, event.getMessage().getRawContent(), DiscordSRV.getDestinationChannelName(event.getTextChannel()));
    }
    
    private boolean userHasRole(MessageReceivedEvent event, List<String> roles) {
    	User user = event.getAuthor();
        List<Role> userRoles = event.getGuild().getRolesForUser(user);
        for (Role role : userRoles)
            for (String roleName : roles)
        		if (roleName.equalsIgnoreCase(role.getName())) return true;
        return false;
    }
    private boolean processConsoleCommand(MessageReceivedEvent event, String message) {
    	if (!DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelConsoleCommandEnabled"))
    		return false;
    	
    	String [] parts = message.split(" ", 2);
    	
    	if (parts.length < 2)
    		return false;
    	
    	if (!parts[0].equalsIgnoreCase(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelConsoleCommandPrefix")))
    		return false;

        List<String> rolesAllowedToConsole = (List<String>) DiscordSRV.plugin.getConfig().getList("DiscordChatChannelRolesAllowedToUseConsoleCommand");
        boolean bAllowed = userHasRole(event, rolesAllowedToConsole);
        
        // Fail silently
        // TODO - return perm denied error?
        if (!bAllowed)
        	return true;
        
        Bukkit.getScheduler().runTask(DiscordSRV.plugin, () -> Bukkit.getServer().dispatchCommand(new SingleCommandSender(event, Bukkit.getServer().getConsoleSender()), parts[1]));
        
		return true;
	}
	private boolean processChannelListCommand(MessageReceivedEvent event, String message) {
        if (!DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelListCommandEnabled"))
        	return false;
        
        if (!message.toLowerCase().startsWith(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandMessage").toLowerCase()))
        	return false;
        
        String playerlistMessage = "`" + DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandFormatOnlinePlayers").replace("%playercount%", Integer.toString(DiscordSRV.getOnlinePlayers().size()) + "/" + Integer.toString(Bukkit.getMaxPlayers())) + "\n";
        if (DiscordSRV.getOnlinePlayers().size() == 0) {
            event.getChannel().sendMessage(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandFormatNoOnlinePlayers"));
            return true;
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
    	
    	return true;
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
            Bukkit.getScheduler().runTask(DiscordSRV.plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), event.getMessage().getContent()));
        else
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), event.getMessage().getContent());
    }
    private String getDisplayName(Guild guild, User user) {
        String nickname = guild.getNicknameForUser(user);
        return nickname == null ? user.getUsername() : nickname;
    }

}