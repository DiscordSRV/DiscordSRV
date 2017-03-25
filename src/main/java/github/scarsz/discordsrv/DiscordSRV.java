package github.scarsz.discordsrv;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.scarsz.discordsrv.api.ApiManager;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostBroadcastEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.hooks.permissions.VaultHook;
import github.scarsz.discordsrv.hooks.chat.*;
import github.scarsz.discordsrv.hooks.world.MultiverseCoreHook;
import github.scarsz.discordsrv.listeners.*;
import github.scarsz.discordsrv.objects.*;
import github.scarsz.discordsrv.objects.metrics.BStats;
import github.scarsz.discordsrv.objects.metrics.MCStats;
import github.scarsz.discordsrv.objects.metrics.MetricsManager;
import github.scarsz.discordsrv.objects.threads.ChannelTopicUpdater;
import github.scarsz.discordsrv.objects.threads.ConsoleMessageQueueWorker;
import github.scarsz.discordsrv.objects.threads.ServerWatchdog;
import github.scarsz.discordsrv.util.*;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPI;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"Convert2streamapi", "unused", "unchecked", "ResultOfMethodCallIgnored", "WeakerAccess", "ConstantConditions"})
public class DiscordSRV extends JavaPlugin implements Listener {

    public static final ApiManager api = new ApiManager();
    public static boolean updateIsAvailable = false;

    @Getter private AccountLinkManager accountLinkManager;
    @Getter private CancellationDetector<AsyncPlayerChatEvent> cancellationDetector = null;
    @Getter private Map<String, TextChannel> channels = new LinkedHashMap<>(); // <in-game channel name, discord channel>
    @Getter private ChannelTopicUpdater channelTopicUpdater;
    @Getter private Map<String, String> colors = new HashMap<>();
    @Getter private CommandManager commandManager = new CommandManager();
    @Getter private File configFile = new File(getDataFolder(), "config.yml");
    @Getter private TextChannel consoleChannel;
    @Getter private Queue<String> consoleMessageQueue = new LinkedList<>();
    @Getter private ConsoleMessageQueueWorker consoleMessageQueueWorker;
    @Getter private File messagesFile = new File(getDataFolder(), "messages.yml");
    @Getter private MetricsManager metrics = new MetricsManager(new File(getDataFolder(), "metrics.json"));
    @Getter private GroupSynchronizationManager groupSynchronizationManager = new GroupSynchronizationManager();
    @Getter private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Getter private List<String> hookedPlugins = new ArrayList<>();
    @Getter private JDA jda;
    @Getter private File linkedAccountsFile = new File(getDataFolder(), "linkedaccounts.json");
    @Getter private Random random = new Random();
    @Getter private List<String> randomPhrases = new ArrayList<>();
    @Getter private Map<String, String> responses = new HashMap<>();
    @Getter private ServerWatchdog serverWatchdog;
    @Getter private long startTime = System.currentTimeMillis();
    @Getter private List<UUID> unsubscribedPlayers = new ArrayList<>();

    public static DiscordSRV getPlugin() {
        return getPlugin(DiscordSRV.class);
    }
    public Map.Entry<String, TextChannel> getMainChatChannelPair() {
        return channels.size() != 0 ? channels.entrySet().iterator().next() : null;
    }
    public String getMainChatChannel() {
        Map.Entry<String, TextChannel> pair = getMainChatChannelPair();
        return pair != null ? pair.getKey() : "";
    }
    public TextChannel getMainTextChannel() {
        Map.Entry<String, TextChannel> pair = getMainChatChannelPair();
        return pair != null ? pair.getValue() : null;
    }
    public Guild getMainGuild() {
        return getMainTextChannel() != null
                ? getMainTextChannel().getGuild()
                : consoleChannel != null
                    ? consoleChannel.getGuild()
                    : null;
    }
    public TextChannel getDestinationTextChannelForGameChannelName(String gameChannelName) {
        TextChannel foundChannel = channels.get(gameChannelName);
        if (foundChannel != null) return foundChannel; // found case-sensitive channel

        // no case-sensitive channel found, try case in-sensitive
        for (Map.Entry<String, TextChannel> channelEntry : channels.entrySet())
            if (channelEntry.getKey().equalsIgnoreCase(gameChannelName)) return channelEntry.getValue();

        return null; // no channel found, case-insensitive or not
    }
    public String getDestinationGameChannelNameForTextChannel(TextChannel source) {
        for (Map.Entry<String, TextChannel> channelEntry : channels.entrySet())
            if (channelEntry.getValue().getId().equals(source.getId()))
                return channelEntry.getKey();
        return null;
    }

