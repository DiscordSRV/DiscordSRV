package github.scarsz.discordsrv;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import github.scarsz.discordsrv.hooks.VaultHook;
import github.scarsz.discordsrv.listeners.PlayerAchievementsListener;
import github.scarsz.discordsrv.listeners.PlayerChatListener;
import github.scarsz.discordsrv.listeners.PlayerDeathListener;
import github.scarsz.discordsrv.listeners.PlayerJoinLeaveListener;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.Metrics;
import github.scarsz.discordsrv.objects.threads.ChannelTopicUpdater;
import github.scarsz.discordsrv.objects.threads.ConsoleMessageQueueWorker;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.HttpUtil;
import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

@SuppressWarnings({"Convert2streamapi", "unused", "unchecked", "ResultOfMethodCallIgnored", "WeakerAccess", "ConstantConditions"})
public class DiscordSRV extends JavaPlugin {

    public static final String snapshotId = "OFFICIAL-V13.0";
    public long startTime = System.currentTimeMillis();
    public static boolean updateIsAvailable = false;

    @Getter private Map<Long, String> channels = new LinkedHashMap<>(); //todo loading
    @Getter private TextChannel consoleChannel;
    @Getter private JDA jda;
    @Getter private List<String> randomPhrases = new ArrayList<>();
    @Getter private ChannelTopicUpdater channelTopicUpdater;
    @Getter private ConsoleMessageQueueWorker consoleMessageQueueWorker;
    @Getter private Map<String, String> colors = new HashMap<>();
    @Getter private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Getter private Map<String, String> responses = new HashMap<>();
    @Getter private List<String> consoleMessageQueue = new LinkedList<>();
    @Getter private List<UUID> unsubscribedPlayers = new ArrayList<>();

    public static DiscordSRV getPlugin() {
        return getPlugin(DiscordSRV.class);
    }
    public Map.Entry<Long, String> getMainChatChannelPair() {
        return channels.size() != 0 ? channels.entrySet().iterator().next() : null;
    }
    public String getMainChatChannel() {
        Map.Entry<Long, String> pair = getMainChatChannelPair();
        return pair != null ? pair.getValue() : "";
    }
    public TextChannel getMainTextChannel() {
        Map.Entry<Long, String> pair = getMainChatChannelPair();
        return pair != null ? jda.getTextChannelById(String.valueOf(pair.getKey())) : null;
    }

    // log messages
    public static void info(String message) {
        DiscordSRV.getPlugin().getLogger().info(message);
    }
    public static void warning(String message) {
        DiscordSRV.getPlugin().getLogger().warning(message);
    }
    public static void error(String message) {
        DiscordSRV.getPlugin().getLogger().severe(message);
    }
    public static void debug(String message) {
        // return if plugin is not in debug mode
        if (!DiscordSRV.getPlugin().getConfig().getBoolean("Debug")) return;

        DiscordSRV.getPlugin().getLogger().info("[DEBUG] " + message + "\n" + ExceptionUtils.getStackTrace(new Throwable()));
    }

