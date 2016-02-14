package com.scarsz.discordsrv;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.exceptions.RateLimitedException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

public class DiscordSRV extends JavaPlugin{
	public static Boolean ready = false;
	public static JDA api;
	public static ServerLogWatcher serverLogWatcher;
	
	public static Plugin plugin;
	
	public void onEnable() {
		plugin = this;
		if (!new File(getDataFolder(), "config.yml").exists()) saveResource("config.yml", false);
		if (getConfig().getDouble("ConfigVersion") < Double.parseDouble(getDescription().getVersion()) || !getConfig().isSet("ConfigVersion"))
			try {
				Files.move(new File(getDataFolder(), "config.yml"), new File(getDataFolder(), "config.yml-build." + getConfig().getInt("ConfigVersion") + ".old"));
				getLogger().info("Your DiscordSRV config file was outdated; a new one has been made for the new build.");
			} catch (IOException ex) {}
		if (!new File(getDataFolder(), "config.yml").exists()) saveResource("config.yml", false);
		
		// update check
		if (Double.parseDouble(requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/master/latestbuild")) > Double.parseDouble(getDescription().getVersion())){
			getLogger().warning("The current build of DiscordSRV is outdated! Update at http://dev.bukkit.org/bukkit-plugins/discordsrv/");
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
		for (Guild server : api.getGuilds()){
			for (Channel channel : server.getTextChannels()){
				if (channel.getName().equals(chatChannel))
					getLogger().info("Chat: " + server.getName() + " - " + channel.getName() + " [" + channel.getId() + "]");
				else if (channel.getName().equals(consoleChannel))
					getLogger().info("Console: " + server.getName() + " - " + channel.getName() + " [" + channel.getId() + "]");
			}
		}
		
		// in-game chat events
		getServer().getPluginManager().registerEvents(new ChatListener(api, this), this);
		getServer().getPluginManager().registerEvents(new PlayerDeathListener(api, this), this);
		// console streaming thread
		if (getConfig().getBoolean("DiscordConsoleChannelEnabled")){
			serverLogWatcher = new ServerLogWatcher(api, this);
			serverLogWatcher.start();
		}
		
		// player join/leave message events
		if (getConfig().getBoolean("MinecraftPlayerJoinMessageEnabled"))
			getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(api, this), this);
		
		// enable metrics
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			getLogger().warning("Unable to start metrics. Oh well.");
		}
		
		// test channels
		// - chat channel
		TextChannel chatTextChannel = getChannel(chatChannel);
		TextChannel consoleTextChannel = getChannel(consoleChannel);
		if (!testChannel(chatTextChannel)) getLogger().warning("Channel \"" + chatChannel + "\" was not accessible");
		if (testChannel(chatTextChannel) && !chatTextChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + chatTextChannel.getName());
		if (testChannel(chatTextChannel) && !chatTextChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + chatTextChannel.getName());
		// - console channel
		if (!testChannel(consoleTextChannel)) getLogger().warning("Channel \"" + consoleChannel + "\" was not accessible");
		if (testChannel(consoleTextChannel) && !consoleTextChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + consoleTextChannel.getName());
		if (testChannel(consoleTextChannel) && !consoleTextChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + consoleTextChannel.getName());
	}
	public void onDisable() {
		if (serverLogWatcher != null && !serverLogWatcher.isInterrupted())
			serverLogWatcher.interrupt();
		serverLogWatcher = null;
		try { api.shutdown(false); } catch (Exception e) { getLogger().info("Discord shutting down before logged in"); }
		api = null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.isOp()) return true;
		if (args.length == 0) return false;
		if (args[0].equalsIgnoreCase("reload")) {
			reloadConfig();
			sender.sendMessage("DiscordSRV config has been reloaded");
			return true;
		}
		return false; 
	}
	
	public static String requestHttp(String requestUrl){
		String sourceLine = null;
<<<<<<< HEAD

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
        
=======
		
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
	        
>>>>>>> origin/master
		return sourceLine;
	}
	public static Boolean testChannel(TextChannel channel){
		return channel != null;
	}
	public static TextChannel getChannel(String name){
		if (api == null) return null;
		for (Guild g : api.getGuilds())
			for (TextChannel c : g.getTextChannels())
				if (c.getName().equals(name)) return c;
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
}
