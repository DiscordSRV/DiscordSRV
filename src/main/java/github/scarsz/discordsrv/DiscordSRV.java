/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.scarsz.configuralize.DynamicConfig;
import github.scarsz.configuralize.Language;
import github.scarsz.configuralize.ParseException;
import github.scarsz.discordsrv.api.ApiManager;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostBroadcastEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.hooks.VaultHook;
import github.scarsz.discordsrv.hooks.chat.*;
import github.scarsz.discordsrv.hooks.world.MultiverseCoreHook;
import github.scarsz.discordsrv.listeners.*;
import github.scarsz.discordsrv.modules.requirelink.RequireLinkModule;
import github.scarsz.discordsrv.modules.voice.VoiceModule;
import github.scarsz.discordsrv.objects.CancellationDetector;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.StrippedDnsClient;
import github.scarsz.discordsrv.objects.log4j.ConsoleAppender;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.objects.managers.CommandManager;
import github.scarsz.discordsrv.objects.managers.JdbcAccountLinkManager;
import github.scarsz.discordsrv.objects.metrics.BStats;
import github.scarsz.discordsrv.objects.metrics.MCStats;
import github.scarsz.discordsrv.objects.threads.ChannelTopicUpdater;
import github.scarsz.discordsrv.objects.threads.ConsoleMessageQueueWorker;
import github.scarsz.discordsrv.objects.threads.PresenceUpdater;
import github.scarsz.discordsrv.objects.threads.ServerWatchdog;
import github.scarsz.discordsrv.util.*;
import lombok.Getter;
import me.vankka.reserializer.discord.DiscordSerializer;
import me.vankka.reserializer.minecraft.MinecraftSerializer;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.record.Record;

import javax.net.ssl.SSLContext;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "unchecked", "WeakerAccess", "ConstantConditions"})
public class DiscordSRV extends JavaPlugin implements Listener {

    public static final ApiManager api = new ApiManager();
    public static boolean isReady = false;
    public static boolean updateIsAvailable = false;
    public static String version = "";

    @Getter private AccountLinkManager accountLinkManager;
    @Getter private CancellationDetector<AsyncPlayerChatEvent> cancellationDetector = null;
    @Getter private final Map<String, String> channels = new LinkedHashMap<>(); // <in-game channel name, discord channel>
    @Getter private ChannelTopicUpdater channelTopicUpdater;
    @Getter private final Map<String, String> colors = new HashMap<>();
    @Getter private CommandManager commandManager = new CommandManager();
    @Getter private File configFile = new File(getDataFolder(), "config.yml");
    @Getter private Queue<String> consoleMessageQueue = new LinkedList<>();
    @Getter private ConsoleMessageQueueWorker consoleMessageQueueWorker;
    @Getter private File debugFolder = new File(getDataFolder(), "debug");
    @Getter private File messagesFile = new File(getDataFolder(), "messages.yml");
    @Getter private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Getter private List<String> hookedPlugins = new ArrayList<>();
    @Getter private JDA jda = null;
    @Getter private File linkedAccountsFile = new File(getDataFolder(), "linkedaccounts.json");
    @Getter private Random random = new Random();
    @Getter private List<String> randomPhrases = new ArrayList<>();
    @Getter private Map<String, String> responses = new HashMap<>();
    @Getter private ServerWatchdog serverWatchdog;
    @Getter private VoiceModule voiceModule;
    @Getter private RequireLinkModule requireLinkModule;
    @Getter private PresenceUpdater presenceUpdater;
    @Getter private long startTime = System.currentTimeMillis();
    private DynamicConfig config;
    private String consoleChannel;

