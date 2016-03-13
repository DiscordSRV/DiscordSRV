package com.scarsz.discordsrv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.exceptions.RateLimitedException;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class DiscordSRV extends JavaPlugin {
	public static JDA api;
	public static Plugin plugin;
	public static Boolean ready = false;
	public static ServerLogWatcher serverLogWatcher;
	public static List<String> unsubscribedPlayers = new ArrayList<>();
	
	public static Guild guild;
	public static TextChannel chatChannel, consoleChannel;
	
	// debug stuff (broken atm)
	public static int DebugCancelledMinecraftChatEventsCount, DebugCancelledMinecraftChatEventsCountTotal = 0; // amount of cancelled chat events registered in session
	public static int DebugMinecraftChatEventsCount, DebugMinecraftChatEventsCountTotal = 0; // amount of minecraft chat events registered in session
	public static int DebugDiscordChatEventsCount, DebugDiscordChatEventsCountTotal = 0; // amount of discord chat events registered in session
	public static int DebugDiscordChatChannelEventsCount, DebugDiscordChatChannelEventsCountTotal = 0; // amount of chat channel events registered in session
	public static int DebugDiscordConsoleChannelEventsCount, DebugDiscordConsoleChannelEventsCountTotal = 0; // amount of console channel registered in session
	public static int DebugMinecraftCommandsCount, DebugMinecraftCommandsCountTotal = 0; // amount of times a /discordsrv command has been used
	public static int DebugConsoleLogLinesProcessed, DebugConsoleLogLinesProcessedTotal = 0; // amount of times a line has been taken from logs/latest.log
	public static int DebugConsoleMessagesSent, DebugConsoleMessagesSentTotal = 0; // amount of times a message has been sent with console log contents
	public static int DebugConsoleMessagesNull, DebugConsoleMessagesNullTotal = 0; // amount of times a console log line has been null
	public static int DebugConsoleMessagesNotNull, DebugConsoleMessagesNotNullTotal = 0; // amount of times a console log line has not been null
	
	public void onEnable() {
		// set static plugin variable for discordsrv methods to use
		plugin = this;
		
		// load config, create if doesn't exist, update config if old
		reloadConfig();
		if (!new File(getDataFolder(), "config.yml").exists()) {
			saveResource("config.yml", false);
			reloadConfig();
		}
		if (getConfig().getDouble("ConfigVersion") < Double.parseDouble(getDescription().getVersion()) || !getConfig().isSet("ConfigVersion"))
			try {
				Files.move(new File(getDataFolder(), "config.yml"), new File(getDataFolder(), "config.yml-build." + getConfig().getDouble("ConfigVersion") + ".old"));
				getLogger().info("Your DiscordSRV config file was outdated; a new one has been made for the new build.");
			} catch (IOException ex) {}
		if (!new File(getDataFolder(), "config.yml").exists()) saveResource("config.yml", false);
		
		// update check
		if (!plugin.getConfig().getBoolean("UpdateCheckDisabled")){
			double latest = Double.parseDouble(requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/master/latestbuild"));
			if (latest > Double.parseDouble(getDescription().getVersion())) {
				getLogger().warning(System.lineSeparator() + System.lineSeparator() + "The current build of DiscordSRV is outdated! Get build " + latest + " at http://scarsz.com/discordsrv/" + System.lineSeparator() + System.lineSeparator());
			}
			
			double minimum = Double.parseDouble(requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/master/minimumbuild"));
			if (minimum > Double.parseDouble(getDescription().getVersion())) {
				getLogger().warning(System.lineSeparator() + System.lineSeparator() + "The current build of DiscordSRV does not meet the minimum! DiscordSRV will not start. Get build " + latest + " at http://dev.bukkit.org/server-mods/discordsrv/" + System.lineSeparator() + System.lineSeparator());
				Bukkit.getPluginManager().disablePlugin(this);
				return;
			}
		}
		
		// login to discord
		buildJda();
		if (api == null) {
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		// print the servers that the bot can see
		getLogger().info("DiscordSRV is able to see the following servers:");
		for (Guild server : api.getGuilds()) {
			getLogger().info("Server: " + server.getName() + " [ID " + server.getId() + "]");
		}
		getLogger().info("DiscordSRV is able to see the following channels from specified server:");
		for (Guild server : api.getGuilds()) {
			for (Channel channel : server.getTextChannels()){
				getLogger().info("Channel: " + channel.getName() + " [ID " + channel.getId() + "]");
			}
		}
		
		// check & get location info
		guild = api.getGuildById(getConfig().getString("DiscordServerId"));
		chatChannel = api.getTextChannelById(getConfig().getString("DiscordChatChannelId"));
		consoleChannel = api.getTextChannelById(getConfig().getString("DiscordConsoleChannelId"));
		if (guild == null) {
			getLogger().severe(System.lineSeparator() + System.lineSeparator() + "DiscordSRV could not find the server ID you specified- set it in the config. Plugin being disabled." + System.lineSeparator() + System.lineSeparator());
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		if (chatChannel == null) getLogger().warning("Specified chat channel ID from config could not be found");
		if (consoleChannel == null) getLogger().warning("Specified console channel ID from config could not be found");
		if (chatChannel == null && consoleChannel == null) {
			getLogger().severe("Chat and console channels are both unavailable, disabling");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		// send startup message if enabled
		if (plugin.getConfig().getBoolean("DiscordChatChannelServerStartupMessageEnabled"))
			sendMessage(chatChannel, plugin.getConfig().getString("DiscordChatChannelServerStartupMessage"));
		
		// in-game chat events
		getServer().getPluginManager().registerEvents(new ChatListener(api, this), this);
		getServer().getPluginManager().registerEvents(new PlayerDeathListener(api, this), this);
		// console streaming thread
		if (getConfig().getBoolean("DiscordConsoleChannelEnabled")) {
			serverLogWatcher = new ServerLogWatcher(api, this);
			serverLogWatcher.start();
		}
		
		// player join/leave message events
		if (getConfig().getBoolean("MinecraftPlayerJoinMessageEnabled"))
			getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(api, this), this);
		
		// player achievement events
		if (getConfig().getBoolean("MinecraftPlayerAchievementMessagesEnabled"))
			getServer().getPluginManager().registerEvents(new AchievementListener(api, this), this);
		
		// enable metrics
		if (!getConfig().getBoolean("MetricsDisabled"))
			try {
				Metrics metrics = new Metrics(this);
				metrics.start();
			} catch (IOException e) {
				getLogger().warning("Unable to start metrics. Oh well.");
			}
		
		// - chat channel
		if (getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft") || getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")){
			if (!testChannel(chatChannel)) getLogger().warning("Channel \"" + chatChannel + "\" was not accessible");
			if (testChannel(chatChannel) && !chatChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + chatChannel.getName());
			if (testChannel(chatChannel) && !chatChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + chatChannel.getName());
		}
		// - console channel
		if (getConfig().getBoolean("DiscordConsoleChannelEnabled")){
			if (!testChannel(consoleChannel)) getLogger().warning("Channel \"" + consoleChannel + "\" was not accessible");
			if (testChannel(consoleChannel) && !consoleChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + consoleChannel.getName());
			if (testChannel(consoleChannel) && !consoleChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + consoleChannel.getName());	
		}
		
		// load unsubscribed users
		if (new File(getDataFolder(), "unsubscribed.txt").exists())
			try {
				for (String id : FileUtils.readFileToString(new File(getDataFolder(), "unsubscribed.txt")).split("\n"))
					unsubscribedPlayers.add(id);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	private void buildJda() {
		// shutdown if already started
		if (api != null) try { api.shutdown(false); } catch (Exception e) { e.printStackTrace(); }
		
		try {
			api = new JDABuilder(getConfig().getString("DiscordEmail"), getConfig().getString("DiscordPassword"))
					.addListener(new DiscordListener(getServer(), this))
					.setAutoReconnect(true)
					.setAudioEnabled(false)
					.buildBlocking();
		} catch (LoginException | IllegalArgumentException | InterruptedException e) {
			getLogger().severe(System.lineSeparator() + System.lineSeparator() + "Error building DiscordSRV: " + e.getMessage() + System.lineSeparator() + System.lineSeparator());
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		// game status
		if (!getConfig().getString("DiscordGameStatus").isEmpty())
			api.getAccountManager().setGame(getConfig().getString("DiscordGameStatus"));
	}
	public void onDisable() {
		// kill server log watcher
		if (serverLogWatcher != null && !serverLogWatcher.isInterrupted())
			serverLogWatcher.interrupt();
		serverLogWatcher = null;
		
		// disconnect from discord
		try { api.shutdown(false); } catch (Exception e) { getLogger().info("Discord shutting down before logged in"); }
		api = null;
		
		// save unsubscribed users
		if (new File(getDataFolder(), "unsubscribed.txt").exists()) new File(getDataFolder(), "unsubscribed.txt").delete();
		String players = "";
		for (String id : unsubscribedPlayers) players += id + "\n";
		if (players.length() > 0){
			players = players.substring(0, players.length() - 1);
			try {
				FileUtils.writeStringToFile(new File(getDataFolder(), "unsubscribed.txt"), players);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		DebugMinecraftCommandsCount++;
		if (args.length == 0){
			if (!sender.isOp())
				sender.sendMessage("/discordsrv toggle/subscribe/unsubscribe");
			else
				sender.sendMessage("/discordsrv reload/rebuild/debug/toggle/subscribe/unsubscribe");
			return true;
		}
		if (args[0].equalsIgnoreCase("reload")) {
			if (!sender.isOp()) return true;
			reloadConfig();
			sender.sendMessage("DiscordSRV config has been reloaded. Some config options require a restart.");
			return true;
		}
		if (args[0].equalsIgnoreCase("debug")) {
			if (!sender.isOp()) return true;
			FileReader fr = null;
			try {
				fr = new FileReader(new File(new File(".").getAbsolutePath() + "/logs/latest.log").getAbsolutePath());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			BufferedReader br = new BufferedReader(fr);
		    
		    List<String> discordsrvMessages = new ArrayList<String>();
		    discordsrvMessages.add(ChatColor.RED + "Lines for DiscordSRV from latest.log:");
		    Boolean done = false;
		    while (!done)
		    {
		    	String line = null;
				try {
					line = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (line == null) done = true;
				if (line != null && line.contains("DiscordSRV")) discordsrvMessages.add(line);
		    }
		    discordsrvMessages.add(ChatColor.AQUA + "Version: " + ChatColor.RESET + Bukkit.getVersion());
		    discordsrvMessages.add(ChatColor.AQUA + "Bukkit version: " + ChatColor.RESET + Bukkit.getBukkitVersion());
		    discordsrvMessages.add(ChatColor.AQUA + "OS: " + ChatColor.RESET + System.getProperty("os.name"));
		    for (String message : discordsrvMessages) sender.sendMessage(message);
		    try { fr.close(); } catch (IOException e) { e.printStackTrace(); }
			return true;
		}
		if (args[0].equalsIgnoreCase("rebuild")) {
			if (!sender.isOp()) return true;
			//buildJda();
			sender.sendMessage("Disabled because no workie");
			return true;
		}
		
		if (!(sender instanceof Player)) return true;
		Player senderPlayer = (Player) sender;
		if (args[0].equalsIgnoreCase("toggle")){
			Boolean subscribed = getSubscribed(senderPlayer.getUniqueId());
			setSubscribed(senderPlayer.getUniqueId(), !subscribed);
			
			String subscribedMessage = getSubscribed(senderPlayer.getUniqueId()) ? "subscribed" : "unsubscribed";
			sender.sendMessage(ChatColor.AQUA + "You have been " + subscribedMessage + " to Discord messages.");
		}
		if (args[0].equalsIgnoreCase("subscribe")){
			setSubscribed(senderPlayer.getUniqueId(), true);
			sender.sendMessage(ChatColor.AQUA + "You have been subscribed to Discord messages.");
		}
		if (args[0].equalsIgnoreCase("unsubscribe")){
			setSubscribed(senderPlayer.getUniqueId(), false);
			sender.sendMessage(ChatColor.AQUA + "You are no longer subscribed to Discord messages.");
		}
		return true; 
	}
	
	public static String requestHttp(String requestUrl) {
		String sourceLine = null;

        URL address = null;
		try {
			address = new URL(requestUrl);
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		}
		
        InputStreamReader pageInput = null;
		try {
			pageInput = new InputStreamReader(address.openStream());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
        BufferedReader source = new BufferedReader(pageInput);

        try {
            sourceLine = source.readLine();
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
        
		return sourceLine;
	}
    public static Boolean testChannel(TextChannel channel) {
    	return channel != null;
    }
	public static void sendMessage(TextChannel channel, String message) {
		if (api == null || channel == null || (!channel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ) || !channel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE))) return;
		message = ChatColor.stripColor(message).replace("[m", "").replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "").replaceAll("\\[[0-9]{1,3}m", "").replace("[m", "").replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "").replaceAll("\\[[0-9]{1,3}m", "");
        
		for (Object phrase : plugin.getConfig().getList("DiscordChatChannelCutPhrases")) {
        	if (message.contains((String) phrase)) {
        		message = message.replace((String) phrase, "");
        	}
        }
        
		Boolean sent = false;
		Integer tries = 0;
		if (message.length() > 2000) {
			plugin.getLogger().warning("Tried sending message with length of " + message.length() + " (" + (message.length() - 2000) + " over limit)");
			message = message.substring(0, 1999);
		}
		while (!sent && tries < 3) {
			try {
				channel.sendMessage(message);
				sent = true;
			} catch (RateLimitedException e) {
				tries++;
				verboseWait(e.getTimeout());
			}
		}
	}
	public static void verboseWait(long time) {
		if (plugin.getConfig().getBoolean("RateLimitSleepVerbose")){
			long intervals = time / 4;
			while (time > intervals){
				System.out.println("Waiting " + time + " ms");
				try { Thread.sleep(intervals); } catch (InterruptedException e) {};
				time = time - intervals;
			}
			System.out.println("Waiting " + time + " ms");
		}
		else try { Thread.sleep(time); } catch (InterruptedException e) {};
	}
	public static String getPrimaryGroup(Player player) {
		if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return "";
		
		RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> service = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		return service != null ? service.getProvider().getPrimaryGroup(player) == "default" ? "" : service.getProvider().getPrimaryGroup(player) : "";
	}
	public static String getAllRoles(MessageReceivedEvent event) {
		String roles = "";
		for (Role role : event.getGuild().getRolesForUser(event.getAuthor())){
			roles += role.getName() + plugin.getConfig().getString("DiscordToMinecraftAllRolesSeperator");
		}
		if (!roles.isEmpty()) roles = roles.substring(0, roles.length() - plugin.getConfig().getString("DiscordToMinecraftAllRolesSeperator").length());
		return roles;
	}
	public static Role getTopRole(MessageReceivedEvent event) {
		Role highestRole = null;
		for (Role role : event.getGuild().getRolesForUser(event.getAuthor())){
			if (highestRole == null) highestRole = role;
			else if (highestRole.getPosition() < role.getPosition()) highestRole = role;
		}
		return highestRole;
	}
	public static String getRoleName(Role role) {
		return role == null ? "" : role.getName();
	}
	public static List<Player> getOnlinePlayers() {
		List<Player> players = new ArrayList<Player>(Bukkit.getOnlinePlayers());
		List<Player> playersToRemove = new ArrayList<Player>();
		for (Player player : players){
			if (Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket") && VanishedPlayerCheck.checkPlayerIsVanished(player.getName(), plugin))
				playersToRemove.add(player);
		}
		players.removeAll(playersToRemove);
		return players;
	}
	public static boolean getSubscribed(UUID uniqueId) {
		return !unsubscribedPlayers.contains(uniqueId.toString());
	}
	public static void setSubscribed(UUID uniqueId, boolean subscribed) {
		if (subscribed && unsubscribedPlayers.contains(uniqueId.toString())) unsubscribedPlayers.remove(uniqueId.toString());
		if (!subscribed && !unsubscribedPlayers.contains(uniqueId.toString())) unsubscribedPlayers.add(uniqueId.toString());
	}
	public static void broadcastMessage(String message) {
		for (Player player : Bukkit.getOnlinePlayers())
			if (getSubscribed(player.getUniqueId()))
				player.sendMessage(message);
	}
	public static void loadTotals() {
		if (!new File("totals.json").exists()) return;
		Type type = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, Integer> totals = new HashMap<String, Integer>();
		try {
			totals = new Gson().fromJson(FileUtils.readFileToString(new File("totals.json")), type);
		} catch (JsonSyntaxException | IOException e) {
			e.printStackTrace();
		}
		
		DebugCancelledMinecraftChatEventsCountTotal = totals.get("DebugCancelledMinecraftChatEventsCountTotal");
		DebugMinecraftChatEventsCountTotal = totals.get("DebugMinecraftChatEventsCountTotal");
		DebugDiscordChatEventsCountTotal = totals.get("DebugDiscordChatEventsCountTotal");
		DebugDiscordChatChannelEventsCountTotal = totals.get("DebugDiscordChatChannelEventsCountTotal");
		DebugDiscordConsoleChannelEventsCountTotal = totals.get("DebugDiscordConsoleChannelEventsCountTotal");
		DebugMinecraftCommandsCountTotal = totals.get("DebugMinecraftCommandsCountTotal");
		DebugConsoleLogLinesProcessedTotal = totals.get("DebugConsoleLogLinesProcessedTotal");
		DebugConsoleMessagesSentTotal = totals.get("DebugConsoleMessagesSentTotal");
		DebugConsoleMessagesNullTotal = totals.get("DebugConsoleMessagesNullTotal");
		DebugConsoleMessagesNotNullTotal = totals.get("DebugConsoleMessagesNotNullTotal");
	}
	public static void saveTotals() {
		Map<String, Integer> totals = new HashMap<String, Integer>();
		totals.put("DebugCancelledMinecraftChatEventsCountTotal", DebugCancelledMinecraftChatEventsCountTotal);
		totals.put("DebugMinecraftChatEventsCountTotal", DebugMinecraftChatEventsCountTotal);
		totals.put("DebugDiscordChatEventsCountTotal", DebugDiscordChatEventsCountTotal);
		totals.put("DebugDiscordChatChannelEventsCountTotal", DebugDiscordChatChannelEventsCountTotal);
		totals.put("DebugDiscordConsoleChannelEventsCountTotal", DebugDiscordConsoleChannelEventsCountTotal);
		totals.put("DebugMinecraftCommandsCountTotal", DebugMinecraftCommandsCountTotal);
		totals.put("DebugConsoleLogLinesProcessedTotal", DebugConsoleLogLinesProcessedTotal);
		totals.put("DebugConsoleMessagesSentTotal", DebugConsoleMessagesSentTotal);
		totals.put("DebugConsoleMessagesNullTotal", DebugConsoleMessagesNullTotal);
		totals.put("DebugConsoleMessagesNotNullTotal", DebugConsoleMessagesNotNullTotal);
		try {
			FileUtils.writeStringToFile(new File("totals.json"), new Gson().toJson(totals));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String convertRoleToMinecraftColor(Role role) {
		if (role == null) return "";
		String before = Integer.toHexString(role.getColor());
		
		if (before.equalsIgnoreCase("99AAB5")) return "&f";
		if (before.equalsIgnoreCase("1ABC9C")) return "&a";
		if (before.equalsIgnoreCase("2ECC71")) return "&a";
		if (before.equalsIgnoreCase("3498DB")) return "&9";
		if (before.equalsIgnoreCase("9B59B6")) return "&5";
		if (before.equalsIgnoreCase("E91E63")) return "&d";
		if (before.equalsIgnoreCase("F1C40F")) return "&e";
		if (before.equalsIgnoreCase("E67E22")) return "&6";
		if (before.equalsIgnoreCase("E74C3C")) return "&c";
		if (before.equalsIgnoreCase("95A5A6")) return "&7";
		if (before.equalsIgnoreCase("607D8B")) return "&8";
		if (before.equalsIgnoreCase("11806A")) return "&2";
		if (before.equalsIgnoreCase("1F8B4C")) return "&2";
		if (before.equalsIgnoreCase("206694")) return "&1";
		if (before.equalsIgnoreCase("71368A")) return "&5";
		if (before.equalsIgnoreCase("AD1457")) return "&d";
		if (before.equalsIgnoreCase("C27C0E")) return "&6";
		if (before.equalsIgnoreCase("A84300")) return "&6";
		if (before.equalsIgnoreCase("992D22")) return "&4";
		if (before.equalsIgnoreCase("979C9F")) return "&7";
		if (before.equalsIgnoreCase("546E7A")) return "&8";
		
		return null;
	}
}