    @Override
    public void onEnable() {
        // check if the person is trying to use the plugin on Thermos without updating to ASM5
        try {
            File specialSourceFile = new File("libraries/net/md-5/SpecialSource/1.7-SNAPSHOT/SpecialSource-1.7-SNAPSHOT.jar");
            if (specialSourceFile.exists() && DigestUtils.md5Hex(FileUtils.readFileToByteArray(specialSourceFile)).equalsIgnoreCase("096777a1b6098130d6c925f1c04050a3")) {
                getLogger().warning("");
                getLogger().warning("");
                getLogger().warning("You're attempting to use DiscordSRV on Thermos without applying the SpecialSource/ASM5 fix.");
                getLogger().warning("DiscordSRV WILL NOT work without it on Thermos. Blame the Thermos developers for having outdated libraries.");
                getLogger().warning("");
                getLogger().warning("Instructions for updating to ASM5:");
                getLogger().warning("1. Navigate to the libraries/net/md-5/SpecialSource/1.7-SNAPSHOT folder of the server");
                getLogger().warning("2. Delete the SpecialSource-1.7-SNAPSHOT.jar jar file");
                getLogger().warning("3. Download SpecialSource v1.7.4 from http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar");
                getLogger().warning("4. Copy the jar file to the libraries/net/md-5/SpecialSource/1.7-SNAPSHOT folder");
                getLogger().warning("5. Rename the jar file you just copied to SpecialSource-1.7-SNAPSHOT.jar");
                getLogger().warning("6. Restart the server");
                getLogger().warning("");
                getLogger().warning("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // remove all event listeners from existing jda to prevent having multiple listeners when jda is recreated
        if (jda != null) jda.getRegisteredListeners().forEach(o -> jda.removeEventListener(o));

        // make sure configuration files exist, save default ones if they don't
        if (!new File(getDataFolder(), "colors.json").exists()) saveResource("colors.json", false);
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", false);
            reloadConfig();
        }

        //todo config migration

        // update check
        if (!getConfig().getBoolean("UpdateCheckDisabled")) {
            double latest = Double.parseDouble(HttpUtil.requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/master/latestbuild"));
            if (latest > Double.parseDouble(getDescription().getVersion())) {
                getLogger().warning(System.lineSeparator() + System.lineSeparator() + "The current build of DiscordSRV is outdated! Get build " + latest + " at http://dev.bukkit.org/bukkit-plugins/discordsrv/" + System.lineSeparator() + System.lineSeparator());
                updateIsAvailable = true;
            }

            double minimum = Double.parseDouble(HttpUtil.requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/master/minimumbuild"));
            if (minimum > Double.parseDouble(getDescription().getVersion())) {
                getLogger().warning(System.lineSeparator() + System.lineSeparator() + "The current build of DiscordSRV does not meet the minimum! DiscordSRV will not start. Get build " + latest + " at http://dev.bukkit.org/server-mods/discordsrv/" + System.lineSeparator() + System.lineSeparator());
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            if (!updateIsAvailable) getLogger().warning("DiscordSRV is up-to-date. For change logs see the latest file at http://dev.bukkit.org/bukkit-plugins/discordsrv/");
        }

        // cool kids club thank yous
        if (!getConfig().getBoolean("CoolKidsClubThankYousDisabled")) {
            String thankYou = HttpUtil.requestHttp("https://github.com/Scarsz/DiscordSRV/raw/randomaccessfiles/coolkidsclub");
            if (thankYou.length() > 1) getLogger().info("Thank you so much to these people for allowing DiscordSRV to grow to what it is: " + thankYou);
        }

        // random phrases for debug handler
        if (!getConfig().getBoolean("RandomPhrasesDisabled"))
            Collections.addAll(randomPhrases, HttpUtil.requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/randomaccessfiles/randomphrases").split("\n"));

        // set simplelog level to jack shit because we have our own appender, remove timestamps from JDA messages
        if (SimpleLog.LEVEL != SimpleLog.Level.OFF) {
            SimpleLog.LEVEL = SimpleLog.Level.OFF;
            SimpleLog.addListener(new SimpleLog.LogListener() {
                @Override
                public void onLog(SimpleLog simpleLog, SimpleLog.Level level, Object o) {
                    switch (level) {
                        case INFO:
                            getLogger().info("[JDA] " + o);
                            break;
                        case WARNING:
                            getLogger().warning("[JDA] " + o);
                            break;
                        case FATAL:
                            getLogger().severe("[JDA] " + o);
                            break;
                    }
                }
                @Override
                public void onError(SimpleLog simpleLog, Throwable throwable) {}
            });
        }

        // shutdown previously existing jda if plugin gets reloaded
        if (jda != null) try { jda.shutdown(false); } catch (Exception e) { e.printStackTrace(); }

        // log in to discord
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .setAudioEnabled(false)
                    .setBulkDeleteSplittingEnabled(false)
                    .setAutoReconnect(true)
                    .setToken(getConfig().getString("BotToken"))
                    .buildBlocking();
        } catch (LoginException | RateLimitedException e) {
            getLogger().severe("DiscordSRV failed to connect to Discord. Reason: " + e.getLocalizedMessage());
        } catch (InterruptedException e) {
            getLogger().severe("This shouldn't have happened under any circumstance. Weird.");
            e.printStackTrace();
        }

        // game status
        if (!getConfig().getString("DiscordGameStatus").isEmpty())
            jda.getPresence().setGame(Game.of(getConfig().getString("DiscordGameStatus")));

        // print the things the bot can see
        for (Guild server : jda.getGuilds()) {
            getLogger().info("Found guild " + server);
            for (TextChannel channel : server.getTextChannels()) {
                getLogger().info("- " + channel);
            }
        }

        // show warning if bot wasn't in any guilds
        if (jda.getGuilds().size() == 0) {
            getLogger().severe("The bot is not a part of any Discord guilds. Follow the installation instructions.");
            return;
        }

        // set console channel
        consoleChannel = !getConfig().getString("DiscordConsoleChannelId").equals("") ? jda.getTextChannelById(getConfig().getString("DiscordConsoleChannelId")) : null;

        // see if console channel exists; if it does, tell user where it's been assigned & add console appender
        if (consoleChannel != null) {
            getLogger().info("Console forwarding assigned to text channel " + consoleChannel);

            //todo console appender
        } else {
            getLogger().info("Console channel ID was blank, not forwarding console output");
        }

        // warn if no channels have been linked
        if (getMainTextChannel() == null) getLogger().warning("No channels have been linked");
        if (getMainTextChannel() == null && consoleChannel == null) getLogger().severe("No channels nor a console channel have been linked. Have you followed the installation instructions?");
        // warn if the console channel is connected to a chat channel
        if (getMainTextChannel().getId().equals(consoleChannel.getId())) getLogger().warning("The console channel was assigned to a channel that's being used for chat. Did you blindly copy/paste an ID into the channel ID config option?");

        // send server startup message
        DiscordUtil.sendMessage(getMainTextChannel(), getConfig().getString("DiscordChatChannelServerStartupMessage"));

        // start channel topic updater
        if (channelTopicUpdater != null ) {
            if (channelTopicUpdater.getState() == Thread.State.NEW) {
                channelTopicUpdater.start();
            } else {
                channelTopicUpdater.interrupt();
                channelTopicUpdater = new ChannelTopicUpdater();
                channelTopicUpdater.start();
            }
        } else {
            channelTopicUpdater = new ChannelTopicUpdater();
            channelTopicUpdater.start();
        }

        // start console message queue worker
        if (consoleMessageQueueWorker != null ) {
            if (consoleMessageQueueWorker.getState() == Thread.State.NEW) {
                consoleMessageQueueWorker.start();
            } else {
                consoleMessageQueueWorker.interrupt();
                consoleMessageQueueWorker = new ConsoleMessageQueueWorker();
                consoleMessageQueueWorker.start();
            }
        } else {
            consoleMessageQueueWorker = new ConsoleMessageQueueWorker();
            consoleMessageQueueWorker.start();
        }

        // start lag (tps) monitor
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Lag(), 100L, 1L);

        // register bukkit events
        Bukkit.getPluginManager().registerEvents(new PlayerAchievementsListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);

        // enable metrics
        if (!getConfig().getBoolean("MetricsDisabled"))
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (IOException e) {
                getLogger().warning("Unable to start metrics. Oh well.");
            }

        // load user-defined colors
        if (!new File(getDataFolder(), "colors.json").exists()) saveResource("colors.json", false);
        try {
            LinkedTreeMap<String, String> colorsJson = gson.fromJson(Files.toString(new File(getDataFolder(), "colors.json"), Charset.defaultCharset()), LinkedTreeMap.class);
            for (String key : colorsJson.keySet()) {
                String definition = colorsJson.get(key);
                key = key.toLowerCase();
                colors.put(key, definition);
            }
            getLogger().info("Colors: " + colors);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // load canned responses
        try {
            responses.clear();
            String key = "DiscordCannedResponses";

            FileReader fr = new FileReader(new File(getDataFolder(), "config.yml"));
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

        // start extra threads

    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling DiscordSRV...");
        long shutdownStartTime = System.currentTimeMillis();

        // send server shutdown message
        DiscordUtil.sendMessageBlocking(getMainTextChannel(), getConfig().getString("DiscordChatChannelServerShutdownMessage"));

        // set status as invisible to not look like bot is online when it's not
        jda.getPresence().setStatus(OnlineStatus.INVISIBLE);

        // shut down jda gracefully with false to allow it to be reusable
        jda.shutdown(false);

        // kill channel topic updater
        if (channelTopicUpdater != null) channelTopicUpdater.interrupt();

        getLogger().info("Shutdown completed in " + (System.currentTimeMillis() - shutdownStartTime) + "ms");
    }

    public void processChatMessage(Player player, String message, String channel, boolean cancelled) {
        // ReportCanceledChatEvents debug message
        if (DiscordSRV.getPlugin().getConfig().getBoolean("ReportCanceledChatEvents")) DiscordSRV.getPlugin().getLogger().info("Chat message received, canceled: " + cancelled);

        // return if player doesn't have permission
        if (!player.hasPermission("discordsrv.chat") && !(player.isOp() || player.hasPermission("discordsrv.admin"))) {
            if (DiscordSRV.getPlugin().getConfig().getBoolean("EventDebug")) DiscordSRV.getPlugin().getLogger().info("User " + player.getName() + " sent a message but it was not delivered to Discord due to lack of permission");
            return;
        }

        // return if mcMMO is enabled and message is from party or admin chat
        //todo if (Bukkit.getPluginManager().isPluginEnabled("mcMMO") && (ChatAPI.isUsingPartyChat(sender) || ChatAPI.isUsingAdminChat(sender))) return;

        // return if event canceled
        if (DiscordSRV.getPlugin().getConfig().getBoolean("DontSendCanceledChatEvents") && cancelled) return;

        // return if should not send in-game chat
        if (!DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) return;

        // return if user is unsubscribed from Discord and config says don't send those peoples' messages
        if (!DiscordSRV.getPlugin().getUnsubscribedPlayers().contains(player.getUniqueId()) && !DiscordSRV.getPlugin().getConfig().getBoolean("MinecraftUnsubscribedMessageForwarding")) return;

        // return if doesn't match prefix filter
        if (!message.startsWith(DiscordSRV.getPlugin().getConfig().getString("DiscordChatChannelPrefix"))) return;

        String userPrimaryGroup = VaultHook.getPrimaryGroup(player);
        boolean hasGoodGroup = !"".equals(userPrimaryGroup.replace(" ", ""));

        String format = hasGoodGroup ? DiscordSRV.getPlugin().getConfig().getString("MinecraftChatToDiscordMessageFormat") : DiscordSRV.getPlugin().getConfig().getString("MinecraftChatToDiscordMessageFormatNoPrimaryGroup");
        String discordMessage = format
                .replaceAll("&([0-9a-qs-z])", "")
                .replace("%message%", DiscordUtil.stripColor(message))
                .replace("%primarygroup%", VaultHook.getPrimaryGroup(player))
                .replace("%displayname%", DiscordUtil.stripColor(DiscordUtil.escapeMarkdown(player.getDisplayName())))
                .replace("%username%", DiscordUtil.stripColor(DiscordUtil.escapeMarkdown(player.getName())))
                .replace("%world%", player.getWorld().getName())
                //todo .replace("%worldalias%", DiscordUtil.stripColor(MultiverseCoreHook.getWorldAlias(sender.getWorld().getName())))
                .replace("%time%", new Date().toString())
                .replace("%date%", new Date().toString());

        discordMessage = DiscordUtil.convertMentionsFromNames(discordMessage, DiscordSRV.getPlugin().getMainTextChannel().getGuild());

        if (channel == null) DiscordUtil.sendMessage(getMainTextChannel(), discordMessage);
        else DiscordUtil.sendMessage(getTextChannelFromChannelName(channel), discordMessage);
    }

    private TextChannel getTextChannelFromChannelName(String inGameChannelName) {
        for (Map.Entry<Long, String> channelSet : channels.entrySet())
            if (channelSet.getValue().equals(inGameChannelName))
                return jda.getTextChannelById(channelSet.getValue());
        return null;
    }

//    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//        if (args.length == 0) {
//            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("DiscordCommandFormat")));
//            return true;
//        }
//        if (args[0].equals("?") || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("dowhatyouwantcauseapirateisfreeyouareapirateyarharfiddledeedee")) {
//            if (!sender.isOp() && !sender.hasPermission("discordsrv.admin")) sender.sendMessage(ChatColor.AQUA + "/discord toggle/subscribe/unsubscribe/link/linked/clearlinked");
//            else sender.sendMessage(ChatColor.AQUA + "/discord bcast/setpicture/reload/rebuild/debug/toggle/subscribe/unsubscribe/link/linked/clearlinked");
//        }
//        if (args[0].equalsIgnoreCase("bcast")) {
//            if (!sender.isOp() && !sender.hasPermission("discordsrv.admin")) {
//                sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
//                return true;
//            }
//            List<String> messageStrings = Arrays.asList(args);
//            String message = String.join(" ", messageStrings.subList(1, messageStrings.size()));
//            sendMessage(chatChannel, message);
//        }
//        if (args[0].equalsIgnoreCase("setpicture")) {
//            if (!sender.isOp() && !sender.hasPermission("discordsrv.admin")) {
//                sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
//                return true;
//            }
//            if (args.length < 2) {
//                sender.sendMessage(ChatColor.AQUA + "Must give URL to picture to set as bot picture");
//                return true;
//            }
//            try {
//                sender.sendMessage(ChatColor.AQUA + "Downloading picture...");
//                ReadableByteChannel in = Channels.newChannel(new URL(args[1]).openStream());
//                FileChannel out = new FileOutputStream(getDataFolder().getAbsolutePath() + "/picture.jpg").getChannel();
//                out.transferFrom(in, 0, Long.MAX_VALUE);
//                out.close();
//            } catch (IOException e) {
//                sender.sendMessage(ChatColor.AQUA + "Download failed: " + e.getMessage());
//                return true;
//            }
//            try {
//                jda.getAccountManager().setAvatar(AvatarUtil.getAvatar(new File(getDataFolder().getAbsolutePath() + "/picture.jpg"))).update();
//                sender.sendMessage(ChatColor.AQUA + "Picture updated successfully");
//            } catch (UnsupportedEncodingException e) {
//                sender.sendMessage(ChatColor.AQUA + "Error setting picture as avatar: " + e.getMessage());
//            }
//        }
//        if (args[0].equalsIgnoreCase("reload")) {
//            if (!sender.isOp() && !sender.hasPermission("discordsrv.admin")) return true;
//            reloadConfig();
//            sender.sendMessage(ChatColor.AQUA + "DiscordSRV config has been reloaded. Some config options require a restart.");
//        }
//        if (args[0].equalsIgnoreCase("debug")) {
//            if (!sender.isOp() && !sender.hasPermission("discordsrv.admin")) return true;
//            String hastebinUrl = DebugHandler.run();
//            sender.sendMessage(ChatColor.AQUA + "Debug information has been uploaded to " + hastebinUrl + ". Please join the official DiscordSRV guild on the plugin page if you need help understanding this log- be sure to share it with us.");
//        }
//        if (args[0].equalsIgnoreCase("rebuild")) {
//            if (!sender.isOp() && !sender.hasPermission("discordsrv.admin")) return true;
//            //buildJda();
//            sender.sendMessage(ChatColor.AQUA + "Disabled because no workie");
//        }
//
//        if (!(sender instanceof Player)) return true;
//        Player senderPlayer = (Player) sender;
//
//        // subscriptions
//        if (args[0].equalsIgnoreCase("toggle")) setIsSubscribed(senderPlayer.getUniqueId(), !getIsSubscribed(senderPlayer.getUniqueId()));
//        if (args[0].equalsIgnoreCase("subscribe")) setIsSubscribed(senderPlayer.getUniqueId(), true);
//        if (args[0].equalsIgnoreCase("unsubscribe")) setIsSubscribed(senderPlayer.getUniqueId(), false);
//        if (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("subscribe") || args[0].equalsIgnoreCase("unsubscribe"))
//            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getIsSubscribed(senderPlayer.getUniqueId())
//                    ? getConfig().getString("MinecraftSubscriptionMessagesOnSubscribe")
//                    : getConfig().getString("MinecraftSubscriptionMessagesOnUnsubscribe")
//            ));
//
//        // account linking
//        if (args[0].equalsIgnoreCase("link")) {
//            String code = "";
//            Random random = new Random();
//            for (int i = 0; i < 4; i++) code += random.nextInt(10);
//            linkingCodes.put(code, senderPlayer.getUniqueId());
//            sender.sendMessage(ChatColor.AQUA + "Your link code is " + code + ". Send a private message to the bot (" + jda.getSelfInfo().getUsername() + ") on Discord with just this code as the message to link your Discord account to your UUID.");
//        }
//        if (args[0].equalsIgnoreCase("linked")) {
//            sender.sendMessage(ChatColor.AQUA + "Your UUID is linked to " + (accountLinkManager.getId(senderPlayer.getUniqueId()) != null ? jda.getUserById(accountLinkManager.getId(senderPlayer.getUniqueId())) != null ? jda.getUserById(accountLinkManager.getId(senderPlayer.getUniqueId())) : accountLinkManager.getId(senderPlayer.getUniqueId()) : "nobody."));
//        }
//        if (args[0].equalsIgnoreCase("clearlinked")) {
//            sender.sendMessage(ChatColor.AQUA + "Your UUID is no longer associated with " + (accountLinkManager.getId(senderPlayer.getUniqueId()) != null ? jda.getUserById(accountLinkManager.getId(senderPlayer.getUniqueId())) != null ? jda.getUserById(accountLinkManager.getId(senderPlayer.getUniqueId())) : accountLinkManager.getId(senderPlayer.getUniqueId()) : "nobody. Never was."));
//            accountLinkManager.unlink(senderPlayer.getUniqueId());
//        }
//
//        return true;
//    }

//    private static String requestHttp(String requestUrl) {
//        try {
//            return IOUtils.toString(new URL(requestUrl), Charset.defaultCharset());
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "";
//        }
//    }
//    public static void sendMessage(TextChannel channel, String message) {
//        sendMessage(channel, message, true, 0);
//    }
//    public static void sendMessage(TextChannel channel, String message, boolean editMessage, int expiration) {
//        if (jda == null || channel == null || (!channel.checkPermission(jda.getSelfInfo(), Permission.MESSAGE_READ) || !channel.checkPermission(jda.getSelfInfo(), Permission.MESSAGE_WRITE))) return;
//        message = DiscordUtil.stripColor(message)
//                .replaceAll("[&§][0-9a-fklmnor]", "") // removing &'s with addition of non-caught §'s if they get through somehow
//                .replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "")
//                .replaceAll("\\[[0-9]{1,3}m", "")
//                .replace("[m", "");
//
//        if (editMessage)
//            for (String phrase : DiscordSRV.plugin.getConfig().getStringList("DiscordChatChannelCutPhrases"))
//                message = message.replace(phrase, "");
//
//        String overflow = null;
//        if (message.length() > 2000) {
//            plugin.getLogger().warning("Tried sending message with length of " + message.length() + " (" + (message.length() - 2000) + " over limit)");
//            overflow = message.substring(1999);
//            message = message.substring(0, 1999);
//        }
//
//        channel.sendMessageAsync(message, m -> {
//            if (expiration > 0) {
//                try { Thread.sleep(expiration); } catch (InterruptedException e) { e.printStackTrace(); }
//                if (channel.checkPermission(DiscordSRV.jda.getSelfInfo(), Permission.MESSAGE_MANAGE)) m.deleteMessage(); else DiscordSRV.plugin.getLogger().warning("Could not delete message in channel " + channel + ", no permission to manage messages");
//            }
//        });
//        if (overflow != null) sendMessage(channel, overflow, editMessage, expiration);
//    }
//    public static void sendMessageToChatChannel(String message) {
//        sendMessage(chatChannel, message);
//    }
//    public static void sendMessageToConsoleChannel(String message) {
//        sendMessage(consoleChannel, message);
//    }
//    private static String getPrimaryGroup(Player player) {
//        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return " ";
//
//        RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> service = DiscordSRV.plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
//
//        if (service == null) return " ";
//        try {
//            String primaryGroup = service.getProvider().getPrimaryGroup(player);
//            if (!primaryGroup.equals("default")) return primaryGroup;
//        } catch (Exception ignored) { }
//        return " ";
//    }
//    public static String getAllRoles(GuildMessageReceivedEvent event) {
//        String roles = "";
//        for (Role role : event.getGuild().getRolesForUser(event.getAuthor())) {
//            roles += role.getName() + DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftAllRolesSeperator");
//        }
//        if (!roles.isEmpty()) roles = roles.substring(0, roles.length() - DiscordSRV.plugin.getConfig().getString("DiscordToMinecraftAllRolesSeperator").length());
//        return roles;
//    }
//    public static Role getTopRole(GuildMessageReceivedEvent event) {
//        Role highestRole = null;
//        for (Role role : event.getGuild().getRolesForUser(event.getAuthor())) {
//            if (highestRole == null) highestRole = role;
//            else if (highestRole.getPosition() < role.getPosition()) highestRole = role;
//        }
//        return highestRole;
//    }
//    public static String getRoleName(Role role) {
//        return role == null ? "" : role.getName();
//    }
//    public static List<Player> getOnlinePlayers() {
//        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
//        List<Player> playersToRemove = new ArrayList<>();
//        for (Player player : players) {
//            if (VanishedPlayerCheck.checkPlayerIsVanished(player))
//                playersToRemove.add(player);
//        }
//        players.removeAll(playersToRemove);
//        return players;
//    }
//    private static boolean getIsSubscribed(UUID uniqueId) {
//        return !unsubscribedPlayers.contains(uniqueId.toString());
//    }
//    private static void setIsSubscribed(UUID uniqueId, boolean subscribed) {
//        if (subscribed && unsubscribedPlayers.contains(uniqueId.toString())) unsubscribedPlayers.remove(uniqueId.toString());
//        if (!subscribed && !unsubscribedPlayers.contains(uniqueId.toString())) unsubscribedPlayers.add(uniqueId.toString());
//    }
//    public static void broadcastMessageToMinecraftServer(String message, String rawMessage, String channel) {
//        boolean usingChatPlugin = hookedPlugins.size() > 0 || !channel.equalsIgnoreCase(plugin.getConfig().getString("DiscordMainChatChannel"));
//
//        if (!usingChatPlugin) {
//            for (Player player : Bukkit.getOnlinePlayers()) {
//                if (getIsSubscribed(player.getUniqueId())) {
//                    player.sendMessage(message);
//                    notifyPlayersOfMentions(Collections.singletonList(player), rawMessage);
//                }
//            }
//        } else {
//            if (hookedPlugins.contains("herochat")) HerochatHook.broadcastMessageToChannel(channel, message, rawMessage);
//            if (hookedPlugins.contains("legendchat")) LegendChatHook.broadcastMessageToChannel(channel, message, rawMessage);
//            if (hookedPlugins.contains("lunachat")) LunaChatHook.broadcastMessageToChannel(channel, message, rawMessage);
//            if (hookedPlugins.contains("townychat")) TownyChatHook.broadcastMessageToChannel(channel, message, rawMessage);
//            if (hookedPlugins.contains("venturechat")) VentureChatHook.broadcastMessageToChannel(channel, message, rawMessage);
//        }
//    }
//    public static String convertRoleToMinecraftColor(Role role) {
//        if (role == null) {
//            if (DiscordSRV.plugin.getConfig().getBoolean("ColorLookupDebug")) DiscordSRV.plugin.getLogger().info("Role null, using no color");
//            return "";
//        }
//        String colorHex = Integer.toHexString(role.getColor());
//        String output = colors.get(colorHex);
//
//        if (DiscordSRV.plugin.getConfig().getBoolean("ColorLookupDebug")) DiscordSRV.plugin.getLogger().info("Role " + role + " results to hex \"" + colorHex + "\" and output \"" + output + "\"");
//
//        return output != null ? output : "";
//    }
//    private static String convertMentionsFromNames(String message) {
//        if (!message.contains("@")) return message;
//        List<String> splitMessage = new ArrayList<>(Arrays.asList(message.split("@| ")));
//        for (User user : chatChannel.getUsers())
//            for (String segment : splitMessage)
//                if (user.getUsername().toLowerCase().equals(segment.toLowerCase()) || (chatChannel.getGuild().getNicknameForUser(user) != null && chatChannel.getGuild().getNicknameForUser(user).toLowerCase().equals(segment.toLowerCase()))) {
//                    splitMessage.set(splitMessage.indexOf(segment), user.getAsMention());
//                }
//        splitMessage.removeAll(Arrays.asList("", null));
//        return String.join(" ", splitMessage);
//    }
//    public static String getDestinationChannelName(TextChannel textChannel) {
//        for (String channelName : channels.keySet()) {
//            String registeredChannelId = getTextChannelFromChannelName(channelName).getId();
//            String paramChannelId = textChannel.getId();
//            if (registeredChannelId.equals(paramChannelId)) {
//                return channelName;
//            }
//        }
//        return null;
//    }
//    public static void notifyPlayersOfMentions(List<Player> possiblyPlayers, String parseMessage) {
//        if (!canUsePingNotificationSounds) return;
//
//        List<String> splitMessage = new ArrayList<>();
//        for (String phrase : parseMessage.replaceAll("[^a-zA-Z]", " ").split(" ")) splitMessage.add(phrase.toLowerCase());
//
//        for (Player player : possiblyPlayers) {
//            boolean playerOnline = player.isOnline();
//            boolean phraseContainsName = splitMessage.contains(player.getName().toLowerCase());
//            boolean phraseContainsDisplayName = splitMessage.contains(DiscordUtil.stripColor(player.getDisplayName()).toLowerCase());
//            boolean shouldDing = phraseContainsName || phraseContainsDisplayName;
//            if (playerOnline && shouldDing) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 1);
//        }
//    }
//    public static TextChannel getTextChannelFromChannelName(String channelName) {
//        if (channels.containsKey(channelName)) return channels.get(channelName);
//        if (channels.containsKey(channelName.toLowerCase())) return channels.get(channelName.toLowerCase());
//        return null;
//    }
//    public static boolean chatChannelIsLinked(String channelName) {
//        return channels.containsKey(channelName) || channels.containsKey(channelName.toLowerCase());
//    }
//    public static String getDisplayName(Guild guild, User user) {
//        String nickname = guild.getNicknameForUser(user);
//        return nickname == null ? user.getUsername() : nickname;
//    }
//    public static boolean checkIfPluginEnabled(String pluginName) {
//        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
//            if (plugin.getName().toLowerCase().startsWith(pluginName.toLowerCase())) return true;
//        }
//        return false;
//    }
//    public static void notifyListeners(Object event) {
//        for (DiscordSRVListenerInterface listener : listeners) {
//            if (listener == null) continue;
//
//            if (event instanceof MessageReceivedEvent) listener.onDiscordMessageReceived((MessageReceivedEvent) event);
//            else if (event instanceof ProcessChatEvent) listener.onProcessChat((ProcessChatEvent) event);
//            else if (event instanceof Event) listener.onRawDiscordEventReceived((Event) event);
//        }
//    }
//    public static int purgeChannel(TextChannel channel) {
//        List<Message> messages = channel.getHistory().retrieveAll();
//        int deletions = messages.size();
//        while (messages.size() > 100) {
//            List<Message> messagesToDelete = messages.subList(0, 100);
//            channel.deleteMessages(messagesToDelete);
//            messages.removeAll(messagesToDelete);
//        }
//        if (messages.size() > 0) channel.deleteMessages(messages);
//        return deletions;
//    }
//    public static String escapeMarkdown(String text) {
//        return text.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~");
//    }

}
