package com.scarsz.discordsrv;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

import javax.security.auth.login.LoginException;

import com.google.gson.JsonIOException;
import com.scarsz.discordsrv.listeners.*;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.exceptions.RateLimitedException;

import net.dv8tion.jda.utils.AvatarUtil;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONException;

import com.google.common.io.Files;
import com.scarsz.discordsrv.threads.ChannelTopicUpdater;
import com.scarsz.discordsrv.threads.ServerLogWatcher;
import com.scarsz.discordsrv.threads.ServerLogWatcherHelper;

public class DiscordSRV extends JavaPlugin {
    public static JDA api;
    public static Plugin plugin;
    public static Long startTime = System.nanoTime();
    public static Boolean updateIsAvailable = false;
    public static ServerLogWatcher serverLogWatcher;
    public static ServerLogWatcherHelper serverLogWatcherHelper;
    public static ChannelTopicUpdater channelTopicUpdater;
    public static List<String> unsubscribedPlayers = new ArrayList<>();

    public static Guild guild;
    public static TextChannel chatChannel;
    public static TextChannel consoleChannel;

    public void onEnable() {
        // set static plugin variable for discordsrv methods to use
        plugin = this;

        // load config, create if doesn't exist, update config if old
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", false);
            reloadConfig();
        }
        if (getConfig().getDouble("ConfigVersion") < Double.parseDouble(getDescription().getVersion()) || !getConfig().isSet("ConfigVersion"))
            try {
                //Files.move(new File(getDataFolder(), "config.yml"), new File(getDataFolder(), "config.yml-build." + getConfig().getDouble("ConfigVersion") + ".old"));
                getLogger().info("Your DiscordSRV config file was outdated; attempting migration...");

                File config = new File(getDataFolder(), "config.yml");
                File oldConfig = new File(getDataFolder(), "config.yml-build." + getConfig().getDouble("ConfigVersion") + ".old");
                Files.move(config, oldConfig);
                if (!new File(getDataFolder(), "config.yml").exists()) saveResource("config.yml", false);

                Scanner s1 = new Scanner(oldConfig);
                ArrayList<String> oldConfigLines = new ArrayList<>();
                while (s1.hasNextLine()) oldConfigLines.add(s1.nextLine());
                s1.close();

                Scanner s2 = new Scanner(config);
                ArrayList<String> newConfigLines = new ArrayList<>();
                while (s2.hasNextLine()) newConfigLines.add(s2.nextLine());
                s2.close();

                Map<String, String> oldConfigMap = new HashMap<>();
                for (String line : oldConfigLines) {
                    if (line.startsWith("#") || line.startsWith("-") || line.isEmpty()) continue;
                    List<String> lineSplit = new ArrayList<>();
                    Collections.addAll(lineSplit, line.split(": +|:"));
                    if (lineSplit.size() < 2) continue;
                    String key = lineSplit.get(0);
                    lineSplit.remove(0);
                    String value = String.join(": ", lineSplit);
                    oldConfigMap.put(key, value);
                }

                Map<String, String> newConfigMap = new HashMap<>();
                for (String line : newConfigLines) {
                    if (line.startsWith("#") || line.startsWith("-") || line.isEmpty()) continue;
                    List<String> lineSplit = new ArrayList<>();
                    for (String segment : line.split(": +|:")) lineSplit.add(segment);
                    if (lineSplit.size() >= 2) newConfigMap.put(lineSplit.get(0), lineSplit.get(1));
                }

                for (String key : oldConfigMap.keySet()) {
                    if (newConfigMap.containsKey(key) && !key.startsWith("ConfigVersion")) {
                        getLogger().info("Migrating config option " + key + " with value " + oldConfigMap.get(key) + " to new config");
                        newConfigMap.put(key, oldConfigMap.get(key));
                    }
                }

                for (String line : newConfigLines) {
                    if (line.startsWith("#") || line.startsWith("ConfigVersion")) continue;
                    String key = line.split(":")[0];
                    if (oldConfigMap.containsKey(key))
                        newConfigLines.set(newConfigLines.indexOf(line), key + ": " + oldConfigMap.get(key));
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(config));
                for (String line : newConfigLines) writer.write(line + System.lineSeparator());
                writer.flush();
                writer.close();

                getLogger().info("Migration complete. Note: migration does not apply to config options that are lists.");
                reloadConfig();
            } catch (IOException ex) { }
        if (!new File(getDataFolder(), "config.yml").exists()) saveResource("config.yml", false);
        reloadConfig();