    public static DiscordSRV getPlugin() {
        return getPlugin(DiscordSRV.class);
    }
    public static DynamicConfig config() {
        return getPlugin().config;
    }
    public void reloadConfig() {
        try {
            config().loadAll();
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }
    public void reloadChannels() {
        synchronized (channels) {
            channels.clear();
            config().dget("Channels").children().forEach(dynamic -> {
                this.channels.put(dynamic.key().convert().intoString(), dynamic.convert().intoString());
            });
        }
    }
    public String getMainChatChannel() {
        return channels.size() != 0 ? channels.entrySet().iterator().next().getKey() : null;
    }
    public TextChannel getMainTextChannel() {
        return channels.size() != 0 && jda != null ? jda.getTextChannelById(channels.entrySet().iterator().next().getValue()) : null;
    }
    public Guild getMainGuild() {
        if (jda == null) return null;

        return getMainTextChannel() != null
                ? getMainTextChannel().getGuild()
                : getConsoleChannel() != null
                    ? getConsoleChannel().getGuild()
                    : jda.getGuilds().size() > 0
                        ? jda.getGuilds().get(0)
                        : null;
    }
    public TextChannel getConsoleChannel() {
        return StringUtils.isNotBlank(consoleChannel) && StringUtils.isNumeric(consoleChannel)
                ? jda.getTextChannelById(consoleChannel)
                : null;
    }
    public TextChannel getDestinationTextChannelForGameChannelName(String gameChannelName) {
        Map.Entry<String, String> entry = channels.entrySet().stream().filter(e -> e.getKey().equals(gameChannelName)).findFirst().orElse(null);
        if (entry != null) return jda.getTextChannelById(entry.getValue()); // found case-sensitive channel

        // no case-sensitive channel found, try case in-sensitive
        entry = channels.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(gameChannelName)).findFirst().orElse(null);
        if (entry != null) return jda.getTextChannelById(entry.getValue()); // found case-insensitive channel

        return null; // no channel found, case-insensitive or not
    }
    public String getDestinationGameChannelNameForTextChannel(TextChannel source) {
        for (Map.Entry<String, String> channelEntry : channels.entrySet()) {
            if (channelEntry == null) continue;
            if (channelEntry.getKey() == null) continue;
            if (channelEntry.getValue() == null) continue;
            TextChannel channel = jda.getTextChannelById(channelEntry.getValue());
            if (channel != null && channel.equals(source)) return channelEntry.getKey();
        }
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
        if (DiscordSRV.config().getInt("DebugLevel") == 0) return;

        getPlugin().getLogger().info("[DEBUG] " + message + (DiscordSRV.config().getInt("DebugLevel") >= 2 ? "\n" + DebugUtil.getStackTrace() : ""));
    }