    // log messages
    public static void info(LangUtil.InternalMessage message) {
        info(message.toString());
    }
    public static void info(String message) {
        getPlugin().getLogger().info(message);
    }
    public static void warning(LangUtil.InternalMessage message) {
        warning(message.toString());
    }
    public static void warning(String message) {
        getPlugin().getLogger().warning(message);
    }
    public static void error(LangUtil.InternalMessage message) {
        error(message.toString());
    }
    public static void error(String message) {
        getPlugin().getLogger().severe(message);
    }
    public static void debug(String message) {
        // return if plugin is not in debug mode
        if (getPlugin().getConfig().getInt("DebugLevel") == 0) return;

        getPlugin().getLogger().info("[DEBUG] " + message + (getPlugin().getConfig().getInt("DebugLevel") >= 2 ? "\n" + DebugUtil.getStackTrace() : ""));
    }

    @Override
    public void onEnable() {
        new Thread(this::init, "DiscordSRV - Initialization").start();
    }

    public void init() {
        // check if the person is trying to use the plugin without updating to ASM 5
        try {
            File specialSourceFile = new File("libraries/net/md-5/SpecialSource/1.7-SNAPSHOT/SpecialSource-1.7-SNAPSHOT.jar");
            if (!specialSourceFile.exists()) specialSourceFile = new File("bin/net/md-5/SpecialSource/1.7-SNAPSHOT/SpecialSource-1.7-SNAPSHOT.jar");
            if (specialSourceFile.exists() && DigestUtils.md5Hex(FileUtils.readFileToByteArray(specialSourceFile)).equalsIgnoreCase("096777a1b6098130d6c925f1c04050a3")) {
                warning(LangUtil.InternalMessage.ASM_WARNING.toString()
                                .replace("{specialsourcefolder}", specialSourceFile.getParentFile().getPath())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // remove all event listeners from existing jda to prevent having multiple listeners when jda is recreated
        if (jda != null) jda.getRegisteredListeners().forEach(o -> jda.removeEventListener(o));

        // make sure configuration file exists, save default ones if they don't
        if (!configFile.exists()) {
            LangUtil.saveConfig();
            reloadConfig();
        }
        // make sure lang file exists, save default ones if they don't
        if (!messagesFile.exists()) {
            LangUtil.saveMessages();
            LangUtil.reloadMessages();
        }

        ConfigUtil.migrate();

        try {
            getConfig();
        } catch (IllegalArgumentException e) {
            error(LangUtil.InternalMessage.INVALID_CONFIG + ": " + e.getMessage());
            try {
                new Yaml().load(FileUtils.readFileToString(getConfigFile(), Charset.defaultCharset()));
            } catch (IOException io) {
                error(io.getMessage());
            }
            return;
        }

        // update check
        if (!getConfig().getBoolean("UpdateCheckDisabled")) {
            updateIsAvailable = UpdateUtil.checkForUpdates();
            if (!isEnabled()) return; // don't load other shit if the plugin was disabled by the update checker
        }

        // cool kids club thank yous
        if (!getConfig().getBoolean("CoolKidsClubThankYousDisabled")) {
            String thankYou = HttpUtil.requestHttp("https://github.com/Scarsz/DiscordSRV/raw/randomaccessfiles/coolkidsclub").replace("\n", "");
            if (thankYou.length() > 1) info(LangUtil.InternalMessage.DONATOR_THANKS + ": " + thankYou);
        }

        // random phrases for debug handler
        if (!getConfig().getBoolean("RandomPhrasesDisabled"))
            Collections.addAll(randomPhrases, HttpUtil.requestHttp("https://raw.githubusercontent.com/Scarsz/DiscordSRV/randomaccessfiles/randomphrases").split("\n"));

        // set SimpleLog level to jack shit because we have our own appender; remove timestamps from JDA messages
        if (SimpleLog.LEVEL != SimpleLog.Level.OFF) {
            SimpleLog.LEVEL = SimpleLog.Level.OFF;
            SimpleLog.addListener(new SimpleLog.LogListener() {
                @Override
                public void onLog(SimpleLog simpleLog, SimpleLog.Level level, Object o) {
                    switch (level) {
                        case INFO:
                            info("[JDA] " + o);
                            break;
                        case WARNING:
                            warning("[JDA] " + o);
                            break;
                        case FATAL:
                            error("[JDA] " + o);
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
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .setToken(getConfig().getString("BotToken"))
                    .addListener(new DiscordBanListener())
                    .addListener(new DiscordChatListener())
                    .addListener(new DiscordConsoleListener())
                    .addListener(new DiscordDebugListener())
                    .addListener(new DiscordPrivateMessageListener())
                    .buildBlocking();
        } catch (LoginException | RateLimitedException e) {
            error(LangUtil.InternalMessage.FAILED_TO_CONNECT_TO_DISCORD + ": " + e.getMessage());
            return;
        } catch (InterruptedException e) {
            error("This shouldn't have happened under any circumstance.");
            e.printStackTrace();
            return;
        } catch (Exception ignored) {}

        // game status
        if (!getConfig().getString("DiscordGameStatus").isEmpty())
            DiscordUtil.setGameStatus(getConfig().getString("DiscordGameStatus"));

        // print the things the bot can see
        for (Guild server : jda.getGuilds()) {
            info(LangUtil.InternalMessage.FOUND_SERVER + " " + server);
            for (TextChannel channel : server.getTextChannels()) info("- " + channel);
            for (Role role : server.getRoles()) info("- " + role);
        }

        // show warning if bot wasn't in any guilds
        if (jda.getGuilds().size() == 0) {
            DiscordSRV.error(LangUtil.InternalMessage.BOT_NOT_IN_ANY_SERVERS);
            return;
        }

        // set console channel
        consoleChannel = jda.getTextChannelById(getConfig().getString("DiscordConsoleChannelId"));

        // see if console channel exists; if it does, tell user where it's been assigned & add console appender
        if (consoleChannel != null) {
            info(LangUtil.InternalMessage.CONSOLE_FORWARDING_ASSIGNED_TO_CHANNEL + " " + consoleChannel);

            // attach appender to queue console messages
            Logger rootLogger = (Logger) LogManager.getRootLogger();
            rootLogger.addAppender(new ConsoleAppender());

            // start console message queue worker thread
            if (consoleMessageQueueWorker != null) {
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
        } else {
            info(LangUtil.InternalMessage.NOT_FORWARDING_CONSOLE_OUTPUT.toString());
        }

        // load channels
        for (Map.Entry<String, Object> channelEntry : ((MemorySection) getConfig().get("Channels")).getValues(true).entrySet())
            channels.put(channelEntry.getKey(), jda.getTextChannelById((String) channelEntry.getValue()));

        // warn if no channels have been linked
        if (getMainTextChannel() == null) warning(LangUtil.InternalMessage.NO_CHANNELS_LINKED);
        if (getMainTextChannel() == null && consoleChannel == null) error(LangUtil.InternalMessage.NO_CHANNELS_LINKED_NOR_CONSOLE);
        // warn if the console channel is connected to a chat channel
        if (getMainTextChannel() != null && consoleChannel != null && getMainTextChannel().getId().equals(consoleChannel.getId())) warning(LangUtil.InternalMessage.CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL);

        // send server startup message
        DiscordUtil.sendMessage(getMainTextChannel(), LangUtil.Message.SERVER_STARTUP_MESSAGE.toString());

        // start channel topic updater
        if (serverWatchdog != null) {
            if (serverWatchdog.getState() == Thread.State.NEW) {
                serverWatchdog.start();
            } else {
                serverWatchdog.interrupt();
                serverWatchdog = new ServerWatchdog();
                serverWatchdog.start();
            }
        } else {
            serverWatchdog = new ServerWatchdog();
            serverWatchdog.start();
        }

        // start lag (tps) monitor
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Lag(), 100L, 1L);

        // cancellation detector
        if (getConfig().getInt("DebugLevel") > 0) {
            if (cancellationDetector != null) {
                cancellationDetector.close();
                cancellationDetector = null;
            }
            cancellationDetector = new CancellationDetector<>(AsyncPlayerChatEvent.class);
            cancellationDetector.addListener((plugin, event) -> {
                info(LangUtil.InternalMessage.PLUGIN_CANCELLED_CHAT_EVENT.toString()
                        .replace("{plugin}", plugin.toString())
                        .replace("{author}", event.getPlayer().getName())
                        .replace("{message}", event.getMessage())
                );
            });
            info(LangUtil.InternalMessage.CHAT_CANCELLATION_DETECTOR_ENABLED);
        }

        // register events
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new PlayerAchievementsListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerBanListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinLeaveListener(), this);

        // in-game chat events
        if (PluginUtil.pluginHookIsEnabled("herochat")) {
            info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "HeroChat"));
            getServer().getPluginManager().registerEvents(new HerochatHook(), this);
        } else if (PluginUtil.pluginHookIsEnabled("legendchat")) {
            info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "LegendChat"));
            getServer().getPluginManager().registerEvents(new LegendChatHook(), this);
        } else if (PluginUtil.pluginHookIsEnabled("lunachat")) {
            info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "LunaChat"));
            getServer().getPluginManager().registerEvents(new LunaChatHook(), this);
        } else if (PluginUtil.checkIfPluginEnabled("towny") && PluginUtil.pluginHookIsEnabled("townychat")) {
            info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "TownyChat"));
            getServer().getPluginManager().registerEvents(new TownyChatHook(), this);
        } else if (PluginUtil.pluginHookIsEnabled("venturechat")) {
            info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "VentureChat"));
            getServer().getPluginManager().registerEvents(new VentureChatHook(), this);
        } else {
            info(LangUtil.InternalMessage.PLUGIN_HOOKS_NOT_ENABLED);
            getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        }

        // load user-defined colors
        colors.clear();
        for (Map.Entry<String, Object> colorEntry : ((MemorySection) getConfig().get("DiscordChatChannelColorTranslations")).getValues(true).entrySet())
            colors.put(colorEntry.getKey().toUpperCase(), (String) colorEntry.getValue());
        info(LangUtil.InternalMessage.COLORS + " " + colors);

        // load canned responses
        responses.clear();
        for (Map.Entry<String, Object> responseEntry : ((MemorySection) getConfig().get("DiscordCannedResponses")).getValues(true).entrySet())
            responses.put(responseEntry.getKey(), (String) responseEntry.getValue());

        // load account links
        accountLinkManager = new AccountLinkManager(linkedAccountsFile);

        // initialize group synchronization manager
        groupSynchronizationManager.init();

        // start server watchdog
        if (channelTopicUpdater != null) {
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

        // enable metrics
        if (!getConfig().getBoolean("MetricsDisabled")) {
            try {
                MCStats MCStats = new MCStats(this);
                MCStats.start();
            } catch (IOException e) {
                warning("Unable to start metrics: " + e.getMessage());
            }

            BStats bStats = new BStats(this);
            bStats.addCustomChart(new BStats.LambdaSimplePie("linked_channels", () -> String.valueOf(channels.size())));
            bStats.addCustomChart(new BStats.LambdaSimplePie("console_channel_enabled", () -> String.valueOf(consoleChannel != null)));
            bStats.addCustomChart(new BStats.LambdaSingleLineChart("messages_sent_to_discord", () -> metrics.get("messages_sent_to_discord")));
            bStats.addCustomChart(new BStats.LambdaSingleLineChart("messages_sent_to_minecraft", () -> metrics.get("messages_sent_to_minecraft")));
            bStats.addCustomChart(new BStats.LambdaSingleLineChart("console_commands_processed", () -> metrics.get("console_commands_processed")));
            bStats.addCustomChart(new BStats.LambdaSimpleBarChart("hooked_plugins", () -> new HashMap<String, Integer>() {{
                if (hookedPlugins.size() == 0) {
                    put("none", 1);
                } else {
                    for (String hookedPlugin : hookedPlugins) {
                        put(hookedPlugin.toLowerCase(), 1);
                    }
                }
            }}));
            bStats.addCustomChart(new BStats.LambdaAdvancedPie("subscribed_players", () -> new HashMap<String, Integer>() {{
                put("subscribed", ChannelTopicUpdater.getPlayerDataFolder().listFiles(f -> f.getName().endsWith(".dat")).length - unsubscribedPlayers.size());
                put("unsubscribed", unsubscribedPlayers.size());
            }}));
            bStats.addCustomChart(new BStats.LambdaSingleLineChart("minecraft-discord_account_links", () -> accountLinkManager.getLinkedAccounts().size()));
        }
    }

    @Override
    public void onDisable() {
        long shutdownStartTime = System.currentTimeMillis();

        // send server shutdown message
        DiscordUtil.sendMessageBlocking(getMainTextChannel(), LangUtil.Message.SERVER_SHUTDOWN_MESSAGE.toString());

        // set server shutdown topics if enabled
        if (getConfig().getBoolean("ChannelTopicUpdaterChannelTopicsAtShutdownEnabled")) {
            DiscordUtil.setTextChannelTopic(getMainTextChannel(), ChannelTopicUpdater.applyPlaceholders(LangUtil.Message.CHAT_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN.toString()));
            DiscordUtil.setTextChannelTopic(getConsoleChannel(), ChannelTopicUpdater.applyPlaceholders(LangUtil.Message.CONSOLE_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN.toString()));
        }

        // set status as invisible to not look like bot is online when it's not
        if (jda != null) jda.getPresence().setStatus(OnlineStatus.INVISIBLE);

        // shut down jda gracefully
        if (jda != null) jda.shutdown(false);

        // kill channel topic updater
        if (channelTopicUpdater != null) channelTopicUpdater.interrupt();

        // kill console message queue worker
        if (consoleMessageQueueWorker != null) consoleMessageQueueWorker.interrupt();

        // serialize account links to disk
        if (accountLinkManager != null) accountLinkManager.save();

        // close cancellation detector
        if (cancellationDetector != null) cancellationDetector.close();

        if (metrics != null) metrics.save();

        info(LangUtil.InternalMessage.SHUTDOWN_COMPLETED.toString()
                .replace("{ms}", String.valueOf(System.currentTimeMillis() - shutdownStartTime))
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return commandManager.handle(sender, null, new String[] {});
        } else {
            return commandManager.handle(sender, args[0], Arrays.stream(args).skip(1).collect(Collectors.toList()).toArray(new String[0]));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command bukkitCommand, String alias, String[] args) {
        String command = args[0];
        String[] commandArgs = Arrays.stream(args).skip(1).collect(Collectors.toList()).toArray(new String[0]);

        if (command.equals(""))
            return new ArrayList<String>() {{
                for (Map.Entry<String, Method> command : getCommandManager().getCommands().entrySet())
                    if (GamePermissionUtil.hasPermission(sender, command.getValue().getAnnotation(github.scarsz.discordsrv.commands.Command.class).permission()))
                        add(command.getKey());
            }};
        if (commandArgs.length == 0)
            return new ArrayList<String>() {{
                for (Map.Entry<String, Method> commandPair : getCommandManager().getCommands().entrySet())
                    if (commandPair.getKey().toLowerCase().startsWith(command.toLowerCase()))
                        if (GamePermissionUtil.hasPermission(sender, commandPair.getValue().getAnnotation(github.scarsz.discordsrv.commands.Command.class).permission()))
                            add(commandPair.getKey());
            }};
        return null;
    }

    public void processChatMessage(Player player, String message, String channel, boolean cancelled) {
        // log debug message to notify that a chat message was being processed
        debug("Chat message received, canceled: " + cancelled);

        // return if player doesn't have permission
        if (!player.hasPermission("discordsrv.chat")) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord due to lack of permission");
            return;
        }

        // return if mcMMO is enabled and message is from party or admin chat
        if (Bukkit.getPluginManager().isPluginEnabled("mcMMO")) {
            try {
                Method isUsingAdminChat = Class.forName("com.gmail.nossr50.api.ChatAPI").getMethod("isUsingAdminChat", Player.class);
                Method isUsingPartyChat = Class.forName("com.gmail.nossr50.api.ChatAPI").getMethod("isUsingPartyChat", Player.class);
                if (((boolean) isUsingAdminChat.invoke(null, player)) || ((boolean) isUsingPartyChat.invoke(null, player))) return;
            } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // return if event canceled
        if (getConfig().getBoolean("DontSendCanceledChatEvents") && cancelled) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord because the chat event was canceled and DontSendCanceledChatEvents is true");
            return;
        }

        // return if should not send in-game chat
        if (!getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord because DiscordChatChannelMinecraftToDiscord is false");
            return;
        }

        // return if user is unsubscribed from Discord and config says don't send those peoples' messages
        if (getUnsubscribedPlayers().contains(player.getUniqueId()) && !getConfig().getBoolean("MinecraftUnsubscribedMessageForwarding")) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord because the user is unsubscribed to Discord and MinecraftUnsubscribedMessageForwarding is false");
            return;
        }

        // return if doesn't match prefix filter
        if (!message.startsWith(getConfig().getString("DiscordChatChannelPrefix"))) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord because the message didn't start with \"" + getConfig().getString("DiscordChatChannelPrefix") + "\" (DiscordChatChannelPrefix)");
            return;
        }

        GameChatMessagePreProcessEvent preEvent = (GameChatMessagePreProcessEvent) api.callEvent(new GameChatMessagePreProcessEvent(channel, message, player));
        channel = preEvent.getChannel(); // update channel from event in case any listeners modified it
        message = preEvent.getMessage(); // update message from event in case any listeners modified it

        String userPrimaryGroup = VaultHook.getPrimaryGroup(player);
        boolean hasGoodGroup = !"".equals(userPrimaryGroup.replace(" ", ""));

        String format = hasGoodGroup
                ? LangUtil.Message.CHAT_TO_DISCORD.toString()
                : LangUtil.Message.CHAT_TO_DISCORD_NO_PRIMARY_GROUP.toString();
        String discordMessage = format
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%message%", DiscordUtil.stripColor(message))
                .replace("%primarygroup%", VaultHook.getPrimaryGroup(player))
                .replace("%displayname%", DiscordUtil.stripColor(DiscordUtil.escapeMarkdown(player.getDisplayName())))
                .replace("%username%", DiscordUtil.stripColor(DiscordUtil.escapeMarkdown(player.getName())))
                .replace("%world%", player.getWorld().getName())
                .replace("%worldalias%", DiscordUtil.stripColor(MultiverseCoreHook.getWorldAlias(player.getWorld().getName())))
        ;

        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = PlaceholderAPI.setPlaceholders(player, discordMessage);
        discordMessage = DiscordUtil.stripColor(discordMessage);
        discordMessage = DiscordUtil.convertMentionsFromNames(discordMessage, getMainGuild());

        GameChatMessagePostProcessEvent postEvent = (GameChatMessagePostProcessEvent) api.callEvent(new GameChatMessagePostProcessEvent(channel, discordMessage, player, preEvent.isCancelled()));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("GameChatMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }
        channel = postEvent.getChannel(); // update channel from event in case any listeners modified it
        discordMessage = postEvent.getProcessedMessage(); // update message from event in case any listeners modified it

        if (channel == null) DiscordUtil.sendMessage(getMainTextChannel(), discordMessage);
        else DiscordUtil.sendMessage(getDestinationTextChannelForGameChannelName(channel), discordMessage);
    }

    public void broadcastMessageToMinecraftServer(String channel, String message) {
        // apply regex to message
        if (StringUtils.isNotBlank(getConfig().getString("DiscordChatChannelRegex")))
            message = message.replaceAll(getConfig().getString("DiscordChatChannelRegex"), getConfig().getString("DiscordChatChannelRegexReplacement"));

        if (getHookedPlugins().size() == 0 || channel == null) {
            for (Player player : PlayerUtil.getOnlinePlayers()) {
                if (getUnsubscribedPlayers().contains(player.getUniqueId())) continue; // don't send this player the message if they're unsubscribed
                player.sendMessage(message);
            }
            PlayerUtil.notifyPlayersOfMentions(null, message);
            api.callEvent(new DiscordGuildMessagePostBroadcastEvent(channel, message));
        } else {
            if (getHookedPlugins().contains("herochat")) HerochatHook.broadcastMessageToChannel(channel, message);
            else if (getHookedPlugins().contains("legendchat")) LegendChatHook.broadcastMessageToChannel(channel, message);
            else if (getHookedPlugins().contains("lunachat")) LunaChatHook.broadcastMessageToChannel(channel, message);
            else if (getHookedPlugins().contains("townychat")) TownyChatHook.broadcastMessageToChannel(channel, message);
            else if (getHookedPlugins().contains("venturechat")) VentureChatHook.broadcastMessageToChannel(channel, message);
            else {
                error("Hooked plugins " + getHookedPlugins() + " are somehow in the hooked plugins list yet aren't supported.");
                broadcastMessageToMinecraftServer(null, message);
                return;
            }
            api.callEvent(new DiscordGuildMessagePostBroadcastEvent(channel, message));
        }
        DiscordSRV.getPlugin().getMetrics().increment("messages_sent_to_minecraft");
    }

    public void setIsSubscribed(UUID playerUuid, boolean subscribed) {
        if (subscribed) {
            unsubscribedPlayers.remove(playerUuid);
        } else {
            if (!unsubscribedPlayers.contains(playerUuid))
                unsubscribedPlayers.add(playerUuid);
        }
    }

}
