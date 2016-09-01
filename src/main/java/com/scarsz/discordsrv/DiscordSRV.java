package com.scarsz.discordsrv;

import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.scarsz.discordsrv.api.DiscordSRVListenerInterface;
import com.scarsz.discordsrv.api.events.ProcessChatEvent;
import com.scarsz.discordsrv.hooks.chat.HerochatHook;
import com.scarsz.discordsrv.hooks.chat.LegendChatHook;
import com.scarsz.discordsrv.hooks.chat.VentureChatHook;
import com.scarsz.discordsrv.hooks.worlds.MultiverseCoreHook;
import com.scarsz.discordsrv.listeners.*;
import com.scarsz.discordsrv.objects.*;
import com.scarsz.discordsrv.threads.ChannelTopicUpdater;
import com.scarsz.discordsrv.threads.ConsoleMessageQueueWorker;
import com.scarsz.discordsrv.threads.ServerLogWatcher;
import com.scarsz.discordsrv.util.DebugHandler;
import com.scarsz.discordsrv.util.VanishedPlayerCheck;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.AvatarUtil;
import net.dv8tion.jda.utils.SimpleLog;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.*;

@SuppressWarnings({"Convert2streamapi", "unused", "unchecked", "ResultOfMethodCallIgnored", "WeakerAccess", "ConstantConditions"})
public class DiscordSRV extends JavaPlugin {

    // snapshot id
    public static final String snapshotId = "CUSTOM";

    // channels
    public static final HashMap<String, TextChannel> channels = new HashMap<>();
    public static TextChannel chatChannel;
    public static TextChannel consoleChannel;

    // threads
    public static ChannelTopicUpdater channelTopicUpdater;
    public static ConsoleMessageQueueWorker consoleMessageQueueWorker;
    public static ServerLogWatcher serverLogWatcher;

    // plugin hooks
    public static boolean usingHerochat = false;
    public static boolean usingLegendChat = false;
    public static boolean usingVentureChat = false;

    // account linking
    public static AccountLinkManager accountLinkManager;
    public static HashMap<String, UUID> linkingCodes = new HashMap<>();

    // misc variables
    public static Gson gson = new Gson();
    private static boolean canUsePing = false;
    private static CancellationDetector<AsyncPlayerChatEvent> detector = new CancellationDetector<>(AsyncPlayerChatEvent.class);
    public static JDA jda = null;
    public static Plugin plugin;
    public static final long startTime = System.nanoTime();
    public static final HashMap<String, String> colors = new HashMap<>();
    public static boolean updateIsAvailable = false;
    public static final List<String> unsubscribedPlayers = new ArrayList<>();
    public static final List<DiscordSRVListenerInterface> listeners = new ArrayList<>();
    public static final List<String> messageQueue = new ArrayList<>();
    public static final Map<String, String> responses = new HashedMap();

    public void onEnable() {
        // not sure if it's needed but clearing the listeners list onEnable might be a fix for the plugin not being reloadable
        if (jda != null) jda.getRegisteredListeners().forEach(o -> jda.removeEventListener(o));

        // set static plugin variable for discordsrv methods to use
        plugin = this;

        // load config, create if doesn't exist, update config if old
        if (!new File(getDataFolder(), "colors.json").exists()) saveResource("colors.json", false);
        if (!new File(getDataFolder(), "channels.json").exists()) saveResource("channels.json", false);
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
                    Collections.addAll(lineSplit, line.split(": +|:"));
                    if (lineSplit.size() >= 2) newConfigMap.put(lineSplit.get(0), lineSplit.get(1));
                }