    public DiscordSRV() {
        // load config
        getDataFolder().mkdirs();
        config = new DynamicConfig();
        config.addSource(DiscordSRV.class, "config", new File(getDataFolder(), "config.yml"));
        config.addSource(DiscordSRV.class, "messages", new File(getDataFolder(), "messages.yml"));
        config.addSource(DiscordSRV.class, "voice", new File(getDataFolder(), "voice.yml"));
        config.addSource(DiscordSRV.class, "linking", new File(getDataFolder(), "linking.yml"));
        String languageCode = System.getProperty("user.language").toUpperCase();
        Language language = null;
        try {
            Language lang = Language.valueOf(languageCode);
            if (config.isLanguageAvailable(lang)) {
                language = lang;
            } else {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            String lang = language != null ? language.getName() : languageCode.toUpperCase();
            getLogger().info("Unknown user language " + lang + ".");
            getLogger().info("If you fluently speak " + lang + " as well as English, see the GitHub repo to translate it!");
        }
        if (language == null) language = Language.EN;
        config.setLanguage(language);
        try {
            config.saveAllDefaults();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save default config files", e);
        }
        try {
            config.loadAll();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }
        String forcedLanguage = config.getString("ForcedLanguage");
        if (StringUtils.isNotBlank(forcedLanguage) && !forcedLanguage.equalsIgnoreCase("none")) {
            Arrays.stream(Language.values())
                    .filter(lang -> lang.getCode().equalsIgnoreCase(forcedLanguage) ||
                            lang.name().equalsIgnoreCase(forcedLanguage)
                    )
                    .findFirst().ifPresent(lang -> config.setLanguage(lang));
        }
    }

    @Override
    public void onEnable() {
        ConfigUtil.migrate();
        DiscordSRV.debug("Language is " + config.getLanguage().getName());

        version = getDescription().getVersion();
        Thread initThread = new Thread(this::init, "DiscordSRV - Initialization");
        initThread.setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            getLogger().severe("DiscordSRV failed to load properly: " + e.getMessage() + ". See " + github.scarsz.discordsrv.util.DebugUtil.run("DiscordSRV") + " for more information.");
        });
        initThread.start();
    }

    public void init() {
        // check if the person is trying to use the plugin without updating to ASM 5
        try {
            File specialSourceFile = new File("libraries/net/md-5/SpecialSource/1.7-SNAPSHOT/SpecialSource-1.7-SNAPSHOT.jar");
            if (!specialSourceFile.exists()) specialSourceFile = new File("bin/net/md-5/SpecialSource/1.7-SNAPSHOT/SpecialSource-1.7-SNAPSHOT.jar");
            if (specialSourceFile.exists() && DigestUtils.md5Hex(FileUtils.readFileToByteArray(specialSourceFile)).equalsIgnoreCase("096777a1b6098130d6c925f1c04050a3")) {
                DiscordSRV.warning(LangUtil.InternalMessage.ASM_WARNING.toString()
                        .replace("{specialsourcefolder}", specialSourceFile.getParentFile().getPath())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // remove all event listeners from existing jda to prevent having multiple listeners when jda is recreated
        if (jda != null) jda.getRegisteredListeners().forEach(o -> jda.removeEventListener(o));

        requireLinkModule = new RequireLinkModule();

        // update check
        if (!config().getBooleanElse("UpdateCheckDisabled", false)) {
            updateIsAvailable = UpdateUtil.checkForUpdates();
            if (!isEnabled()) return;
        }

        // PebbleHost partner
        // note: I do not receive any money from Pebble regarding the usage of DiscordSRV's promo code.
        // they're just legitimately a great, transparent host and the code is there purely to help people save a little money.
        if (config().getBoolean("PartnerPebbleHost") &&
                System.getenv("IGetItBroIDontNeedANewHost") == null &&
                System.getProperty("IGetItBroIDontNeedANewHost") == null) {
            for (String s : LangUtil.InternalMessage.PARTNER_PEBBLE.toString().split("\n")) {
                ChatColor color = s.startsWith("=") ? ChatColor.DARK_GRAY : ChatColor.GREEN;
                getLogger().info(color + s);
            }
        }

        // random phrases for debug handler
        if (config().getBooleanElse("RandomPhrasesDisabled", false)) {
            Collections.addAll(randomPhrases, HttpUtil.requestHttp("https://raw.githubusercontent.com/DiscordSRV/DiscordSRV/randomaccessfiles/randomphrases").split("\n"));
        }

        // shutdown previously existing jda if plugin gets reloaded
        if (jda != null) try { jda.shutdown(); jda = null; } catch (Exception e) { e.printStackTrace(); }

        // set proxy just in case this JVM doesn't have a proxy selector for some reason
        if (ProxySelector.getDefault() == null) {
            ProxySelector.setDefault(new ProxySelector() {
                private final List<Proxy> DIRECT_CONNECTION = Collections.unmodifiableList(Collections.singletonList(Proxy.NO_PROXY));
                public void connectFailed(URI arg0, SocketAddress arg1, IOException arg2) {}
                public List<Proxy> select(URI uri) { return DIRECT_CONNECTION; }
            });
        }

        // set ssl to TLSv1.2
        if (config().getBoolean("ForceTLSv12")) {
            try {
                SSLContext context = SSLContext.getInstance("TLSv1.2");
                context.init(null, null, null);
                SSLContext.setDefault(context);
            } catch (Exception ignored) {}
        }

        // check log4j capabilities
        boolean serverIsLog4jCapable = false;
        boolean serverIsLog4j21Capable = false;
        try {
            serverIsLog4jCapable = Class.forName("org.apache.logging.log4j.core.Logger") != null;
        } catch (ClassNotFoundException e) {
            error("Log4j classes are NOT available, console channel will not be attached");
        }
        try {
            serverIsLog4j21Capable = Class.forName("org.apache.logging.log4j.core.Filter") != null;
        } catch (ClassNotFoundException e) {
            error("Log4j 2.1 classes are NOT available, JDA messages will NOT be formatted properly");
        }

        // add log4j filter for JDA messages
        if (serverIsLog4j21Capable) {
            try {
                Class jdaFilterClass = Class.forName("github.scarsz.discordsrv.objects.log4j.JdaFilter");
                Object jdaFilter = jdaFilterClass.newInstance();
                ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger()).addFilter((org.apache.logging.log4j.core.Filter) jdaFilter);
            } catch (Exception e) {
                DiscordSRV.error("Failed to attach JDA message filter to root logger: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (config().getBoolean("DebugJDA")) {
            LoggerContext config = ((LoggerContext) LogManager.getContext(false));
            config.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(Level.ALL);
            config.updateLoggers();
        }

        if (config().getBoolean("DebugJDARestActionStacks")) {
            RestAction.setPassContext(true);
        }

        // http client for JDA
        Dns dns = Dns.SYSTEM;
        try {
            List<InetAddress> fallbackDnsServers = new LinkedList<>();
            // CloudFlare resolvers
            fallbackDnsServers.add(InetAddress.getByName("1.1.1.1"));
            fallbackDnsServers.add(InetAddress.getByName("1.0.0.1"));
            // Google resolvers
            fallbackDnsServers.add(InetAddress.getByName("8.8.8.8"));
            fallbackDnsServers.add(InetAddress.getByName("8.8.4.4"));

            dns = new Dns() {
                // maybe drop minidns in favor of something else
                // https://github.com/dnsjava/dnsjava/blob/master/src/main/java/org/xbill/DNS/SimpleResolver.java
                // https://satreth.blogspot.com/2015/01/java-dns-query.html

                private StrippedDnsClient client = new StrippedDnsClient();
                private boolean dnsSucks = false;
                @NotNull @Override
                public List<InetAddress> lookup(@NotNull String host) throws UnknownHostException {
                    if (!dnsSucks) {
                        try {
                            return Dns.SYSTEM.lookup(host);
                        } catch (Exception e) {
                            dnsSucks = true;
                            DiscordSRV.error("System DNS FAILED to resolve hostname " + host + ", using fallback DNS servers!");
                        }
                    }
                    return lookupPublic(host);
                }
                private List<InetAddress> lookupPublic(String host) throws UnknownHostException {
                    for (InetAddress dnsServer : fallbackDnsServers) {
                        try {
                            DnsMessage query = client.query(host, Record.TYPE.A, Record.CLASS.IN, dnsServer);
                            List<InetAddress> resolved = query.answerSection.stream()
                                    .map(record -> record.payloadData.toString())
                                    .map(s -> {
                                        try {
                                            return InetAddress.getByName(s);
                                        } catch (UnknownHostException e) {
                                            // impossible
                                            e.printStackTrace();
                                            return null;
                                        }
                                    })
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .collect(Collectors.toList());
                            if (resolved.size() > 0) {
                                return resolved;
                            } else {
                                DiscordSRV.error("DNS server " + dnsServer.getHostAddress() + " failed to resolve " + host + ": no results");
                            }
                        } catch (Exception ex) {
                            DiscordSRV.error("DNS server " + dnsServer.getHostAddress() + " failed to resolve " + host + ": " + ex.getMessage());
                        }

                        // this dns server gave us an error so we move this dns server to the end of the
                        // list, effectively making it the last resort for future requests
                        fallbackDnsServers.remove(dnsServer);
                        fallbackDnsServers.add(dnsServer);
                    }

                    // this sleep is here to prevent OkHTTP from repeatedly trying to query DNS servers with no
                    // delay of it's own when internet connectivity is lost. that's extremely bad because it'll be
                    // spitting errors into the console and consuming 100% cpu
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    UnknownHostException exception = new UnknownHostException("All DNS resolvers failed to resolve hostname " + host + ". Not good.");
                    exception.setStackTrace(new StackTraceElement[]{exception.getStackTrace()[0]});
                    throw exception;
                }
            };
        } catch (Exception e) {
            DiscordSRV.error("Failed to make custom DNS client: " + e.getMessage());
        }

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .dns(dns)
                // more lenient timeouts (normally 10 seconds for these 3)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        // log in to discord
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .setHttpClient(httpClient)
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .setToken(config().getString("BotToken").trim())
                    .addEventListeners(new DiscordBanListener())
                    .addEventListeners(new DiscordChatListener())
                    .addEventListeners(new DiscordConsoleListener())
                    .addEventListeners(new DiscordAccountLinkListener())
                    .setContextEnabled(false)
                    .build().awaitReady();
        } catch (LoginException e) {
            DiscordSRV.error(LangUtil.InternalMessage.FAILED_TO_CONNECT_TO_DISCORD + ": " + e.getMessage());
            return;
        } catch (Exception e) {
            DiscordSRV.error("An unknown error occurred building JDA...");
            e.printStackTrace();
            return;
        }

        // start presence updater thread
        if (presenceUpdater != null) {
            if (presenceUpdater.getState() != Thread.State.NEW) {
                presenceUpdater.interrupt();
                presenceUpdater = new PresenceUpdater();
            }
            Bukkit.getScheduler().runTaskLater(this, () -> presenceUpdater.start(), 5 * 20);
        } else {
            presenceUpdater = new PresenceUpdater();
            presenceUpdater.start();
        }

        // print the things the bot can see
        if (config().getBoolean("PrintGuildsAndChannels")) {
            for (Guild server : jda.getGuilds()) {
                DiscordSRV.info(LangUtil.InternalMessage.FOUND_SERVER + " " + server);
                for (TextChannel channel : server.getTextChannels()) DiscordSRV.info("- " + channel);
            }
        }

        // show warning if bot wasn't in any guilds
        if (jda.getGuilds().size() == 0) {
            DiscordSRV.error(LangUtil.InternalMessage.BOT_NOT_IN_ANY_SERVERS);
            return;
        }

        // set console channel
        String consoleChannelId = config().getString("DiscordConsoleChannelId");
        if (consoleChannelId != null) consoleChannel = consoleChannelId;

        // see if console channel exists; if it does, tell user where it's been assigned & add console appender
        if (serverIsLog4jCapable && StringUtils.isNotBlank(consoleChannel)) {
            DiscordSRV.info(LangUtil.InternalMessage.CONSOLE_FORWARDING_ASSIGNED_TO_CHANNEL + " " + consoleChannel);

            // attach appender to queue console messages
            new ConsoleAppender();

            // start console message queue worker thread
            if (consoleMessageQueueWorker != null) {
                if (consoleMessageQueueWorker.getState() != Thread.State.NEW) {
                    consoleMessageQueueWorker.interrupt();
                    consoleMessageQueueWorker = new ConsoleMessageQueueWorker();
                }
            } else {
                consoleMessageQueueWorker = new ConsoleMessageQueueWorker();
            }
            consoleMessageQueueWorker.start();
        } else {
            DiscordSRV.info(LangUtil.InternalMessage.NOT_FORWARDING_CONSOLE_OUTPUT.toString());
        }

        reloadChannels();

        // warn if no channels have been linked
        if (getMainTextChannel() == null) DiscordSRV.warning(LangUtil.InternalMessage.NO_CHANNELS_LINKED);
        if (getMainTextChannel() == null && StringUtils.isBlank(consoleChannel)) DiscordSRV.error(LangUtil.InternalMessage.NO_CHANNELS_LINKED_NOR_CONSOLE);
        // warn if the console channel is connected to a chat channel
        if (getMainTextChannel() != null && StringUtils.isNotBlank(consoleChannel) && getMainTextChannel().getId().equals(consoleChannel)) DiscordSRV.warning(LangUtil.InternalMessage.CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL);

        // send server startup message
        DiscordUtil.sendMessage(getMainTextChannel(), LangUtil.Message.SERVER_STARTUP_MESSAGE.toString(), 0, false);

        // extra enabled check before doing bukkit api stuff
        if (!isEnabled()) return;

        // start server watchdog
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
        reloadCancellationDetector();

        // load account links
        if (JdbcAccountLinkManager.shouldUseJdbc()) {
            try {
                accountLinkManager = new JdbcAccountLinkManager();
            } catch (SQLException e) {
                DiscordSRV.warning("JDBC account link backend failed to initialize: " + e.getMessage() + "\n" + DebugUtil.getStackTrace());
                DiscordSRV.warning("Account link manager falling back to flat file");
                accountLinkManager = new AccountLinkManager();
            }
        } else {
            accountLinkManager = new AccountLinkManager();
        }

        // register events
        Bukkit.getPluginManager().registerEvents(this, this);
        new PlayerBanListener();
        new PlayerDeathListener();
        new PlayerJoinLeaveListener();
        try {
            Class<?> c = Class.forName("org.bukkit.event.player.PlayerAchievementAwardedEvent");
            if (c.isAnnotationPresent(Deprecated.class)) throw new ClassNotFoundException();
            new PlayerAchievementsListener();
        } catch (Exception ignored) {
            new PlayerAdvancementDoneListener();
        }

        // in-game chat events
        if (PluginUtil.pluginHookIsEnabled("herochat")) {
            DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "HeroChat"));
            getServer().getPluginManager().registerEvents(new HerochatHook(), this);
        } else if (PluginUtil.pluginHookIsEnabled("legendchat")) {
            DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "LegendChat"));
            getServer().getPluginManager().registerEvents(new LegendChatHook(), this);
        } else if (PluginUtil.pluginHookIsEnabled("lunachat")) {
            DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "LunaChat"));
            getServer().getPluginManager().registerEvents(new LunaChatHook(), this);
        } else if (PluginUtil.checkIfPluginEnabled("towny") && PluginUtil.pluginHookIsEnabled("townychat")) {
            DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "TownyChat"));
            getServer().getPluginManager().registerEvents(new TownyChatHook(), this);
        } else if (PluginUtil.pluginHookIsEnabled("ultimatechat", false)) {
            DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "UltimateChat"));
            getServer().getPluginManager().registerEvents(new UltimateChatHook(), this);
        } else if (PluginUtil.pluginHookIsEnabled("venturechat")) {
            DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "VentureChat"));
            getServer().getPluginManager().registerEvents(new VentureChatHook(), this);
        } else if (PluginUtil.pluginHookIsEnabled("evernifefancychat")) {
            DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "EverNifeFancyChat"));
            getServer().getPluginManager().registerEvents(new FancyChatHook(), this);
        } else {
            DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOKS_NOT_ENABLED);
            getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        }

        // load user-defined colors
        reloadColors();

        // load canned responses
        responses.clear();
        config().dget("DiscordCannedResponses").children().forEach(dynamic -> {
            responses.put(dynamic.key().convert().intoString(), dynamic.convert().intoString());
        });

        // start channel topic updater
        if (channelTopicUpdater != null) {
            if (channelTopicUpdater.getState() != Thread.State.NEW) {
                channelTopicUpdater.interrupt();
                channelTopicUpdater = new ChannelTopicUpdater();
            }
        } else {
            channelTopicUpdater = new ChannelTopicUpdater();
        }
        channelTopicUpdater.start();

        // enable metrics
        if (!config().getBooleanElse("MetricsDisabled", false)) {
            try {
                MCStats MCStats = new MCStats(this);
                MCStats.start();
            } catch (IOException e) {
                DiscordSRV.warning("Unable to start metrics: " + e.getMessage());
            }

            BStats bStats = new BStats(this);
            bStats.addCustomChart(new BStats.SimplePie("linked_channels", () -> String.valueOf(channels.size())));
            bStats.addCustomChart(new BStats.AdvancedPie("hooked_plugins", () -> new HashMap<String, Integer>(){{
                if (hookedPlugins.size() == 0) {
                    put("none", 1);
                } else {
                    for (String hookedPlugin : hookedPlugins) {
                        put(hookedPlugin.toLowerCase(), 1);
                    }
                }
            }}));
            bStats.addCustomChart(new BStats.SingleLineChart("minecraft-discord_account_links", () -> accountLinkManager.getLinkedAccounts().size()));
            bStats.addCustomChart(new BStats.SimplePie("server_language", () -> DiscordSRV.config().getLanguage().getName()));
            bStats.addCustomChart(new BStats.AdvancedPie("features", () -> new HashMap<String, Integer>() {{
                if (getConsoleChannel() != null) put("Console channel", 1);
                if (StringUtils.isNotBlank(config().getString("DiscordChatChannelPrefix"))) put("Chatting prefix", 1);
                if (JdbcAccountLinkManager.shouldUseJdbc(true)) put("JDBC", 1);
                if (config().getBoolean("Experiment_MCDiscordReserializer")) put("MC <-> Discord Reserializer", 1);
                if (config().getBoolean("Experiment_WebhookChatMessageDelivery")) put("Webhooks", 1);
                if (config().getBoolean("DiscordChatChannelTranslateMentions")) put("Mentions", 1);
                if (config().getStringList("GroupRoleSynchronizationRoleIdsToSync").stream().anyMatch(s -> s.replace("0", "").length() > 0)) put("Group -> role synchronization", 1);
                if (config().getBoolean("Voice enabled")) put("Voice", 1);
                if (config().getBoolean("Require linked account to play.Enabled")) {
                    put("Require linked account to play", 1);
                    if (config().getBoolean("Require linked account to play.Subscriber role.Require subscriber role to join")) {
                        put("Required subscriber role to play", 1);
                    }
                }
            }}));
        }

        // metrics file deprecated since v1.18.1
        File metricsFile = new File(getDataFolder(), "metrics.json");
        if (metricsFile.exists() && !metricsFile.delete()) metricsFile.deleteOnExit();

        // dummy sync target to initialize class
        GroupSynchronizationUtil.reSyncGroups(null);

        voiceModule = new VoiceModule();

        if (getCommand("discord").getPlugin() != this) {
            DiscordSRV.warning("/discord command is being handled by plugin other than DiscordSRV. You must use /discordsrv instead.");
        }

        // set ready status
        if (jda.getStatus() == JDA.Status.CONNECTED) isReady = true;
    }

    @Override
    public void onDisable() {
        long shutdownStartTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.invokeAll(Collections.singletonList(() -> {
                // set server shutdown topics if enabled
                if (config().getBoolean("ChannelTopicUpdaterChannelTopicsAtShutdownEnabled")) {
                    DiscordUtil.setTextChannelTopic(
                            getMainTextChannel(),
                            LangUtil.Message.CHAT_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN.toString()
                                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                                    .replace("%serverversion%", Bukkit.getBukkitVersion())
                                    .replace("%totalplayers%", Integer.toString(getTotalPlayerCount()))
                    );
                    DiscordUtil.setTextChannelTopic(
                            getConsoleChannel(),
                            LangUtil.Message.CONSOLE_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN.toString()
                                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                                    .replace("%serverversion%", Bukkit.getBukkitVersion())
                                    .replace("%totalplayers%", Integer.toString(getTotalPlayerCount()))
                    );
                }

                // shut down voice module
                if (voiceModule != null) voiceModule.shutdown();

                // kill channel topic updater
                if (channelTopicUpdater != null) channelTopicUpdater.interrupt();

                // kill console message queue worker
                if (consoleMessageQueueWorker != null) consoleMessageQueueWorker.interrupt();

                // kill presence updater
                if (presenceUpdater != null) presenceUpdater.interrupt();

                // serialize account links to disk
                if (accountLinkManager != null) accountLinkManager.save();

                // close cancellation detector
                if (cancellationDetector != null) cancellationDetector.close();

                // send server shutdown message
                DiscordUtil.sendMessageBlocking(getMainTextChannel(), LangUtil.Message.SERVER_SHUTDOWN_MESSAGE.toString());

                // try to shut down jda gracefully
                if (jda != null) {
                    CompletableFuture<Void> shutdownTask = new CompletableFuture<>();
                    jda.addEventListener(new ListenerAdapter() {
                        @Override
                        public void onShutdown(@NotNull ShutdownEvent event) {
                            shutdownTask.complete(null);
                        }
                    });
                    jda.shutdown();
                    try {
                        shutdownTask.get(5, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        getLogger().warning("JDA took too long to shut down, skipping");
                    }
                }

                DiscordSRV.info(LangUtil.InternalMessage.SHUTDOWN_COMPLETED.toString()
                        .replace("{ms}", String.valueOf(System.currentTimeMillis() - shutdownStartTime))
                );

                return null;
            }), 15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executor.shutdownNow();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            return commandManager.handle(sender, null, new String[] {});
        } else {
            return commandManager.handle(sender, args[0], Arrays.stream(args).skip(1).toArray(String[]::new));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command bukkitCommand, @NotNull String alias, String[] args) {
        String command = args[0];
        String[] commandArgs = Arrays.stream(args).skip(1).toArray(String[]::new);

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

    public void reloadColors() {
        synchronized (colors) {
            colors.clear();
            config().dget("DiscordChatChannelColorTranslations").children().forEach(dynamic -> {
                colors.put(dynamic.key().convert().intoString().toUpperCase(), dynamic.convert().intoString());
            });
        }
    }

    public void reloadCancellationDetector() {
        if (cancellationDetector != null) {
            cancellationDetector.close();
            cancellationDetector = null;
        }

        if (config().getInt("DebugLevel") > 0) {
            cancellationDetector = new CancellationDetector<>(AsyncPlayerChatEvent.class);
            cancellationDetector.addListener((plugin, event) -> DiscordSRV.info("Plugin " + plugin.toString()
                    + " cancelled AsyncPlayerChatEvent (author: " + event.getPlayer().getName()
                    + " | message: " + event.getMessage() + ")"));
            DiscordSRV.info(LangUtil.InternalMessage.CHAT_CANCELLATION_DETECTOR_ENABLED);
        }
    }

    public void processChatMessage(Player player, String message, String channel, boolean cancelled) {
        // log debug message to notify that a chat message was being processed
        debug("Chat message received, canceled: " + cancelled);

        if (player == null) {
            debug("Received chat message was from a null sender, not processing message");
            return;
        }

        // return if player doesn't have permission
        if (!player.hasPermission("discordsrv.chat")) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord due to lack of permission");
            return;
        }

        // return if mcMMO is enabled and message is from party or admin chat
        if (PluginUtil.pluginHookIsEnabled("mcMMO", false)) {
            if (player.hasMetadata("mcMMO: Player Data")) {
                boolean usingAdminChat = com.gmail.nossr50.api.ChatAPI.isUsingAdminChat(player);
                boolean usingPartyChat = com.gmail.nossr50.api.ChatAPI.isUsingPartyChat(player);
                if (usingAdminChat || usingPartyChat) {
                    debug("Not processing message because message was from " + (usingAdminChat ? "admin" : "party") + " chat");
                    return;
                }
            }
        }

        // return if event canceled
        if (config().getBoolean("RespectChatPlugins") && cancelled) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord because the chat event was canceled");
            return;
        }

        // return if should not send in-game chat
        if (!config().getBoolean("DiscordChatChannelMinecraftToDiscord")) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord because DiscordChatChannelMinecraftToDiscord is false");
            return;
        }

        // return if doesn't match prefix filter
        if (!DiscordUtil.strip(message).startsWith(config().getString("DiscordChatChannelPrefix"))) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord because the message didn't start with \"" + config().getString("DiscordChatChannelPrefix") + "\" (DiscordChatChannelPrefix): \"" + message + "\"");
            return;
        }

        GameChatMessagePreProcessEvent preEvent = (GameChatMessagePreProcessEvent) api.callEvent(new GameChatMessagePreProcessEvent(channel, message, player));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug("GameChatMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }
        channel = preEvent.getChannel(); // update channel from event in case any listeners modified it
        message = preEvent.getMessage(); // update message from event in case any listeners modified it

        String userPrimaryGroup = VaultHook.getPrimaryGroup(player);
        boolean hasGoodGroup = StringUtils.isNotBlank(userPrimaryGroup);

        // capitalize the first letter of the user's primary group to look neater
        if (hasGoodGroup) userPrimaryGroup = userPrimaryGroup.substring(0, 1).toUpperCase() + userPrimaryGroup.substring(1);

        boolean reserializer = DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer");

        String username = DiscordUtil.strip(player.getName());
        if (!reserializer) username = DiscordUtil.escapeMarkdown(username);

        String discordMessage = (hasGoodGroup
                ? LangUtil.Message.CHAT_TO_DISCORD.toString()
                : LangUtil.Message.CHAT_TO_DISCORD_NO_PRIMARY_GROUP.toString())
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%channelname%", channel != null ? channel.substring(0, 1).toUpperCase() + channel.substring(1) : "")
                .replace("%primarygroup%", userPrimaryGroup)
                .replace("%username%", username)
                .replace("%world%", player.getWorld().getName())
                .replace("%worldalias%", DiscordUtil.strip(MultiverseCoreHook.getWorldAlias(player.getWorld().getName())))
        ;
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, discordMessage);

        String displayName = DiscordUtil.strip(player.getDisplayName());
        if (!reserializer) displayName = DiscordUtil.escapeMarkdown(displayName);

        discordMessage = discordMessage
                .replace("%displayname%", displayName)
                .replace("%message%", message);

        if (!reserializer) discordMessage = DiscordUtil.strip(discordMessage);

        if (config().getBoolean("DiscordChatChannelTranslateMentions")) {
            discordMessage = DiscordUtil.convertMentionsFromNames(discordMessage, getMainGuild());
        } else {
            discordMessage = discordMessage.replace("@", "@\u200B"); // zero-width space
            message = message.replace("@", "@\u200B"); // zero-width space
        }

        GameChatMessagePostProcessEvent postEvent = (GameChatMessagePostProcessEvent) api.callEvent(new GameChatMessagePostProcessEvent(channel, discordMessage, player, preEvent.isCancelled()));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("GameChatMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }
        channel = postEvent.getChannel(); // update channel from event in case any listeners modified it
        discordMessage = postEvent.getProcessedMessage(); // update message from event in case any listeners modified it

        if (reserializer) discordMessage = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.INSTANCE.deserialize(discordMessage));

        if (!config().getBoolean("Experiment_WebhookChatMessageDelivery")) {
            if (channel == null) {
                DiscordUtil.sendMessage(getMainTextChannel(), discordMessage);
            } else {
                DiscordUtil.sendMessage(getDestinationTextChannelForGameChannelName(channel), discordMessage);
            }
        } else {
            if (channel == null) channel = getMainChatChannel();

            TextChannel destinationChannel = getDestinationTextChannelForGameChannelName(channel);

            if (destinationChannel == null) {
                DiscordSRV.debug("Failed to find Discord channel to forward message from game channel " + channel);
                return;
            }

            if (!DiscordUtil.checkPermission(destinationChannel.getGuild(), Permission.MANAGE_WEBHOOKS)) {
                DiscordSRV.error("Couldn't deliver chat message as webhook because the bot lacks the \"Manage Webhooks\" permission.");
                return;
            }

            if (PluginUtil.pluginHookIsEnabled("placeholderapi")) message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
            if (!reserializer) {
                message = DiscordUtil.strip(message);
            } else {
                message = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.INSTANCE.deserialize(message));
            }
            if (config().getBoolean("DiscordChatChannelTranslateMentions")) message = DiscordUtil.convertMentionsFromNames(message, getMainGuild());

            WebhookUtil.deliverMessage(destinationChannel, player, message);
        }
    }

    public void broadcastMessageToMinecraftServer(String channel, String message, User author) {
        // apply regex to message
        if (StringUtils.isNotBlank(config().getString("DiscordChatChannelRegex")))
            message = message.replaceAll(config().getString("DiscordChatChannelRegex"), config().getString("DiscordChatChannelRegexReplacement"));

        // apply placeholder API values
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) {
            Player authorPlayer = null;
            UUID authorLinkedUuid = accountLinkManager.getUuid(author.getId());
            if (authorLinkedUuid != null) authorPlayer = Bukkit.getPlayer(authorLinkedUuid);
            if (authorPlayer != null) {
                message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(authorPlayer, message);
            } else {
                message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, message);
            }
        }

        if (getHookedPlugins().size() == 0 || channel == null) {
            if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer")) {
                TextComponent textComponent = MinecraftSerializer.INSTANCE.serialize(message);
                for (Player player : PlayerUtil.getOnlinePlayers()) TextAdapter.sendComponent(player, textComponent);
            } else {
                for (Player player : PlayerUtil.getOnlinePlayers()) player.sendMessage(message);
            }

            PlayerUtil.notifyPlayersOfMentions(null, message);
        } else {
            if (PluginUtil.pluginHookIsEnabled("herochat")) HerochatHook.broadcastMessageToChannel(channel, message);
            else if (PluginUtil.pluginHookIsEnabled("legendchat")) LegendChatHook.broadcastMessageToChannel(channel, message);
            else if (PluginUtil.pluginHookIsEnabled("lunachat")) LunaChatHook.broadcastMessageToChannel(channel, message);
            else if (PluginUtil.pluginHookIsEnabled("townychat")) TownyChatHook.broadcastMessageToChannel(channel, message);
            else if (PluginUtil.pluginHookIsEnabled("ultimatechat", false)) UltimateChatHook.broadcastMessageToChannel(channel, message);
            else if (PluginUtil.pluginHookIsEnabled("venturechat")) VentureChatHook.broadcastMessageToChannel(channel, message);
            else if (PluginUtil.pluginHookIsEnabled("evernifefancychat")) FancyChatHook.broadcastMessageToChannel(channel, message);
            else {
                broadcastMessageToMinecraftServer(null, message, author);
                return;
            }
        }
        api.callEvent(new DiscordGuildMessagePostBroadcastEvent(channel, message));
    }

    private static File playerDataFolder = null;
    public static int getTotalPlayerCount() {
        if (playerDataFolder == null && Bukkit.getWorlds().size() > 0) {
            playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder().getAbsolutePath(), "/playerdata");
        }

        File[] playerFiles = playerDataFolder.listFiles(f -> f.getName().endsWith(".dat"));
        return playerFiles != null ? playerFiles.length : 0;
    }

}
