/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.DualStackMode;
import com.neovisionaries.ws.client.WebSocketFactory;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import github.scarsz.configuralize.DynamicConfig;
import github.scarsz.configuralize.Language;
import github.scarsz.configuralize.ParseException;
import github.scarsz.discordsrv.api.ApiManager;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostBroadcastEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.hooks.VaultHook;
import github.scarsz.discordsrv.hooks.chat.ChatHook;
import github.scarsz.discordsrv.hooks.world.MultiverseCoreHook;
import github.scarsz.discordsrv.listeners.*;
import github.scarsz.discordsrv.modules.requirelink.RequireLinkModule;
import github.scarsz.discordsrv.modules.voice.VoiceModule;
import github.scarsz.discordsrv.objects.CancellationDetector;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.objects.StrippedDnsClient;
import github.scarsz.discordsrv.objects.log4j.ConsoleAppender;
import github.scarsz.discordsrv.objects.log4j.JdaFilter;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.objects.managers.CommandManager;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import github.scarsz.discordsrv.objects.managers.JdbcAccountLinkManager;
import github.scarsz.discordsrv.objects.metrics.BStats;
import github.scarsz.discordsrv.objects.metrics.MCStats;
import github.scarsz.discordsrv.objects.threads.*;
import github.scarsz.discordsrv.util.*;
import lombok.Getter;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.utils.IOUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.record.Record;

import javax.net.ssl.SSLContext;
import javax.security.auth.login.LoginException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "WeakerAccess", "ConstantConditions"})
public class DiscordSRV extends JavaPlugin implements Listener {

    public static final ApiManager api = new ApiManager();
    public static boolean isReady = false;
    public static boolean updateIsAvailable = false;
    public static boolean updateChecked = false;
    public static String version = "";