                for (String key : oldConfigMap.keySet()) {
                    if (newConfigMap.containsKey(key) && !key.startsWith("ConfigVersion")) {
                        String oldKey = oldConfigMap.get(key);
                        if (key.toLowerCase().equals("bottoken")) oldKey = "OMITTED";
                        getLogger().info("Migrating config option " + key + " with value " + oldKey + " to new config");
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

                getLogger().info("Migration complete. Note: migration does not apply to config options that are multiple-line lists.");
                reloadConfig();
            } catch (IOException ignored) { }
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
        if (jda == null) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // print the things the bot can see
        for (Guild server : jda.getGuilds()) {
            getLogger().info("Found guild " + server);
            for (Channel channel : server.getTextChannels()) {
                getLogger().info("- " + channel);
            }
        }

        if (!new File(getDataFolder(), "channels.json").exists()) saveResource("channels.json", false);
        try {
            for (ChannelInfo<String, String> channel : (List<ChannelInfo<String, String>>) gson.fromJson(FileUtils.readFileToString(new File(getDataFolder(), "channels.json"), Charset.defaultCharset()), new TypeToken<List<ChannelInfo<String, String>>>(){}.getType())) {
                if (channel == null || channel.channelName() == null || channel.channelId() == null) {
                    // malformed channels.json
                    getLogger().warning("JSON parsing error for " + channel + " \"" + channel.channelName() + "\" \"" + channel.channelId() + "\"");
                    continue;
                }

                TextChannel requestedChannel = jda.getTextChannelById(channel.channelId());
                if (requestedChannel == null) continue;
                channels.put(channel.channelName(), requestedChannel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // check & get location info
        chatChannel = getTextChannelFromChannelName(getConfig().getString("DiscordMainChatChannel"));
        consoleChannel = jda.getTextChannelById(getConfig().getString("DiscordConsoleChannelId"));

        if (chatChannel == null) getLogger().warning("Specified chat channel from channels.json could not be found (is it's name set to \"" + getConfig().getString("DiscordMainChatChannel") + "\"?)");
        if (consoleChannel == null) getLogger().warning("Specified console channel from config could not be found");
        if (chatChannel == null && consoleChannel == null) {
            getLogger().severe("Chat and console channels are both unavailable, disabling");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // send startup message if enabled
        if (plugin.getConfig().getBoolean("DiscordChatChannelServerStartupMessageEnabled"))
            sendMessage(chatChannel, DiscordSRV.plugin.getConfig().getString("DiscordChatChannelServerStartupMessage"));

        // in-game chat events
        if (checkIfPluginEnabled("herochat") && getConfig().getBoolean("HeroChatHook")) {
            getLogger().info("Enabling Herochat hook");
            getServer().getPluginManager().registerEvents(new HerochatHook(), this);
        } else if (checkIfPluginEnabled("legendchat") && getConfig().getBoolean("LegendChatHook")) {
            getLogger().info("Enabling LegendChat hook");
            getServer().getPluginManager().registerEvents(new LegendChatHook(), this);
        } else if (checkIfPluginEnabled("venturechat") && getConfig().getBoolean("VentureChatHook")) {
            getLogger().info("Enabling VentureChatHook hook");
            getServer().getPluginManager().registerEvents(new VentureChatHook(), this);
        } else {
            getLogger().info("No chat plugin hooks enabled");
            getServer().getPluginManager().registerEvents(new ChatListener(), this);
        }

        // in-game death events
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        // channel topic updating thread
        if (channelTopicUpdater == null) {
            channelTopicUpdater = new ChannelTopicUpdater();
            channelTopicUpdater.start();
        }

        // console channel streaming
        if (consoleChannel != null) {
            if (!getConfig().getBoolean("LegacyConsoleChannelEngine")) {
                // attach appender to queue console messages
                Logger rootLogger = (Logger) LogManager.getRootLogger();
                rootLogger.addAppender(new ConsoleAppender());

                // start console message queue worker thread
                if (consoleMessageQueueWorker == null) {
                    consoleMessageQueueWorker = new ConsoleMessageQueueWorker();
                    consoleMessageQueueWorker.start();
                }
            } else {
                // kill server log watcher if it's already started
                if (serverLogWatcher != null && !serverLogWatcher.isInterrupted()) serverLogWatcher.interrupt();
                serverLogWatcher = null;
                serverLogWatcher = new ServerLogWatcher();
                serverLogWatcher.start();
            }
        }

        // player join/leave message events
        if (getConfig().getBoolean("MinecraftPlayerJoinMessageEnabled"))
            getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);

        // player achievement events
        if (getConfig().getBoolean("MinecraftPlayerAchievementMessagesEnabled"))
            getServer().getPluginManager().registerEvents(new AchievementListener(), this);

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
            if (!testChannel(chatChannel)) getLogger().warning("Channel " + chatChannel + " was not accessible");
            if (testChannel(chatChannel) && !chatChannel.checkPermission(jda.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + chatChannel);
            if (testChannel(chatChannel) && !chatChannel.checkPermission(jda.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + chatChannel);
        }
        // - console channel
        if (consoleChannel != null) {
            if (!testChannel(consoleChannel)) getLogger().warning("Channel " + consoleChannel + " was not accessible");
            if (testChannel(consoleChannel) && !consoleChannel.checkPermission(jda.getSelfInfo(), Permission.MESSAGE_WRITE)) getLogger().warning("The bot does not have access to send messages in " + consoleChannel);
            if (testChannel(consoleChannel) && !consoleChannel.checkPermission(jda.getSelfInfo(), Permission.MESSAGE_READ)) getLogger().warning("The bot does not have access to read messages in " + consoleChannel);
        }

        // load unsubscribed users
        if (new File(getDataFolder(), "unsubscribed.txt").exists())
            try {
                Collections.addAll(unsubscribedPlayers, Files.toString(new File(getDataFolder(), "unsubscribed.txt"), Charset.defaultCharset()).split("\n"));
            } catch (IOException e) {
                e.printStackTrace();
            }

        // load user-defined colors
        if (!new File(getDataFolder(), "colors.json").exists()) saveResource("colors.json", false);
        try {
            LinkedTreeMap<String, String> colorsJson = gson.fromJson(Files.toString(new File(getDataFolder(), "colors.json"), Charset.defaultCharset()), LinkedTreeMap.class);
            for (String key : colorsJson.keySet()) {
                String definition = colorsJson.get(key).toLowerCase();
                key = key.toLowerCase();
                colors.put(key, definition);
            }
            getLogger().info("Colors: " + colors);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // start TPS poller
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Lag(), 100L, 1L);

        // check if server can do pings
        double thisVersion = Double.valueOf(Bukkit.getBukkitVersion().split("\\.", 2)[1].split("-")[0]);
        canUsePing = thisVersion >= 9.0;
        if (!canUsePing) getLogger().warning("Server version is <1.9, mention sounds are disabled");

        // enable reporting plugins that have canceled chat events
        if (plugin.getConfig().getBoolean("ReportCanceledChatEvents")) {
            getLogger().info("Chat event detector has been enabled");
            detector.addListener((plugin, event) -> System.out.println(event.getClass().getName() + " cancelled by " + plugin));
        }

        // super listener for all discord events
        jda.addEventListener(new ListenerAdapter() {
            public void onEvent(Event event) {
                // don't notify of message receiving events, that's handled in the normal message listener
                if (event.getClass().getName().contains("MessageReceived")) return;

                notifyListeners(event);
            }
        });

        // account link manager
        accountLinkManager = new AccountLinkManager();

        // canned responses
        try {
            responses.clear();
            String key = "DiscordCannedResponses";

            FileReader fr = new FileReader(new File(DiscordSRV.plugin.getDataFolder(), "config.yml"));
            BufferedReader br = new BufferedReader(fr);
            boolean done = false;
            while (!done) {
                String line = br.readLine();
                if (line == null) done = true;
                if (line != null && line.startsWith(key)) {
                    ((Map<String, Object>) new Yaml().load(line.substring(key.length() + 1))).forEach((s, o) -> responses.put(s, String.valueOf(o)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void onDisable() {
        // close detector cause reasons
        detector.close();

        // kill channel topic updater
        if (channelTopicUpdater != null) {
            channelTopicUpdater.interrupt();
            channelTopicUpdater = null;
        }
        // kill message queue worker
        if (consoleMessageQueueWorker != null) {
            consoleMessageQueueWorker.interrupt();
            consoleMessageQueueWorker = null;
        }
        // kill server log watcher
        if (serverLogWatcher != null) {
            serverLogWatcher.interrupt();
            serverLogWatcher = null;
        }

        // server shutdown message
        if (chatChannel != null && getConfig().getBoolean("DiscordChatChannelServerShutdownMessageEnabled")) chatChannel.sendMessage(getConfig().getString("DiscordChatChannelServerShutdownMessage"));

        // disconnect from discord
        try { jda.shutdown(false); } catch (Exception e) { getLogger().info("Discord shutting down before logged in"); }
        jda = null;

        // save unsubscribed users
        if (new File(getDataFolder(), "unsubscribed.txt").exists()) new File(getDataFolder(), "unsubscribed.txt").delete();
        String players = "";
        for (String id : unsubscribedPlayers) players += id + "\n";
        if (players.length() > 0) {
            players = players.substring(0, players.length() - 1);
            try {
                Files.write(players, new File(getDataFolder(), "unsubscribed.txt"), Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("DiscordCommandFormat")));
            return true;
        }
        if (args[0].equals("?") || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("dowhatyouwantcauseapirateisfreeyouareapirateyarharfiddledeedee")) {
            if (!sender.isOp()) sender.sendMessage(ChatColor.AQUA + "/discord toggle/subscribe/unsubscribe/link/linked/clearlinked");
            else sender.sendMessage(ChatColor.AQUA + "/discord bcast/setpicture/reload/rebuild/debug/toggle/subscribe/unsubscribe/link/linked/clearlinked");
        }
        if (args[0].equalsIgnoreCase("bcast")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.AQUA + "Must be OP to use this command");
                return true;
            }
            List<String> messageStrings = Arrays.asList(args);
            String message = String.join(" ", messageStrings.subList(1, messageStrings.size()));
            sendMessage(chatChannel, message);
        }
        if (args[0].equalsIgnoreCase("setpicture")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.AQUA + "Must be OP to use this command");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.AQUA + "Must give URL to picture to set as bot picture");
                return true;
            }
            try {
                sender.sendMessage(ChatColor.AQUA + "Downloading picture...");
                ReadableByteChannel in = Channels.newChannel(new URL(args[1]).openStream());
                FileChannel out = new FileOutputStream(getDataFolder().getAbsolutePath() + "/picture.jpg").getChannel();
                out.transferFrom(in, 0, Long.MAX_VALUE);
                out.close();
            } catch (IOException e) {
                sender.sendMessage(ChatColor.AQUA + "Download failed: " + e.getMessage());
                return true;
            }
            try {
                jda.getAccountManager().setAvatar(AvatarUtil.getAvatar(new File(getDataFolder().getAbsolutePath() + "/picture.jpg"))).update();
                sender.sendMessage(ChatColor.AQUA + "Picture updated successfully");
            } catch (UnsupportedEncodingException e) {
                sender.sendMessage(ChatColor.AQUA + "Error setting picture as avatar: " + e.getMessage());
            }
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp()) return true;
            reloadConfig();
            sender.sendMessage(ChatColor.AQUA + "DiscordSRV config has been reloaded. Some config options require a restart.");
        }
        if (args[0].equalsIgnoreCase("debug")) {
            if (!sender.isOp()) return true;
            String hastebinUrl = DebugHandler.run();
            sender.sendMessage(ChatColor.AQUA + "Debug information has been uploaded to " + hastebinUrl + ". Please join the official DiscordSRV guild on the plugin page if you need help understanding this log- be sure to share it with us.");
        }
        if (args[0].equalsIgnoreCase("rebuild")) {
            if (!sender.isOp()) return true;
            //buildJda();
            sender.sendMessage(ChatColor.AQUA + "Disabled because no workie");
        }

        if (!(sender instanceof Player)) return true;
        Player senderPlayer = (Player) sender;

        // subscriptions
        if (args[0].equalsIgnoreCase("toggle")) setIsSubscribed(senderPlayer.getUniqueId(), !getIsSubscribed(senderPlayer.getUniqueId()));
        if (args[0].equalsIgnoreCase("subscribe")) setIsSubscribed(senderPlayer.getUniqueId(), true);
        if (args[0].equalsIgnoreCase("unsubscribe")) setIsSubscribed(senderPlayer.getUniqueId(), false);
        if (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("subscribe") || args[0].equalsIgnoreCase("unsubscribe"))
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getIsSubscribed(senderPlayer.getUniqueId())
                    ? getConfig().getString("MinecraftSubscriptionMessagesOnSubscribe")
                    : getConfig().getString("MinecraftSubscriptionMessagesOnUnsubscribe")
            ));

        // account linking
        if (args[0].equalsIgnoreCase("link")) {
            String code = "";
            Random random = new Random();
            for (int i = 0; i < 4; i++) code += random.nextInt(10);
            linkingCodes.put(code, senderPlayer.getUniqueId());
            sender.sendMessage(ChatColor.AQUA + "Your link code is " + code + ". Send a private message to the bot (" + jda.getSelfInfo().getUsername() + ") on Discord with just this code as the message to link your Discord account to your UUID.");
        }
        if (args[0].equalsIgnoreCase("linked")) {
            sender.sendMessage(ChatColor.AQUA + "Your UUID is linked to " + (accountLinkManager.getId(senderPlayer.getUniqueId()) != null ? jda.getUserById(accountLinkManager.getId(senderPlayer.getUniqueId())) != null ? jda.getUserById(accountLinkManager.getId(senderPlayer.getUniqueId())) : accountLinkManager.getId(senderPlayer.getUniqueId()) : "nobody."));
        }
        if (args[0].equalsIgnoreCase("clearlinked")) {
            sender.sendMessage(ChatColor.AQUA + "Your UUID is no longer associated with " + (accountLinkManager.getId(senderPlayer.getUniqueId()) != null ? jda.getUserById(accountLinkManager.getId(senderPlayer.getUniqueId())) != null ? jda.getUserById(accountLinkManager.getId(senderPlayer.getUniqueId())) : accountLinkManager.getId(senderPlayer.getUniqueId()) : "nobody. Never was."));
            accountLinkManager.unlink(senderPlayer.getUniqueId());
        }

        return true;
    }

    public static void processChatEvent(boolean isCancelled, Player sender, String message, String channel) {
        // notify listeners
        notifyListeners(new ProcessChatEvent(isCancelled, sender, message, channel));

        // ReportCanceledChatEvents debug message
        if (plugin.getConfig().getBoolean("ReportCanceledChatEvents")) plugin.getLogger().info("Chat message received, canceled: " + isCancelled);

        // return if player doesn't have permission
        if (!sender.hasPermission("discordsrv.chat") && !sender.isOp()) {
            if (plugin.getConfig().getBoolean("EventDebug")) plugin.getLogger().info("User " + sender.getName() + " sent a message but it was not delivered to Discord due to lack of permission");
            return;
        }

        // return if event canceled
        if (plugin.getConfig().getBoolean("DontSendCanceledChatEvents") && isCancelled) return;

        // return if should not send in-game chat
        if (!plugin.getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) return;

        // return if user is unsubscribed from Discord and config says don't send those peoples' messages
        if (!getIsSubscribed(sender.getUniqueId()) && !plugin.getConfig().getBoolean("MinecraftUnsubscribedMessageForwarding")) return;

        // return if doesn't match prefix filter
        if (!message.startsWith(plugin.getConfig().getString("DiscordChatChannelPrefix"))) return;
        
        String userPrimaryGroup = getPrimaryGroup(sender);
        boolean hasGoodGroup = !"".equals(userPrimaryGroup.replace(" ", ""));
        
        String format = hasGoodGroup ? plugin.getConfig().getString("MinecraftChatToDiscordMessageFormat") : plugin.getConfig().getString("MinecraftChatToDiscordMessageFormatNoPrimaryGroup");
        String discordMessage = format
                .replaceAll("&([0-9a-qs-z])", "")
                .replace("%message%", ChatColor.stripColor(message))
                .replace("%primarygroup%", getPrimaryGroup(sender))
                .replace("%displayname%", ChatColor.stripColor(sender.getDisplayName()))
                .replace("%username%", ChatColor.stripColor(sender.getName()))
                .replace("%world%", sender.getWorld().getName())
                .replace("%worldalias%", ChatColor.stripColor(MultiverseCoreHook.getWorldAlias(sender.getWorld().getName())))
                .replace("%time%", new Date().toString());

        discordMessage = convertMentionsFromNames(discordMessage);

        if (channel == null) sendMessage(chatChannel, discordMessage);
        else sendMessage(getTextChannelFromChannelName(channel), discordMessage);
    }

    private void buildJda() {
        // shutdown if already started
        if (jda != null) try { jda.shutdown(false); } catch (Exception e) { e.printStackTrace(); }

        SimpleLog.LEVEL = SimpleLog.Level.WARNING;
        SimpleLog.addListener(new SimpleLog.LogListener() {
            @Override
            public void onLog(SimpleLog simpleLog, SimpleLog.Level level, Object o) {
                if (level == SimpleLog.Level.INFO) getLogger().info("[JDA] " + o);
            }
            @Override
            public void onError(SimpleLog simpleLog, Throwable throwable) {}
        });

        try {
            jda = new JDABuilder()
                    .setBotToken(getConfig().getString("BotToken"))
                    .addListener(new DiscordListener())
                    .setAutoReconnect(true)
                    .setAudioEnabled(false)
                    .setBulkDeleteSplittingEnabled(false)
                    .buildBlocking();
        } catch (LoginException | IllegalArgumentException | InterruptedException e) {
            getLogger().severe(System.lineSeparator() + System.lineSeparator() + "Error building DiscordSRV: " + e.getMessage() + System.lineSeparator() + System.lineSeparator());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // game status
        if (!getConfig().getString("DiscordGameStatus").isEmpty())
            jda.getAccountManager().setGame(getConfig().getString("DiscordGameStatus"));
    }
    private static String requestHttp(String requestUrl) {
        try {
            URL address = new URL(requestUrl);
            InputStreamReader pageInput = new InputStreamReader(address.openStream());
            BufferedReader source = new BufferedReader(pageInput);
            return source.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
    private static boolean testChannel(TextChannel channel) {
        return channel != null;
    }
    public static void sendMessage(TextChannel channel, String message) {
        sendMessage(channel, message, true, 0);
    }
    public static void sendMessage(TextChannel channel, String message, boolean editMessage, int expiration) {
        if (jda == null || channel == null || (!channel.checkPermission(jda.getSelfInfo(), Permission.MESSAGE_READ) || !channel.checkPermission(jda.getSelfInfo(), Permission.MESSAGE_WRITE))) return;
        message = ChatColor.stripColor(message)
                .replaceAll("[&§][0-9a-fklmnor]", "") // removing &'s with addition of non-caught §'s if they get through somehow
                .replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "")
                .replaceAll("\\[[0-9]{1,3}m", "")
                .replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "")
                .replaceAll("\\[[0-9]{1,3}m", "")
                .replace("[m", "");

        if (editMessage)
            for (String phrase : DiscordSRV.plugin.getConfig().getStringList("DiscordChatChannelCutPhrases"))
                message = message.replace(phrase, "");

        String overflow = null;
        if (message.length() > 2000) {
            plugin.getLogger().warning("Tried sending message with length of " + message.length() + " (" + (message.length() - 2000) + " over limit)");
            overflow = message.substring(1999);
            message = message.substring(0, 1999);
        }


        channel.sendMessageAsync(message, m -> {
            if (expiration > 0) {
                try { Thread.sleep(expiration); } catch (InterruptedException e) { e.printStackTrace(); }
                if (channel.checkPermission(DiscordSRV.jda.getSelfInfo(), Permission.MESSAGE_MANAGE)) m.deleteMessage(); else DiscordSRV.plugin.getLogger().warning("Could not delete message in channel " + channel + ", no permission to manage messages");
            }
        });
        if (overflow != null) sendMessage(channel, overflow, editMessage, expiration);
    }
    public static void sendMessageToChatChannel(String message) {
        sendMessage(chatChannel, message);
    }
    public static void sendMessageToConsoleChannel(String message) {
        sendMessage(consoleChannel, message);
    }
    private static String getPrimaryGroup(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return " ";

        RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> service = DiscordSRV.plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);

        if (service == null) return " ";
        try {
            String primaryGroup = service.getProvider().getPrimaryGroup(player);
            if (!primaryGroup.equals("default")) return primaryGroup;
        } catch (Exception ignored) { }
        return " ";
    }
    public static String getAllRoles(GuildMessageReceivedEvent event) {
        String roles = "";
        for (Role role : event.getGuild().getRolesForUser(event.getAuthor())) {
            roles += role.getName() + DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftAllRolesSeperator");
        }
        if (!roles.isEmpty()) roles = roles.substring(0, roles.length() - DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftAllRolesSeperator").length());
        return roles;
    }
    public static Role getTopRole(GuildMessageReceivedEvent event) {
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
            if (VanishedPlayerCheck.checkPlayerIsVanished(player))
                playersToRemove.add(player);
        }
        players.removeAll(playersToRemove);
        return players;
    }
    private static boolean getIsSubscribed(UUID uniqueId) {
        return !unsubscribedPlayers.contains(uniqueId.toString());
    }
    private static void setIsSubscribed(UUID uniqueId, boolean subscribed) {
        if (subscribed && unsubscribedPlayers.contains(uniqueId.toString())) unsubscribedPlayers.remove(uniqueId.toString());
        if (!subscribed && !unsubscribedPlayers.contains(uniqueId.toString())) unsubscribedPlayers.add(uniqueId.toString());
    }
    public static void broadcastMessageToMinecraftServer(String message, String rawMessage, String channel) {
        boolean usingChatPlugin = usingLegendChat || usingHerochat || !channel.equalsIgnoreCase(plugin.getConfig().getString("DiscordMainChatChannel"));
        if (!usingChatPlugin) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (getIsSubscribed(player.getUniqueId())) {
                    player.sendMessage(message);
                    notifyPlayersOfMentions(Collections.singletonList(player), rawMessage);
                }
            }
        } else {
            if (usingHerochat) HerochatHook.broadcastMessageToChannel(channel, message, rawMessage);
            if (usingLegendChat) LegendChatHook.broadcastMessageToChannel(channel, message, rawMessage);
            if (usingVentureChat) VentureChatHook.broadcastMessageToChannel(channel, message, rawMessage);
        }
    }
    public static String convertRoleToMinecraftColor(Role role) {
        if (role == null) {
            if (DiscordSRV.plugin.getConfig().getBoolean("ColorLookupDebug")) DiscordSRV.plugin.getLogger().info("Role null, using no color");
            return "";
        }
        String colorHex = Integer.toHexString(role.getColor());
        String output = colors.get(colorHex);

        if (DiscordSRV.plugin.getConfig().getBoolean("ColorLookupDebug")) DiscordSRV.plugin.getLogger().info("Role " + role + " results to hex \"" + colorHex + "\" and output \"" + output + "\"");

        return output != null ? output : "";
    }
    private static String convertMentionsFromNames(String message) {
        if (!message.contains("@")) return message;
        List<String> splitMessage = new ArrayList<>(Arrays.asList(message.split("@| ")));
        for (User user : chatChannel.getUsers())
            for (String segment : splitMessage)
                if (user.getUsername().toLowerCase().equals(segment.toLowerCase()) || (chatChannel.getGuild().getNicknameForUser(user) != null && chatChannel.getGuild().getNicknameForUser(user).toLowerCase().equals(segment.toLowerCase()))) {
                    splitMessage.set(splitMessage.indexOf(segment), user.getAsMention());
                }
        splitMessage.removeAll(Arrays.asList("", null));
        return String.join(" ", splitMessage);
    }
    public static String getDestinationChannelName(TextChannel textChannel) {
        for (String channelName : channels.keySet()) {
            String registeredChannelId = getTextChannelFromChannelName(channelName).getId();
            String paramChannelId = textChannel.getId();
            if (registeredChannelId.equals(paramChannelId)) {
                return channelName;
            }
        }
        return null;
    }
    public static void notifyPlayersOfMentions(List<Player> possiblyPlayers, String parseMessage) {
        if (!canUsePing) return;

        List<String> splitMessage = new ArrayList<>();
        for (String phrase : parseMessage.replaceAll("[^a-zA-Z]", " ").split(" ")) splitMessage.add(phrase.toLowerCase());

        for (Player player : possiblyPlayers) {
            boolean playerOnline = player.isOnline();
            boolean phraseContainsName = splitMessage.contains(player.getName().toLowerCase());
            boolean phraseContainsDisplayName = splitMessage.contains(ChatColor.stripColor(player.getDisplayName()).toLowerCase());
            boolean shouldDing = phraseContainsName || phraseContainsDisplayName;
            if (playerOnline && shouldDing) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 1);
        }
    }
    public static TextChannel getTextChannelFromChannelName(String channelName) {
        if (channels.containsKey(channelName)) return channels.get(channelName);
        if (channels.containsKey(channelName.toLowerCase())) return channels.get(channelName.toLowerCase());
        return null;
    }
    public static boolean chatChannelIsLinked(String channelName) {
        return channels.containsKey(channelName) || channels.containsKey(channelName.toLowerCase());
    }
    public static String getDisplayName(Guild guild, User user) {
        String nickname = guild.getNicknameForUser(user);
        return nickname == null ? user.getUsername() : nickname;
    }
    public static boolean checkIfPluginEnabled(String pluginName) {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.getName().toLowerCase().startsWith(pluginName.toLowerCase())) return true;
        }
        return false;
    }
    public static void notifyListeners(Object event) {
        for (DiscordSRVListenerInterface listener : listeners) {
            if (listener == null) continue;

            if (event instanceof MessageReceivedEvent) listener.onDiscordMessageReceived((MessageReceivedEvent) event);
            else if (event instanceof ProcessChatEvent) listener.onProcessChat((ProcessChatEvent) event);
            else if (event instanceof Event) listener.onRawDiscordEventReceived((Event) event);
        }
    }
    public static int purgeChannel(TextChannel channel) {
        List<Message> messages = channel.getHistory().retrieveAll();
        int deletions = messages.size();
        while (messages.size() > 100) {
            List<Message> messagesToDelete = messages.subList(0, 100);
            channel.deleteMessages(messagesToDelete);
            messages.removeAll(messagesToDelete);
        }
        if (messages.size() > 0) channel.deleteMessages(messages);
        return deletions;
    }

}
