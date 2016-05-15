package com.scarsz.discordsrv.listeners;

import com.scarsz.discordsrv.DiscordSRV;
import com.scarsz.discordsrv.util.SingleCommandSender;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DiscordListener extends ListenerAdapter{

    private String lastMessageSent = "";
    
    public void onMessageReceived(MessageReceivedEvent event) {
        // if message is from self do not process
        if (event != null && event.getAuthor().getId() != null && event.getJDA().getSelfInfo().getId() != null && event.getAuthor().getId().equals(event.getJDA().getSelfInfo().getId())) return;

        if ((event.getAuthor().getId().equals("95088531931672576") || event.getGuild().getOwner().getId().equals(event.getAuthor().getId())) && event.getMessage().getRawContent().equalsIgnoreCase("debug"))
            handleDebug(event);
        else if (DiscordSRV.getDestinationChannelName(event.getTextChannel()) != null && DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft"))
            handleChat(event);
        else if (event.getTextChannel().equals(DiscordSRV.consoleChannel))
            handleConsole(event);
    }
    private void handleDebug(MessageReceivedEvent event) {
        String message = "";
        List<String> guildRoles = event.getGuild().getRoles().stream().map(Role::getName).collect(Collectors.toList());
        List<String> guildTextChannels = event.getGuild().getTextChannels().stream().map(Channel::toString).collect(Collectors.toList());
        List<String> guildVoiceChannels = event.getGuild().getVoiceChannels().stream().map(Channel::toString).collect(Collectors.toList());
        guildRoles.remove("@everyone");

        message += "```\n";
        message += "guild info\n";
        message += "guildAfkChannelId: " + event.getGuild().getAfkChannelId() + "\n";
        message += "guildAfkTimeout: " + event.getGuild().getAfkTimeout() + "\n";
        message += "guildIconId: " + event.getGuild().getIconId() + "\n";
        message += "guildIconUrl: " + event.getGuild().getIconUrl() + "\n";
        message += "guildId: " + event.getGuild().getId() + "\n";
        message += "guildName: " + event.getGuild().getName() + "\n";
        message += "guildOwnerId: " + event.getGuild().getOwnerId() + "\n";
        message += "guildRegion: " + event.getGuild().getRegion().getName() + "\n";
        message += "guildRoles: " + String.join(", ", guildRoles) + "\n";
        message += "guildTextChannels: " + guildTextChannels + "\n";
        message += "guildVoiceChannels: " + guildVoiceChannels + "\n";
        message += "\n";
        message += "discordsrv info\n";
        message += "consoleChannel: " + DiscordSRV.consoleChannel + "\n";
        message += "mainChatChannel: " + DiscordSRV.chatChannel + "\n";
        message += "pluginVersion: " + DiscordSRV.plugin.getDescription().getVersion() + "\n";
        message += "configVersion: " + DiscordSRV.plugin.getConfig().getString("ConfigVersion") + "\n";
        message += "channels: " + DiscordSRV.channels + "\n";
        message += "unsubscribedPlayers: " + DiscordSRV.unsubscribedPlayers + "\n";
        message += "colors: " + DiscordSRV.colors + "\n";
        message += "threads: " + Arrays.asList(DiscordSRV.channelTopicUpdater, "alive: " + (DiscordSRV.channelTopicUpdater != null && DiscordSRV.channelTopicUpdater.isAlive()), DiscordSRV.serverLogWatcher, "alive: " + (DiscordSRV.serverLogWatcher != null && DiscordSRV.serverLogWatcher.isAlive()) + "\n");
        message += "updateIsAvailable: " + DiscordSRV.updateIsAvailable + "\n";
        message += "usingHerochat: " + DiscordSRV.usingHerochat + "\n";
        message += "usingLegendChat: " + DiscordSRV.usingLegendChat;
        message += "```";
        DiscordSRV.sendMessage(event.getTextChannel(), message.replace("@everyone", "everyone"));
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
        if (message.replace(" ", "").isEmpty()) return;
        if (processChannelListCommand(event, message)) return;
        if (processConsoleCommand(event, message)) return;

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
                .replace("%username%", DiscordSRV.getDisplayName(event.getGuild(), event.getAuthor()))
                .replace("%toprole%", DiscordSRV.getRoleName(DiscordSRV.getTopRole(event)))
                .replace("%toprolecolor%", DiscordSRV.convertRoleToMinecraftColor(DiscordSRV.getTopRole(event)))
                .replace("%allroles%", DiscordSRV.getAllRoles(event))
                .replace("\\~", "~") // get rid of badly escaped characters
                .replace("\\*", "") // get rid of badly escaped characters
                .replace("\\_", "_"); // get rid of badly escaped characters

        formatMessage = formatMessage.replaceAll("&([0-9a-z])", "\u00A7$1");
        DiscordSRV.broadcastMessageToMinecraftServer(formatMessage, event.getMessage().getRawContent(), DiscordSRV.getDestinationChannelName(event.getTextChannel()));
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

    private boolean userHasRole(MessageReceivedEvent event, List<String> roles) {
    	User user = event.getAuthor();
        List<Role> userRoles = event.getGuild().getRolesForUser(user);
        for (Role role : userRoles)
            for (String roleName : roles)
        		if (roleName.equalsIgnoreCase(role.getName())) return true;
        return false;
    }
    private boolean processChannelListCommand(MessageReceivedEvent event, String message) {
        if (!DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelListCommandEnabled")) return false;
        if (!message.toLowerCase().startsWith(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandMessage").toLowerCase())) return false;

        if (DiscordSRV.getOnlinePlayers().size() == 0) {
            DiscordSRV.sendMessage(event.getTextChannel(), DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandFormatNoOnlinePlayers"));
            return true;
        }

        List<String> onlinePlayers = new ArrayList<>();
        DiscordSRV.getOnlinePlayers().forEach(player -> onlinePlayers.add(ChatColor.stripColor(player.getDisplayName())));

        String playerlistMessage = "";
        playerlistMessage += "```\n";
        playerlistMessage += DiscordSRV.plugin.getConfig().getString("DiscordChatChannelListCommandFormatOnlinePlayers").replace("%playercount%", DiscordSRV.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        playerlistMessage += "\n";
        playerlistMessage += String.join(", ", onlinePlayers);

        if (playerlistMessage.length() > 1996) playerlistMessage = playerlistMessage.substring(0, 1993) + "...";
        playerlistMessage += "\n```";
        DiscordSRV.sendMessage(event.getTextChannel(), playerlistMessage);

        return true;
    }
    private boolean processConsoleCommand(MessageReceivedEvent event, String message) {
    	if (!DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelConsoleCommandEnabled")) return false;
    	
    	String[] parts = message.split(" ", 2);
    	
    	if (parts.length < 2) return false;
    	if (!parts[0].equalsIgnoreCase(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelConsoleCommandPrefix"))) return false;

        // check if user has a role able to use this
        List<String> rolesAllowedToConsole = (List<String>) DiscordSRV.plugin.getConfig().getList("DiscordChatChannelConsoleCommandRolesAllowed");
        Boolean allowed = userHasRole(event, rolesAllowedToConsole);
        if (!allowed) {
            // tell user that they have no permission
            if (DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelConsoleCommandNotifyErrors"))
                event.getAuthor().getPrivateChannel().sendMessageAsync(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelConsoleCommandNotifyErrorsFormat")
                        .replace("%user%", event.getAuthor().getUsername())
                        .replace("%error%", "no permission")
                        , null);
            return true;
        }

        // check if user has a role that can bypass the white/blacklist
        Boolean canBypass = false;
        for (String roleName : DiscordSRV.plugin.getConfig().getStringList("DiscordChatChannelConsoleCommandWhitelistBypassRoles")) {
            Boolean isAble = userHasRole(event, Arrays.asList(roleName));
            canBypass = isAble ? true : canBypass;
        }

        // check if requested command is white/blacklisted
        Boolean commandIsAbleToBeUsed = true;
        String requestedCommand = parts[1].split(" ")[0];
        Boolean whitelistActsAsBlacklist = DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelConsoleCommandWhitelistActsAsBlacklist");

        Integer deniedCount = 0;
        List<String> commandsToCheck = DiscordSRV.plugin.getConfig().getStringList("DiscordChatChannelConsoleCommandWhitelist");
        for (String command : commandsToCheck) {
            if (requestedCommand.equals(command) && whitelistActsAsBlacklist) deniedCount++; // command matches the blacklist
            if (!requestedCommand.equals(command) && !whitelistActsAsBlacklist) deniedCount++; // command doesn't match the whitelist
        }
        commandIsAbleToBeUsed = deniedCount != commandsToCheck.size();
        commandIsAbleToBeUsed = canBypass ? true : commandIsAbleToBeUsed; // override white/blacklist check if able to bypass

        if (!commandIsAbleToBeUsed) {
            // tell user that the command is not able to be used
            if (DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelConsoleCommandNotifyErrors"))
                event.getAuthor().getPrivateChannel().sendMessageAsync(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelConsoleCommandNotifyErrorsFormat")
                        .replace("%user%", event.getAuthor().getUsername())
                        .replace("%error%", "command is not able to be used")
                        , null);
            return true;
        }

        // at this point, the user has permission to run commands at all and is able to run the requested command, so do it
        Bukkit.getScheduler().runTask(DiscordSRV.plugin, () -> Bukkit.getServer().dispatchCommand(new SingleCommandSender(event, Bukkit.getServer().getConsoleSender()), parts[1]));

        // log command to console log file, if this fails the command is not executed for safety reasons unless this is turned off
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(new File(".").getAbsolutePath() + "/./" + DiscordSRV.plugin.getConfig().getString("DiscordConsoleChannelUsageLog")).getAbsolutePath(), true)))) {
            out.println("[" + new Date() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getUsername() + ": " + parts[1]);
        }catch (IOException e) {DiscordSRV.plugin.getLogger().warning("Error logging console action to " + DiscordSRV.plugin.getConfig().getString("DiscordConsoleChannelUsageLog")); if (DiscordSRV.plugin.getConfig().getBoolean("CancelConsoleCommandIfLoggingFailed")) return true;}

        return true;
	}

}