        // update check
        if (!plugin.getConfig().getBoolean("UpdateCheckDisabled")) {
            double latest = Double.parseDouble(requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/master/latestbuild"));
            if (latest > Double.parseDouble(getDescription().getVersion())) {
                getLogger().warning(System.lineSeparator() + System.lineSeparator() + "The current build of DiscordSRV is outdated! Get build " + latest + " at http://dev.bukkit.org/bukkit-plugins/discordsrv/" + System.lineSeparator() + System.lineSeparator());
                updateIsAvailable = true;
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
            for (Channel channel : server.getTextChannels()) {
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
            sendMessage(chatChannel, DiscordSRV.plugin.getConfig().getString("DiscordChatChannelServerStartupMessage"));

        // in-game chat events
        //check if legendchat is on the server and is enabled in the config
        if (DiscordSRV.plugin.getServer().getPluginManager().isPluginEnabled("LegendChat") && DiscordSRV.plugin.getConfig().getBoolean("legendchat")) getServer().getPluginManager().registerEvents((new LegendChatListener(api)), this);
        //if it's not, use default chat listener
        else getServer().getPluginManager().registerEvents(new ChatListener(api), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(api), this);

        // console streaming thread & helper
        startServerLogWatcher();
        serverLogWatcherHelper = new ServerLogWatcherHelper();
        serverLogWatcherHelper.start();

        // channel topic updating thread
        if (channelTopicUpdater == null) {
            channelTopicUpdater = new ChannelTopicUpdater(api);
            channelTopicUpdater.start();
        }

        // player join/leave message events
        if (getConfig().getBoolean("MinecraftPlayerJoinMessageEnabled"))
            getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(api), this);

        // player achievement events
        if (getConfig().getBoolean("MinecraftPlayerAchievementMessagesEnabled"))
            getServer().getPluginManager().registerEvents(new AchievementListener(api), this);

        // enable metrics
        if (!getConfig().getBoolean("MetricsDisabled"))
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (IOException e) {
                getLogger().warning("Unable to start metrics. Oh well.");
            }

        // - chat channel
        if (getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft") || getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) {
            if (!testChannel(chatChannel)) getLogger().warning("Channel \"" + chatChannel + "\" was not accessible");
            if (testChannel(chatChannel) && !chatChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + chatChannel.getName());
            if (testChannel(chatChannel) && !chatChannel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + chatChannel.getName());
        }
        // - console channel
        if (consoleChannel != null) {
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

        // start TPS poller
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Lag(), 100L, 1L);
    }
    public void onDisable() {
        // kill server log watcher & helper
        if (serverLogWatcher != null && !serverLogWatcher.isInterrupted())
            serverLogWatcher.interrupt();
        serverLogWatcher = null;
        if (serverLogWatcherHelper != null && !serverLogWatcherHelper.isInterrupted())
            serverLogWatcherHelper.interrupt();
        serverLogWatcherHelper = null;

        // server shutdown message
        if (chatChannel != null && getConfig().getBoolean("DiscordChatChannelServerShutdownMessageEnabled")) chatChannel.sendMessage(getConfig().getString("DiscordChatChannelServerShutdownMessage"));

        // disconnect from discord
        try { api.shutdown(false); } catch (Exception e) { getLogger().info("Discord shutting down before logged in"); }
        api = null;

        // save unsubscribed users
        if (new File(getDataFolder(), "unsubscribed.txt").exists()) new File(getDataFolder(), "unsubscribed.txt").delete();
        String players = "";
        for (String id : unsubscribedPlayers) players += id + "\n";
        if (players.length() > 0) {
            players = players.substring(0, players.length() - 1);
            try {
                FileUtils.writeStringToFile(new File(getDataFolder(), "unsubscribed.txt"), players);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!sender.isOp()) sender.sendMessage("/discordsrv toggle/subscribe/unsubscribe");
            else sender.sendMessage("/discordsrv setpicture/reload/rebuild/debug/toggle/subscribe/unsubscribe");
            return true;
        }
        if (args[0].equalsIgnoreCase("setpicture")) {
            if (!sender.isOp()) {
                sender.sendMessage("Must be OP to use this command");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("Must give URL to picture to set as bot picture");
                return true;
            }
            try {
                sender.sendMessage("Downloading picture...");
                ReadableByteChannel in = Channels.newChannel(new URL(args[1]).openStream());
                FileChannel out = new FileOutputStream(getDataFolder().getAbsolutePath() + "/picture.jpg").getChannel();
                out.transferFrom(in, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                sender.sendMessage("Download failed: " + e.getMessage());
                return true;
            }
            try {
                api.getAccountManager().setAvatar(AvatarUtil.getAvatar(new File(getDataFolder().getAbsolutePath() + "/picture.jpg"))).update();
                sender.sendMessage("Picture updated successfully");
            } catch (UnsupportedEncodingException e) {
                sender.sendMessage("Error setting picture as avatar: " + e.getMessage());
            }
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

            List<String> discordsrvMessages = new ArrayList<>();
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
                if (line != null && line.toLowerCase().contains("discordsrv")) discordsrvMessages.add(line);
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
        if (args[0].equalsIgnoreCase("toggle")) {
            Boolean subscribed = getSubscribed(senderPlayer.getUniqueId());
            setSubscribed(senderPlayer.getUniqueId(), !subscribed);

            String subscribedMessage = getSubscribed(senderPlayer.getUniqueId()) ? "subscribed" : "unsubscribed";
            sender.sendMessage(ChatColor.AQUA + "You have been " + subscribedMessage + " to Discord messages.");
        }
        if (args[0].equalsIgnoreCase("subscribe")) {
            setSubscribed(senderPlayer.getUniqueId(), true);
            sender.sendMessage(ChatColor.AQUA + "You have been subscribed to Discord messages.");
        }
        if (args[0].equalsIgnoreCase("unsubscribe")) {
            setSubscribed(senderPlayer.getUniqueId(), false);
            sender.sendMessage(ChatColor.AQUA + "You are no longer subscribed to Discord messages.");
        }
        return true;
    }

    public static void startServerLogWatcher() {
        // kill server log watcher if it's already started
        if (serverLogWatcher != null && !serverLogWatcher.isInterrupted())
            serverLogWatcher.interrupt();
        serverLogWatcher = null;

        if (consoleChannel != null) {
            serverLogWatcher = new ServerLogWatcher(api);
            serverLogWatcher.start();
        }
    }
    private void buildJda() {
        // shutdown if already started
        if (api != null) try { api.shutdown(false); } catch (Exception e) { e.printStackTrace(); }

        try {
            api = new JDABuilder()
                    .setBotToken(getConfig().getString("BotToken"))
                    .addListener(new DiscordListener(getServer()))
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
    private static String requestHttp(String requestUrl) {
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
    private static Boolean testChannel(TextChannel channel) {
        return channel != null;
    }
    public static void sendMessage(TextChannel channel, String message) {
        // send messages on their own threads, removes chat lag from processing
        new Thread(() -> {
            String newMessage = message;

            if (api == null || channel == null || (!channel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_READ) || !channel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE))) return;
            newMessage = ChatColor.stripColor(newMessage).replace("[m", "").replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "").replaceAll("\\[[0-9]{1,3}m", "").replace("[m", "").replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "").replaceAll("\\[[0-9]{1,3}m", "");

            for (Object phrase : DiscordSRV.plugin.getConfig().getList("DiscordChatChannelCutPhrases")) {
                if (newMessage.contains((String) phrase)) {
                    newMessage = newMessage.replace((String) phrase, "");
                }
            }

            Boolean sent = false;
            Integer tries = 0;
            if (newMessage.length() > 2000) {
                plugin.getLogger().warning("Tried sending message with length of " + newMessage.length() + " (" + (newMessage.length() - 2000) + " over limit)");
                newMessage = newMessage.substring(0, 1999);
            }
            while (!sent && tries < 3) {
                try {
                    channel.sendMessage(newMessage);
                    sent = true;
                } catch (RateLimitedException e) {
                    tries++;
                    verboseWait(e.getTimeout());
                } catch (JSONException e) {
                    plugin.getLogger().info("CloudFlare page returned from JSON parse, message sending failed");
                }
            }
        }).start();
    }
    public static void sendMessageToChatChannel(String message) {
        sendMessage(chatChannel, message);
    }
    public static void sendMessageToConsoleChannel(String message) {
        sendMessage(consoleChannel, message);
    }
    private static void verboseWait(long time) {
        if (plugin.getConfig().getBoolean("RateLimitSleepVerbose")) {
            long intervals = time / 4;
            while (time > intervals) {
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

        RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> service = DiscordSRV.plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);

        if (service == null) return "";
        try {
            String primaryGroup = service.getProvider().getPrimaryGroup(player);
            if (!primaryGroup.equals("default")) return primaryGroup;
        } catch (Exception e) { }
        return " ";
    }
    public static String getAllRoles(MessageReceivedEvent event) {
        String roles = "";
        for (Role role : event.getGuild().getRolesForUser(event.getAuthor())) {
            roles += role.getName() + DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftAllRolesSeperator");
        }
        if (!roles.isEmpty()) roles = roles.substring(0, roles.length() - DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftAllRolesSeperator").length());
        return roles;
    }
    public static Role getTopRole(MessageReceivedEvent event) {
        Role highestRole = null;
        for (Role role : event.getGuild().getRolesForUser(event.getAuthor())) {
            if (highestRole == null) highestRole = role;
            else if (highestRole.getPosition() < role.getPosition()) highestRole = role;
        }
        return highestRole;
    }
    public static String getRoleName(Role role) {
        return role == null ? "" : role.getName();
    }
    public static List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Player> playersToRemove = new ArrayList<>();
        for (Player player : players) {
            if (VanishedPlayerCheck.checkPlayerIsVanished(player.getName()))
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
    public static void broadcastMessageToMinecraftServer(String message) {
        for (Player player : Bukkit.getOnlinePlayers())
            if (getSubscribed(player.getUniqueId()))
                player.sendMessage(message);
    }
    public static String convertRoleToMinecraftColor(Role role) {
        if (role == null) return "";
        String before = Integer.toHexString(role.getColor());

        if (before.equalsIgnoreCase("99AAB5")) return "&f";
        if (before.equalsIgnoreCase("1ABC9C")) return "&a";
        if (before.equalsIgnoreCase("2ECC71")) return "&a";
        if (before.equalsIgnoreCase("3498DB")) return "&3";
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

        return "";
    }
    public static String convertMentionsFromNames(String message) {
        if (!message.contains("@")) return message;
        List<String> splitMessage = Arrays.asList(message.split("@| "));
        for (User user : chatChannel.getUsers())
            for (String segment : splitMessage)
                if (user.getUsername().equals(segment))
                    splitMessage.set(splitMessage.indexOf(segment), user.getAsMention());

        String newMessage = String.join(" ", splitMessage);

        return newMessage;
    }
}