    @Getter private AccountLinkManager accountLinkManager;
    @Getter private CancellationDetector<AsyncPlayerChatEvent> cancellationDetector = null;
    @Getter private final Map<String, String> channels = new LinkedHashMap<>(); // <in-game channel name, discord channel>
    @Getter private ChannelTopicUpdater channelTopicUpdater;
    @Getter private final Map<String, String> colors = new HashMap<>();
    @Getter private CommandManager commandManager = new CommandManager();
    @Getter private Queue<String> consoleMessageQueue = new LinkedList<>();
    @Getter private ConsoleMessageQueueWorker consoleMessageQueueWorker;
    @Getter private ScheduledExecutorService updateChecker = null;
    @Getter private ConsoleAppender consoleAppender;
    @Getter private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Getter private GroupSynchronizationManager groupSynchronizationManager = new GroupSynchronizationManager();
    @Getter private JDA jda = null;
    @Getter private Random random = new Random();
    @Getter private ServerWatchdog serverWatchdog;
    @Getter private VoiceModule voiceModule;
    @Getter private RequireLinkModule requireLinkModule;
    @Getter private PresenceUpdater presenceUpdater;
    @Getter private NicknameUpdater nicknameUpdater;
    @Getter private Set<PluginHook> pluginHooks = new HashSet<>();
    @Getter private long startTime = System.currentTimeMillis();
    @Getter private File configFile = new File(getDataFolder(), "config.yml");
    @Getter private File messagesFile = new File(getDataFolder(), "messages.yml");
    @Getter private File voiceFile = new File(getDataFolder(), "voice.yml");
    @Getter private File linkingFile = new File(getDataFolder(), "linking.yml");
    @Getter private File synchronizationFile = new File(getDataFolder(), "synchronization.yml");
    @Getter private File debugFolder = new File(getDataFolder(), "debug");
    @Getter private File logFolder = new File(getDataFolder(), "discord-console-logs");
    @Getter private File linkedAccountsFile = new File(getDataFolder(), "linkedaccounts.json");
    private ExecutorService callbackThreadPool;
    private JdaFilter jdaFilter;
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
            config().dget("Channels").children().forEach(dynamic ->
                    this.channels.put(dynamic.key().convert().intoString(), dynamic.convert().intoString()));
        }
    }
    public String getMainChatChannel() {
        return channels.size() != 0 ? channels.keySet().iterator().next() : null;
    }
    public TextChannel getMainTextChannel() {
        if (channels.isEmpty() || jda == null) return null;
        String firstChannel = channels.values().iterator().next();
        if (StringUtils.isBlank(firstChannel)) return null;
        return jda.getTextChannelById(firstChannel);
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
    public File getLogFile() {
        String fileName = config().getString("DiscordConsoleChannelUsageLog");
        if (StringUtils.isBlank(fileName)) return null;
        fileName = fileName.replace("%date%", TimeUtil.date());
        return new File(this.getLogFolder(), fileName);
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

    @SuppressWarnings("unchecked")
    public DiscordSRV() {
        // load config
        getDataFolder().mkdirs();
        config = new DynamicConfig();
        config.addSource(DiscordSRV.class, "config", getConfigFile());
        config.addSource(DiscordSRV.class, "messages", getMessagesFile());
        config.addSource(DiscordSRV.class, "voice", getVoiceFile());
        config.addSource(DiscordSRV.class, "linking", getLinkingFile());
        config.addSource(DiscordSRV.class, "synchronization", getSynchronizationFile());
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

        // Make discordsrv.sync.x & discordsrv.sync.deny.x permissions denied by default
        try {
            PluginDescriptionFile description = getDescription();
            Class<?> descriptionClass = description.getClass();

            List<org.bukkit.permissions.Permission> permissions = new ArrayList<>(description.getPermissions());
            for (String s : getGroupSynchronizables().keySet()) {
                permissions.add(new org.bukkit.permissions.Permission("discordsrv.sync." + s, null, PermissionDefault.FALSE));
                permissions.add(new org.bukkit.permissions.Permission("discordsrv.sync.deny." + s, null, PermissionDefault.FALSE));
            }

            Field permissionsField = descriptionClass.getDeclaredField("permissions");
            permissionsField.setAccessible(true);
            permissionsField.set(description, ImmutableList.copyOf(permissions));

            Class<?> pluginClass = getClass().getSuperclass();
            Field descriptionField = pluginClass.getDeclaredField("description");
            descriptionField.setAccessible(true);
            descriptionField.set(this, description);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        ConfigUtil.migrate();
        ConfigUtil.logMissingOptions();
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

        requireLinkModule = new RequireLinkModule();

        // start the update checker (will skip if disabled)
        if (!isUpdateCheckDisabled()) {
            if (updateChecker == null) {
                final ThreadFactory gatewayThreadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordSRV - Update Checker").build();
                updateChecker = Executors.newScheduledThreadPool(1);
            }
            updateChecker.scheduleAtFixedRate(() -> {
                DiscordSRV.updateIsAvailable = UpdateUtil.checkForUpdates();
                DiscordSRV.updateChecked = true;
            }, 0, 6, TimeUnit.HOURS);
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
        if (serverIsLog4j21Capable && jdaFilter == null) {
            try {
                Class<?> jdaFilterClass = Class.forName("github.scarsz.discordsrv.objects.log4j.JdaFilter");
                jdaFilter = (JdaFilter) jdaFilterClass.newInstance();
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

        if (config().getBoolean("DebugJDARestActions")) {
            RestAction.setPassContext(true);
        }

        // http client for JDA
        Dns dns = Dns.SYSTEM;
        try {
            List<InetAddress> fallbackDnsServers = new CopyOnWriteArrayList<>(Arrays.asList(
                    // CloudFlare resolvers
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    // Google resolvers
                    InetAddress.getByName("8.8.8.8"),
                    InetAddress.getByName("8.8.4.4")
            ));

            dns = new Dns() {
                // maybe drop minidns in favor of something else
                // https://github.com/dnsjava/dnsjava/blob/master/src/main/java/org/xbill/DNS/SimpleResolver.java
                // https://satreth.blogspot.com/2015/01/java-dns-query.html

                private StrippedDnsClient client = new StrippedDnsClient();
                private int failedRequests = 0;
                @NotNull @Override
                public List<InetAddress> lookup(@NotNull String host) throws UnknownHostException {
                    int max = config.getInt("MaximumAttemptsForSystemDNSBeforeUsingFallbackDNS");
                    //  0 = everything falls back (would only be useful when the system dns literally doesn't work & can't be fixed)
                    // <0 = nothing falls back, everything uses system dns
                    // >0 = falls back if goes past that amount of failed requests in a row
                    if (max < 0 || (max > 0 && failedRequests < max)) {
                        try {
                            List<InetAddress> result = Dns.SYSTEM.lookup(host);
                            failedRequests = 0; // reset on successful lookup
                            return result;
                        } catch (Exception e) {
                            failedRequests++;
                            DiscordSRV.error("System DNS FAILED to resolve hostname " + host + ", " +
                                    (max == 0 ? "" : failedRequests >= max ? "using fallback DNS for this request" : "switching to fallback DNS servers") + "!");
                            if (max == 0) {
                                // not using fallback
                                if (e instanceof UnknownHostException) {
                                    throw e;
                                } else {
                                    return null;
                                }
                            }
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

        OkHttpClient httpClient = IOUtil.newHttpClientBuilder()
                .dns(dns)
                // more lenient timeouts (normally 10 seconds for these 3)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();

        // set custom RestAction failure handler
        Consumer<? super Throwable> defaultFailure = RestAction.getDefaultFailure();
        RestAction.setDefaultFailure(throwable -> {
            if (throwable instanceof HierarchyException) {
                DiscordSRV.error("DiscordSRV failed to perform an action due to being lower in hierarchy than the action's target: " + throwable.getMessage());
            } else if (throwable instanceof PermissionException) {
                DiscordSRV.error("DiscordSRV failed to perform an action because the bot is missing the " + ((PermissionException) throwable).getPermission().name() + " permission: " + throwable.getMessage());
            } else if (throwable instanceof RateLimitedException) {
                DiscordSRV.error("Discord encountered rate limiting, this should not be possible. If you are running multiple DiscordSRV instances on the same token, this is considered API abuse and risks your server being IP banned from Discord. Make one bot per server.");
            } else if (throwable instanceof ErrorResponseException) {
                //ErrorResponse response = ((ErrorResponseException) throwable).getErrorResponse();
                DiscordSRV.error("DiscordSRV encountered an unknown Discord error: " + throwable.getMessage());
            } else {
                DiscordSRV.error("DiscordSRV encountered an unknown exception: " + throwable.getMessage() + "\n" + ExceptionUtils.getStackTrace(throwable));
            }

            if (config().getBoolean("DebugJDARestActions")) {
                Throwable cause = throwable.getCause();
                cause.printStackTrace();
            }
        });

        File tokenFile = new File(getDataFolder(), ".token");
        String token;
        if (StringUtils.isNotBlank(System.getProperty("DISCORDSRV_TOKEN"))) {
            token = System.getProperty("DISCORDSRV_TOKEN");
            DiscordSRV.debug("Using bot token supplied from JVM property DISCORDSRV_TOKEN");
        } else if (StringUtils.isNotBlank(System.getenv("DISCORDSRV_TOKEN"))) {
            token = System.getenv("DISCORDSRV_TOKEN");
            DiscordSRV.debug("Using bot token supplied from environment variable DISCORDSRV_TOKEN");
        } else if (tokenFile.exists()) {
            try {
                token = FileUtils.readFileToString(tokenFile, StandardCharsets.UTF_8);
                DiscordSRV.debug("Using bot token supplied from " + tokenFile.getPath());
            } catch (IOException e) {
                error(".token file could not be read: " + e.getMessage());
                token = null;
            }
        } else {
            token = config.getString("BotToken");
            DiscordSRV.debug("Using bot token supplied from config");
        }

        if (StringUtils.isBlank(token) || "BOTTOKEN".equalsIgnoreCase(token)) {
            error("No bot token has been set in the config; a bot token is required to connect to Discord.");
            return;
        } else if (token.length() < 59) {
            error("An invalid length bot token (" + token.length() + ") has been set in the config; a valid bot token is required to connect to Discord.");
            return;
        } else {
            // remove invalid characters
            token = token.replaceAll("[^\\w\\d-_.]", "");
        }

        callbackThreadPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), pool -> {
            final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("DiscordSRV - JDA Callback " + worker.getPoolIndex());
            return worker;
        }, null, true);

        final ThreadFactory gatewayThreadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordSRV - JDA Gateway").build();
        final ScheduledExecutorService gatewayThreadPool = Executors.newSingleThreadScheduledExecutor(gatewayThreadFactory);

        final ThreadFactory rateLimitThreadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordSRV - JDA Rate Limit").build();
        final ScheduledExecutorService rateLimitThreadPool = new ScheduledThreadPoolExecutor(5, rateLimitThreadFactory);

        // log in to discord
        try {
            // Gateway intents, uncomment closer to end of the deprecation period
            //noinspection deprecation
            jda = new JDABuilder(AccountType.BOT)
//                    JDABuilder.create(
//                    Arrays.asList(
//                            GatewayIntent.GUILD_MEMBERS,
//                            GatewayIntent.GUILD_BANS,
//                            GatewayIntent.GUILD_VOICE_STATES,
//                            GatewayIntent.GUILD_MESSAGES,
//                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
//                            GatewayIntent.DIRECT_MESSAGES
//                    ))
//                    .disableCache(Arrays.stream(CacheFlag.values()).filter(cacheFlag -> cacheFlag != CacheFlag.MEMBER_OVERRIDES && cacheFlag != CacheFlag.VOICE_STATE).collect(Collectors.toList()))
                    .setCallbackPool(callbackThreadPool, false)
                    .setGatewayPool(gatewayThreadPool, true)
                    .setRateLimitPool(rateLimitThreadPool, true)
                    .setWebsocketFactory(new WebSocketFactory()
                            .setDualStackMode(DualStackMode.IPV4_ONLY)
                    )
                    .setHttpClient(httpClient)
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .setToken(token)
                    .addEventListeners(new DiscordBanListener())
                    .addEventListeners(new DiscordChatListener())
                    .addEventListeners(new DiscordConsoleListener())
                    .addEventListeners(new DiscordAccountLinkListener())
                    .addEventListeners(groupSynchronizationManager)
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

        // start nickname updater thread
        if (nicknameUpdater != null) {
            if (nicknameUpdater.getState() != Thread.State.NEW) {
                nicknameUpdater.interrupt();
                nicknameUpdater = new NicknameUpdater();
            }
            Bukkit.getScheduler().runTaskLater(this, () -> nicknameUpdater.start(), 5 * 20);
        } else {
            nicknameUpdater = new NicknameUpdater();
            nicknameUpdater.start();
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
        if (serverIsLog4jCapable && getConsoleChannel() != null) {
            DiscordSRV.info(LangUtil.InternalMessage.CONSOLE_FORWARDING_ASSIGNED_TO_CHANNEL + " " + consoleChannel);

            // attach appender to queue console messages
            consoleAppender = new ConsoleAppender();

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

        // warn if the console channel is connected to a chat channel
        if (getMainTextChannel() != null && StringUtils.isNotBlank(consoleChannel) && getMainTextChannel().getId().equals(consoleChannel)) DiscordSRV.warning(LangUtil.InternalMessage.CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL);

        // send server startup message
        DiscordUtil.sendMessage(getMainTextChannel(), PlaceholderUtil.replacePlaceholdersToDiscord(LangUtil.Message.SERVER_STARTUP_MESSAGE.toString()), 0, false);

        // extra enabled check before doing bukkit api stuff
        if (!isEnabled()) return;

        // start server watchdog
        if (serverWatchdog != null && serverWatchdog.getState() != Thread.State.NEW) serverWatchdog.interrupt();
        serverWatchdog = new ServerWatchdog();
        serverWatchdog.start();

        // start lag (tps) monitor
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Lag(), 100L, 1L);

        // cancellation detector
        reloadCancellationDetector();

        // load account links
        if (JdbcAccountLinkManager.shouldUseJdbc()) {
            try {
                accountLinkManager = new JdbcAccountLinkManager();
            } catch (SQLException e) {
                StringBuilder stringBuilder = new StringBuilder("JDBC account link backend failed to initialize: ").append(ExceptionUtils.getMessage(e));

                Throwable selected = e.getCause();
                while (selected != null) {
                    stringBuilder.append("\n").append("Caused by: ").append(selected instanceof UnknownHostException ? "UnknownHostException" : ExceptionUtils.getMessage(selected));
                    selected = selected.getCause();
                }

                DiscordSRV.warning(
                        stringBuilder.toString()
                                .replace(config.getString("Experiment_JdbcAccountLinkBackend"), "<jdbc url>")
                                .replace(config.getString("Experiment_JdbcUsername"), "<jdbc username>")
                                .replace(config.getString("Experiment_JdbcPassword"), "<jdbc password>")
                );
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
            Class.forName("org.bukkit.event.player.PlayerAdvancementDoneEvent");
            new PlayerAdvancementDoneListener();
        } catch (Exception ignored) {
            new PlayerAchievementsListener();
        }

        // plugin hooks
        for (Class<? extends PluginHook> hookClass : Arrays.asList(
                // chat plugins
                github.scarsz.discordsrv.hooks.chat.FancyChatHook.class,
                github.scarsz.discordsrv.hooks.chat.HerochatHook.class,
                github.scarsz.discordsrv.hooks.chat.LegendChatHook.class,
                github.scarsz.discordsrv.hooks.chat.LunaChatHook.class,
                github.scarsz.discordsrv.hooks.chat.TownyChatHook.class,
                github.scarsz.discordsrv.hooks.chat.UltimateChatHook.class,
                github.scarsz.discordsrv.hooks.chat.VentureChatHook.class,
                // vanish plugins
                github.scarsz.discordsrv.hooks.vanish.EssentialsHook.class,
                github.scarsz.discordsrv.hooks.vanish.PhantomAdminHook.class,
                github.scarsz.discordsrv.hooks.vanish.SuperVanishHook.class,
                github.scarsz.discordsrv.hooks.vanish.VanishNoPacketHook.class,
                // dynmap
                github.scarsz.discordsrv.hooks.DynmapHook.class
        )) {
            try {
                PluginHook pluginHook = hookClass.getDeclaredConstructor().newInstance();
                if (pluginHook.isEnabled()) {
                    DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", pluginHook.getPlugin().getName()));
                    Bukkit.getPluginManager().registerEvents(pluginHook, this);
                    pluginHooks.add(pluginHook);
                }
            } catch (Exception e) {
                // ignore class not found exceptions
                if (!(e instanceof ClassNotFoundException)) {
                    DiscordSRV.error("Failed to load " + hookClass.getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        if (pluginHooks.stream().noneMatch(pluginHook -> pluginHook instanceof ChatHook)) {
            DiscordSRV.info(LangUtil.InternalMessage.NO_CHAT_PLUGIN_HOOKED);
            getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        }

        // load user-defined colors
        reloadColors();

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
                if (pluginHooks.size() == 0) {
                    put("none", 1);
                } else {
                    for (PluginHook hookedPlugin : pluginHooks) {
                        put(hookedPlugin.getPlugin().getName(), 1);
                    }
                }
            }}));
            bStats.addCustomChart(new BStats.SingleLineChart("minecraft-discord_account_links", () -> accountLinkManager.getLinkedAccounts().size()));
            bStats.addCustomChart(new BStats.SimplePie("server_language", () -> DiscordSRV.config().getLanguage().getName()));
            bStats.addCustomChart(new BStats.AdvancedPie("features", () -> new HashMap<String, Integer>() {{
                if (getConsoleChannel() != null) put("Console channel", 1);
                if (StringUtils.isNotBlank(config().getString("DiscordChatChannelPrefix"))) put("Chatting prefix", 1);
                if (JdbcAccountLinkManager.shouldUseJdbc(true)) put("JDBC", 1);
                if (config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) put("Discord <- MC Reserializer", 1);
                if (config().getBoolean("Experiment_MCDiscordReserializer_ToDiscord")) put("MC -> Discord Reserializer", 1);
                if (config().getBoolean("Experiment_MCDiscordReserializer_InBroadcast")) put("Broadcast Reserializer", 1);
                if (config().getBoolean("Experiment_Automatic_Color_Translations")) put("Automatic Color Translation", 1);
                if (config().getBoolean("Experiment_WebhookChatMessageDelivery")) put("Webhooks", 1);
                if (config().getBoolean("DiscordChatChannelTranslateMentions")) put("Mentions", 1);
                if (config().getMap("GroupRoleSynchronizationGroupsAndRolesToSync").values().stream().anyMatch(s -> s.toString().replace("0", "").length() > 0)) put("Group -> role synchronization", 1);
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

        // start the group synchronization task
        if (PluginUtil.pluginHookIsEnabled("Vault") && isGroupRoleSynchronizationEnabled()) {
            int cycleTime = DiscordSRV.config().getInt("GroupRoleSynchronizationCycleTime") * 20 * 60;
            if (cycleTime < 20 * 60) cycleTime = 20 * 60;
            try {
                groupSynchronizationManager.resync(GroupSynchronizationManager.SyncDirection.AUTHORITATIVE);
            } catch (Exception e) {
                error("Failed to resync\n" + ExceptionUtils.getMessage(e));
            }
            Bukkit.getPluginManager().registerEvents(groupSynchronizationManager, this);
            Bukkit.getScheduler().runTaskTimerAsynchronously(DiscordSRV.getPlugin(),
                    () -> groupSynchronizationManager.resync(GroupSynchronizationManager.SyncDirection.TO_DISCORD),
                    cycleTime,
                    cycleTime
            );
            if (PluginUtil.pluginHookIsEnabled("LuckPerms")) {
                Bukkit.getPluginManager().registerEvents(new github.scarsz.discordsrv.hooks.permissions.LuckPermsHook(), this);
            }
        }

        voiceModule = new VoiceModule();

        PluginCommand discordCommand = getCommand("discord");
        if (discordCommand != null && discordCommand.getPlugin() != this) {
            DiscordSRV.warning("/discord command is being handled by plugin other than DiscordSRV. You must use /discordsrv instead.");
        }

        // set ready status
        if (jda.getStatus() == JDA.Status.CONNECTED) {
            isReady = true;
            api.callEvent(new DiscordReadyEvent());
        }
    }

    @Override
    public void onDisable() {
        final long shutdownStartTime = System.currentTimeMillis();
        DebugUtil.disabledOnce = true;
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordSRV - Shutdown").build();
        final ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        try {
            executor.invokeAll(Collections.singletonList(() -> {
                // set server shutdown topics if enabled
                if (config().getBoolean("ChannelTopicUpdaterChannelTopicsAtShutdownEnabled")) {
                    String time = TimeUtil.timeStamp();
                    String serverVersion = Bukkit.getBukkitVersion();
                    String totalPlayers = Integer.toString(getTotalPlayerCount());
                    DiscordUtil.setTextChannelTopic(
                            getMainTextChannel(),
                            LangUtil.Message.CHAT_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN.toString()
                                    .replaceAll("%time%|%date%", time)
                                    .replace("%serverversion%", serverVersion)
                                    .replace("%totalplayers%", totalPlayers)
                    );
                    DiscordUtil.setTextChannelTopic(
                            getConsoleChannel(),
                            LangUtil.Message.CONSOLE_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN.toString()
                                    .replaceAll("%time%|%date%", time)
                                    .replace("%serverversion%", serverVersion)
                                    .replace("%totalplayers%", totalPlayers)
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

                // kill nickname updater
                if (nicknameUpdater != null) nicknameUpdater.interrupt();

                // kill server watchdog
                if (serverWatchdog != null) serverWatchdog.interrupt();

                // shutdown the update checker
                if (updateChecker != null) updateChecker.shutdown();

                // serialize account links to disk
                if (accountLinkManager != null) accountLinkManager.save();

                // close cancellation detector
                if (cancellationDetector != null) cancellationDetector.close();

                // shutdown the console appender
                if (consoleAppender != null) consoleAppender.shutdown();

                // remove the jda filter
                if (jdaFilter != null) {
                    try {
                        org.apache.logging.log4j.core.Logger logger = ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger());

                        Field configField = null;
                        Class<?> targetClass = logger.getClass();

                        // get a field named config or privateConfig from the logger class or any of it's super classes
                        while (targetClass != null) {
                            try {
                                configField = targetClass.getDeclaredField("config");
                                break;
                            } catch (NoSuchFieldException ignored) {}

                            try {
                                configField = targetClass.getDeclaredField("privateConfig");
                                break;
                            } catch (NoSuchFieldException ignored) {}

                            targetClass = targetClass.getSuperclass();
                        }

                        if (configField != null) {
                            if (!configField.isAccessible()) configField.setAccessible(true);

                            Object config = configField.get(logger);
                            Field configField2 = config.getClass().getDeclaredField("config");
                            if (!configField2.isAccessible()) configField2.setAccessible(true);

                            Object config2 = configField2.get(config);
                            if (config2 instanceof org.apache.logging.log4j.core.filter.Filterable) {
                                ((org.apache.logging.log4j.core.filter.Filterable) config2).removeFilter(jdaFilter);
                                jdaFilter = null;
                            }
                        }
                    } catch (Throwable t) {
                        getLogger().warning("Could not remove JDA Filter: " + t.toString());
                    }
                }

                // Clear JDA listeners
                if (jda != null) jda.getEventManager().getRegisteredListeners().forEach(listener -> jda.getEventManager().unregister(listener));

                // send server shutdown message
                String shutdownFormat = LangUtil.Message.SERVER_SHUTDOWN_MESSAGE.toString();

                // Check if the format contains a placeholder (Takes long to do cause the server is shutting down)
                if (Pattern.compile("%[^%]+%").matcher(shutdownFormat).find()) {
                    shutdownFormat = PlaceholderUtil.replacePlaceholdersToDiscord(shutdownFormat);
                }

                DiscordUtil.sendMessageBlocking(getMainTextChannel(), shutdownFormat);

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

                if (callbackThreadPool != null) callbackThreadPool.shutdownNow();

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
            config().dget("DiscordChatChannelColorTranslations").children().forEach(dynamic ->
                    colors.put(dynamic.key().convert().intoString().toUpperCase(), dynamic.convert().intoString()));
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
        if (!GamePermissionUtil.hasPermission(player, "discordsrv.chat")) {
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
        String prefix = config().getString("DiscordChatChannelPrefixRequiredToProcessMessage");
        if (!DiscordUtil.strip(message).startsWith(prefix)) {
            debug("User " + player.getName() + " sent a message but it was not delivered to Discord because the message didn't start with \"" + prefix + "\" (DiscordChatChannelPrefixRequiredToProcessMessage): \"" + message + "\"");
            return;
        }

        GameChatMessagePreProcessEvent preEvent = api.callEvent(new GameChatMessagePreProcessEvent(channel, message, player));
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

        boolean reserializer = DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToDiscord");

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
                .replace("%worldalias%", DiscordUtil.strip(MultiverseCoreHook.getWorldAlias(player.getWorld().getName())));
        discordMessage = PlaceholderUtil.replacePlaceholdersToDiscord(discordMessage, player);

        String displayName = DiscordUtil.strip(player.getDisplayName());
        if (reserializer) {
            message = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacy().deserialize(message));
        } else {
            displayName = DiscordUtil.escapeMarkdown(displayName);
        }

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

        GameChatMessagePostProcessEvent postEvent = api.callEvent(new GameChatMessagePostProcessEvent(channel, discordMessage, player, preEvent.isCancelled()));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("GameChatMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }
        channel = postEvent.getChannel(); // update channel from event in case any listeners modified it
        discordMessage = postEvent.getProcessedMessage(); // update message from event in case any listeners modified it

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

            message = PlaceholderUtil.replacePlaceholdersToDiscord(message, player);
            if (!reserializer) {
                message = DiscordUtil.strip(message);
            } else {
                message = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacy().deserialize(message));
            }

            message = DiscordUtil.cutPhrases(message);

            if (config().getBoolean("DiscordChatChannelTranslateMentions")) message = DiscordUtil.convertMentionsFromNames(message, getMainGuild());

            WebhookUtil.deliverMessage(destinationChannel, player, message);
        }
    }

    public void broadcastMessageToMinecraftServer(String channel, String message, User author) {
        // apply regex to message
        if (StringUtils.isNotBlank(config().getString("DiscordChatChannelRegex")))
            message = message.replaceAll(config().getString("DiscordChatChannelRegex"), config().getString("DiscordChatChannelRegexReplacement"));

        // apply placeholder API values
        Player authorPlayer = null;
        UUID authorLinkedUuid = accountLinkManager.getUuid(author.getId());
        if (authorLinkedUuid != null) authorPlayer = Bukkit.getPlayer(authorLinkedUuid);

        message = PlaceholderUtil.replacePlaceholders(message, authorPlayer);

        if (pluginHooks.size() == 0 || channel == null) {
            if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) {
                TextComponent textComponent = MinecraftSerializer.INSTANCE.serialize(message);
                TextAdapter.sendComponent(PlayerUtil.getOnlinePlayers(), textComponent);
            } else {
                for (Player player : PlayerUtil.getOnlinePlayers()) player.sendMessage(message);
            }

            PlayerUtil.notifyPlayersOfMentions(null, message);
        } else {
            for (PluginHook pluginHook : pluginHooks) {
                if (pluginHook instanceof ChatHook) {
                    ((ChatHook) pluginHook).broadcastMessageToChannel(channel, message);
                    return;
                }
            }

            broadcastMessageToMinecraftServer(null, message, author);
            return;
        }
        api.callEvent(new DiscordGuildMessagePostBroadcastEvent(channel, message));
    }

    public MessageFormat getMessageFromConfiguration(String key) {
        if (!config.getOptional(key).isPresent()) {
            return null;
        }

        Optional<Boolean> enabled = config.getOptionalBoolean(key + ".Enabled");
        if (enabled.isPresent() && !enabled.get()) {
            return null;
        }

        MessageFormat messageFormat = new MessageFormat();

        if (config().getOptional(key + ".Embed").isPresent()) {
            Optional<String> hexColor = config().getOptionalString(key + ".Embed.Color");
            if (hexColor.isPresent()) {
                String hex = hexColor.get().trim();
                if (!hex.startsWith("#")) hex = "#" + hex;
                if (hex.length() == 7) {
                    messageFormat.setColor(
                            new Color(
                                    Integer.valueOf(hex.substring(1, 3), 16),
                                    Integer.valueOf(hex.substring(3, 5), 16),
                                    Integer.valueOf(hex.substring(5, 7), 16)
                            )
                    );
                }
            } else {
                config().getOptionalInt(key + ".Embed.Color").map(Color::new).ifPresent(messageFormat::setColor);
            }

            if (config().getOptional(key + ".Embed.Author").isPresent()) {
                config().getOptionalString(key + ".Embed.Author.Name")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setAuthorName);
                config().getOptionalString(key + ".Embed.Author.Url")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setAuthorUrl);
                config().getOptionalString(key + ".Embed.Author.ImageUrl")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setAuthorImageUrl);
            }

            config().getOptionalString(key + ".Embed.ThumbnailUrl")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setThumbnailUrl);

            config().getOptionalString(key + ".Embed.Title.Text")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setTitle);

            config().getOptionalString(key + ".Embed.Title.Url")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setTitleUrl);

            config().getOptionalString(key + ".Embed.Description")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setDescription);

            Optional<List<String>> fieldsOptional = config().getOptionalStringList(key + ".Embed.Fields");
            if (fieldsOptional.isPresent()) {
                List<MessageEmbed.Field> fields = new ArrayList<>();
                for (String s : fieldsOptional.get()) {
                    if (s.contains(";")) {
                        String[] parts = s.split(";");
                        if (parts.length < 2) {
                            continue;
                        }

                        boolean inline = parts.length < 3 || Boolean.parseBoolean(parts[2]);
                        fields.add(new MessageEmbed.Field(parts[0], parts[1], inline, true));
                    } else {
                        boolean inline = Boolean.parseBoolean(s);
                        fields.add(new MessageEmbed.Field("\u200e", "\u200e", inline, true));
                    }
                }
                messageFormat.setFields(fields);
            }

            config().getOptionalString(key + ".Embed.ImageUrl")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setImageUrl);

            if (config().getOptional(key + ".Embed.Footer").isPresent()) {
                config().getOptionalString(key + ".Embed.Footer.Text")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setFooterText);
                config().getOptionalString(key + ".Embed.Footer.IconUrl")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setFooterIconUrl);
            }

            Optional<Boolean> timestampOptional = config().getOptionalBoolean(key + ".Embed.Timestamp");
            if (timestampOptional.isPresent()) {
                if (timestampOptional.get()) {
                    messageFormat.setTimestamp(new Date().toInstant());
                }
            } else {
                Optional<Long> epochOptional = config().getOptionalLong(key + ".Embed.Timestamp");
                epochOptional.ifPresent(timestamp -> messageFormat.setTimestamp(new Date(timestamp).toInstant()));
            }
        }

        if (config().getOptional(key + ".Webhook").isPresent()) {
            Optional<Boolean> webhookEnabled = config().getOptionalBoolean(key + ".Webhook.Enable");
            if (webhookEnabled.isPresent() && webhookEnabled.get()) {
                messageFormat.setUseWebhooks(true);
                config.getOptionalString(key + ".Webhook.AvatarUrl")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setWebhookAvatarUrl);
                config.getOptionalString(key + ".Webhook.Name")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setWebhookName);
            }
        }

        Optional<String> content = config().getOptionalString(key + ".Content");
        if (content.isPresent() && StringUtils.isNotBlank(content.get())) {
            messageFormat.setContent(content.get());
        }

        return messageFormat.isAnyContent() ? messageFormat : null;
    }

    public Message translateMessage(MessageFormat messageFormat, BiFunction<String, Boolean, String> translator) {
        MessageBuilder messageBuilder = new MessageBuilder();
        Optional.ofNullable(messageFormat.getContent()).map(content -> translator.apply(content, true))
                .filter(StringUtils::isNotBlank).ifPresent(messageBuilder::setContent);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(
                Optional.ofNullable(messageFormat.getAuthorName())
                        .map(content -> translator.apply(content, false)).filter(StringUtils::isNotBlank).orElse(null),
                Optional.ofNullable(messageFormat.getAuthorUrl())
                        .map(content -> translator.apply(content, true)).filter(StringUtils::isNotBlank).orElse(null),
                Optional.ofNullable(messageFormat.getAuthorImageUrl())
                        .map(content -> translator.apply(content, true)).filter(StringUtils::isNotBlank).orElse(null)
        );
        embedBuilder.setThumbnail(Optional.ofNullable(messageFormat.getThumbnailUrl())
                .map(content -> translator.apply(content, true)).filter(StringUtils::isNotBlank).orElse(null));
        embedBuilder.setImage(Optional.ofNullable(messageFormat.getImageUrl())
                .map(content -> translator.apply(content, true)).filter(StringUtils::isNotBlank).orElse(null));
        embedBuilder.setDescription(Optional.ofNullable(messageFormat.getDescription())
                .map(content -> translator.apply(content, true)).filter(StringUtils::isNotBlank).orElse(null));
        embedBuilder.setTitle(
                Optional.ofNullable(messageFormat.getTitle()).map(content -> translator.apply(content, false)).filter(StringUtils::isNotBlank).orElse(null),
                Optional.ofNullable(messageFormat.getTitleUrl()).map(content -> translator.apply(content, true)).filter(StringUtils::isNotBlank).orElse(null)
        );
        embedBuilder.setFooter(
                Optional.ofNullable(messageFormat.getFooterText())
                        .map(content -> translator.apply(content, true)).filter(StringUtils::isNotBlank).orElse(null),
                Optional.ofNullable(messageFormat.getFooterIconUrl())
                        .map(content -> translator.apply(content, true)).filter(StringUtils::isNotBlank).orElse(null)
        );
        embedBuilder.setColor(messageFormat.getColor());
        embedBuilder.setTimestamp(messageFormat.getTimestamp());
        if (!embedBuilder.isEmpty()) messageBuilder.setEmbed(embedBuilder.build());

        return messageBuilder.isEmpty() ? null : messageBuilder.build();
    }

    public String getEmbedAvatarUrl(Player player) {
        return getEmbedAvatarUrl(player.getName(), player.getUniqueId());
    }

    public String getEmbedAvatarUrl(String playerUsername, UUID playerUniqueId) {
        String avatarUrl = DiscordSRV.config().getString("Experiment_EmbedAvatarUrl");

        if (StringUtils.isBlank(avatarUrl)) avatarUrl = "https://minotar.net/helm/{uuid-nodashes}/{size}";
        avatarUrl = avatarUrl
                .replace("{username}", playerUsername)
                .replace("{uuid}", playerUniqueId.toString())
                .replace("{uuid-nodashes}", playerUniqueId.toString().replace("-", ""))
                .replace("{size}", "128");

        return avatarUrl;
    }

    public int getLength(Message message) {
        StringBuilder content = new StringBuilder();
        content.append(message.getContentRaw());

        message.getEmbeds().stream().findFirst().ifPresent(embed -> {
            if (embed.getTitle() != null) {
                content.append(embed.getTitle());
            }
            if (embed.getDescription() != null) {
                content.append(embed.getDescription());
            }
            if (embed.getAuthor() != null) {
                content.append(embed.getAuthor().getName());
            }
            for (MessageEmbed.Field field : embed.getFields()) {
                content.append(field.getName()).append(field.getValue());
            }
        });

        return content.toString().replaceAll("[^A-z]", "").length();
    }

    public Map<String, String> getGroupSynchronizables() {
        HashMap<String, String> map = new HashMap<>();
        config.dget("GroupRoleSynchronizationGroupsAndRolesToSync").children().forEach(dynamic ->
                map.put(dynamic.key().convert().intoString(), dynamic.convert().intoString()));
        return map;
    }

    public Map<String, String> getCannedResponses() {
        Map<String, String> responses = new HashMap<>();
        config.dget("DiscordCannedResponses").children()
                .forEach(dynamic -> responses.put(dynamic.key().convert().intoString(), dynamic.convert().intoString()));
        return responses;
    }

    private static File playerDataFolder = null;
    public static int getTotalPlayerCount() {
        if (playerDataFolder == null && Bukkit.getWorlds().size() > 0) {
            playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder().getAbsolutePath(), "/playerdata");
        }

        File[] playerFiles = playerDataFolder.listFiles(f -> f.getName().endsWith(".dat"));
        return playerFiles != null ? playerFiles.length : 0;
    }

    /**
     * @return Whether or not file system is limited. If this is {@code true}, DiscordSRV will limit itself to not
     * modifying the server's plugins folder. This is used to prevent uploading of plugins via the console channel.
     */
    public static boolean isFileSystemLimited() {
        return System.getenv("LimitFS") != null || System.getProperty("LimitFS") != null;
    }

    /**
     * @return Whether or not DiscordSRV should disable it's update checker. Doing so is dangerous and can lead to
     * security vulnerabilities. You shouldn't use this.
     */
    public static boolean isUpdateCheckDisabled() {
        return System.getenv("NoUpdateChecks") != null || System.getProperty("NoUpdateChecks") != null ||
                config().getBooleanElse("UpdateCheckDisabled", false);
    }

    /**
     * @return Whether or not DiscordSRV group role synchronization has been enabled in the configuration.
     */
    public boolean isGroupRoleSynchronizationEnabled() {
        final Map<String, String> groupsAndRolesToSync = config.getMap("GroupRoleSynchronizationGroupsAndRolesToSync");
        if (groupsAndRolesToSync.isEmpty()) return false;
        for (Map.Entry<String, String> entry : groupsAndRolesToSync.entrySet()) {
            final String group = entry.getKey();
            if (!group.isEmpty()) {
                final String roleId = entry.getValue();
                if (!(roleId.isEmpty() || roleId.equals("000000000000000000"))) return true;
            }
        }
        return false;
    }

}
