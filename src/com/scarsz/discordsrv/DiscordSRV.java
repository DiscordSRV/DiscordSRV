package com.scarsz.discordsrv;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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

public class DiscordSRV extends JavaPlugin{
	public static JDA api;
	public static Plugin plugin;
	public static Boolean ready = false;
	public static ServerLogWatcher serverLogWatcher;
	public static List<String> unsubscribedPlayers = new ArrayList<>();
	
	public void onEnable(){
		// set static plugin variable for discordsrv methods to use
		plugin = this;
		
		// load config, create if doesn't exist, update config if old
		if (!new File(getDataFolder(), "config.yml").exists()) saveResource("config.yml", false);
		if (getConfig().getDouble("ConfigVersion") < Double.parseDouble(getDescription().getVersion()) || !getConfig().isSet("ConfigVersion"))
			try {
				Files.move(new File(getDataFolder(), "config.yml"), new File(getDataFolder(), "config.yml-build." + getConfig().getDouble("ConfigVersion") + ".old"));
				getLogger().info("Your DiscordSRV config file was outdated; a new one has been made for the new build.");
			} catch (IOException ex) {}
		if (!new File(getDataFolder(), "config.yml").exists()) saveResource("config.yml", false);
		
		// update check
		if (!plugin.getConfig().getBoolean("UpdateCheckDisabled")){
			double latest = Double.parseDouble(requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/master/latestbuild"));
			if (latest > Double.parseDouble(getDescription().getVersion()))
				getLogger().warning(System.lineSeparator() + System.lineSeparator() + "The current build of DiscordSRV is outdated! Get build " + latest + " at http://scarsz.com/discordsrv/" + System.lineSeparator() + System.lineSeparator());
		}
		
		// login to discord
		try {
			api = new JDABuilder(getConfig().getString("DiscordEmail"), getConfig().getString("DiscordPassword"))
			.addListener(new DiscordListener(getServer(), this)).enableVoice(false).buildBlocking();
		} catch (IllegalArgumentException | LoginException | InterruptedException e) {
			getLogger().severe(System.lineSeparator() + System.lineSeparator() + "Error enabling DiscordSRV: " + e.getMessage() + System.lineSeparator() + System.lineSeparator());
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		// print the servers that the bot will listen on
		String chatChannel = getConfig().getString("DiscordChatChannelName");
		String consoleChannel = getConfig().getString("DiscordConsoleChannelName");
		getLogger().info("DiscordSRV will listen to these channels:");
		for (Guild server : api.getGuilds()) {
			for (Channel channel : server.getTextChannels()){
				if (channel.getName().equals(chatChannel) && (getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft") || getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")))
					getLogger().info("Chat: " + server.getName() + " - " + channel.getName() + " [" + channel.getId() + "]");
				else if (channel.getName().equals(consoleChannel) && getConfig().getBoolean("DiscordConsoleChannelEnabled"))
					getLogger().info("Console: " + server.getName() + " - " + channel.getName() + " [" + channel.getId() + "]");
			}
		}
		
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
		
		// enable metrics
		if (!getConfig().getBoolean("MetricsDisabled"))
			try {
				Metrics metrics = new Metrics(this);
				metrics.start();
			} catch (IOException e) {
				getLogger().warning("Unable to start metrics. Oh well.");
			}
		
		// test channels
		TextChannel chatTextChannel = getChannel(chatChannel);
		TextChannel consoleTextChannel = getChannel(consoleChannel);
		// - chat channel
		if (getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft") || getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")){
			if (!testChannel(chatTextChannel)) getLogger().warning("Channel \"" + chatChannel + "\" was not accessible");
			if (testChannel(chatTextChannel) && !chatTextChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + chatTextChannel.getName());
			if (testChannel(chatTextChannel) && !chatTextChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + chatTextChannel.getName());
		}
		// - console channel
		if (getConfig().getBoolean("DiscordConsoleChannelEnabled")){
			if (!testChannel(consoleTextChannel)) getLogger().warning("Channel \"" + consoleChannel + "\" was not accessible");
			if (testChannel(consoleTextChannel) && !consoleTextChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + consoleTextChannel.getName());
			if (testChannel(consoleTextChannel) && !consoleTextChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + consoleTextChannel.getName());	
		}
		
		// game status
		if (!getConfig().getString("DiscordGameStatus").isEmpty())
			api.getAccountManager().setGame(getConfig().getString("DiscordGameStatus"));
		
		// load unsubscribed users
		if (new File(getDataFolder(), "unsubscribed.txt").exists())
			try {
				for (String id : FileUtils.readFileToString(new File(getDataFolder(), "unsubscribed.txt")).split("\n"))
					unsubscribedPlayers.add(id);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	public void onDisable(){
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
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (args[0].equalsIgnoreCase("reload")){
			if (!sender.isOp()) return true;
			reloadConfig();
			sender.sendMessage("DiscordSRV config has been reloaded. Some config options require a restart.");
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
	
	public static String requestHttp(String requestUrl){
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
    public static Boolean testChannel(TextChannel channel){
    	return channel != null;
    }
	public static TextChannel getChannel(String name)
    {
    	if (api == null) return null;
        for (Guild g : api.getGuilds())
            for (TextChannel c : g.getTextChannels())
                if (c.getName().equals(name))
                	return c;
        return null;
    }
	public static void sendMessage(TextChannel channel, String message){
		if (api == null || channel == null || (!channel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ) || !channel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE))) return;
		message = ChatColor.stripColor(message).replace("[m", "").replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "").replaceAll("\\[[0-9]{1,3}m", "").replace("[m", "").replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "").replaceAll("\\[[0-9]{1,3}m", "");
		Boolean sent = false;
		Integer tries = 0;
		while (!sent && tries < 3){
			try {
				channel.sendMessage(message);
				sent = true;
			} catch (RateLimitedException e) {
				tries++;
				verboseWait(e.getTimeout());
			}
		}
	}
	public static void verboseWait(long time){
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
	public static String getPrimaryGroup(Player player){
		if (!Bukkit.getPluginManager().isPluginEnabled("Vault")){
			plugin.getLogger().warning("Attempted to get user " + player + "'s primary group without Vault enabled");
			return "";
		}
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
	public static String getTopRole(MessageReceivedEvent event) {
		Role highestRole = null;
		for (Role role : event.getGuild().getRolesForUser(event.getAuthor())){
			if (highestRole == null) highestRole = role;
			else if (highestRole.getPosition() < role.getPosition()) highestRole = role;
		}
		return highestRole != null ? highestRole.getName() : "";
	}
	public static List<Player> getOnlinePlayers() {
		List<Player> players = new ArrayList<Player>(Bukkit.getOnlinePlayers());
		List<Player> playersToRemove = new ArrayList<Player>();
		for (Player player : players){
			if (Bukkit.getPluginManager().isPluginEnabled("VanishNoPacket") && VanishedPlayerCheck.checkPlayerIsVanished(player.getName()))
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
	public static void broadcastMessage(String message){
		for (Player player : Bukkit.getOnlinePlayers())
			if (getSubscribed(player.getUniqueId()))
				player.sendMessage(message);
	}
}