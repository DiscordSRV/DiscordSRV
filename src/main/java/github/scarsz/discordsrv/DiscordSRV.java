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

import alexh.weak.Dynamic;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.DualStackMode;
import com.neovisionaries.ws.client.WebSocketFactory;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
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
import github.scarsz.discordsrv.hooks.vanish.VanishHook;
import github.scarsz.discordsrv.hooks.world.MultiverseCoreHook;
import github.scarsz.discordsrv.listeners.*;
import github.scarsz.discordsrv.modules.alerts.AlertListener;
import github.scarsz.discordsrv.modules.requirelink.RequireLinkModule;
import github.scarsz.discordsrv.modules.voice.VoiceModule;
import github.scarsz.discordsrv.objects.*;
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
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.IOUtil;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitWorker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.record.Record;

import javax.annotation.CheckReturnValue;
import javax.net.ssl.SSLContext;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * DiscordSRV's main class, can be accessed via {@link #getPlugin()}.
 *
 * @see #getAccountLinkManager()
 * @see #sendJoinMessage(Player, String)
 * @see #sendLeaveMessage(Player, String)
 */
@SuppressWarnings({"unused", "WeakerAccess", "ConstantConditions"})
public class DiscordSRV extends JavaPlugin {

    public static final ApiManager api = new ApiManager();
    public static boolean isReady = false;
    public static boolean updateIsAvailable = false;
    public static boolean updateChecked = false;
    public static boolean invalidBotToken = false;
    public static String version = "";

    // Managers
    @Getter private AccountLinkManager accountLinkManager;
    @Getter private CommandManager commandManager = new CommandManager();
    @Getter private GroupSynchronizationManager groupSynchronizationManager = new GroupSynchronizationManager();

    // Threads
    @Getter private ChannelTopicUpdater channelTopicUpdater;
    @Getter private ConsoleMessageQueueWorker consoleMessageQueueWorker;
    @Getter private NicknameUpdater nicknameUpdater;
    @Getter private PresenceUpdater presenceUpdater;
    @Getter private ServerWatchdog serverWatchdog;
    @Getter private ScheduledExecutorService updateChecker = null;

    // Modules
    @Getter private AlertListener alertListener = null;
    @Getter private RequireLinkModule requireLinkModule;
    @Getter private VoiceModule voiceModule;

    // Config
    @Getter private final Map<String, String> channels = new LinkedHashMap<>(); // <in-game channel name, discord channel>
    @Getter private final Map<String, String> colors = new HashMap<>();
    @Getter private final Map<Pattern, String> consoleRegexes = new HashMap<>();
    @Getter private final Map<Pattern, String> gameRegexes = new HashMap<>();
    @Getter private final Map<Pattern, String> discordRegexes = new HashMap<>();
    private final DynamicConfig config;

    // Console
    @Getter private final Deque<ConsoleMessage> consoleMessageQueue = new LinkedList<>();
    @Getter private ConsoleAppender consoleAppender;

    @Getter private final long startTime = System.currentTimeMillis();
    @Getter private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Getter private CancellationDetector<AsyncPlayerChatEvent> cancellationDetector = null;
    @Getter private final Set<PluginHook> pluginHooks = new HashSet<>();

    // Files
    @Getter private final File configFile = new File(getDataFolder(), "config.yml");
    @Getter private final File messagesFile = new File(getDataFolder(), "messages.yml");
    @Getter private final File voiceFile = new File(getDataFolder(), "voice.yml");
    @Getter private final File linkingFile = new File(getDataFolder(), "linking.yml");
    @Getter private final File synchronizationFile = new File(getDataFolder(), "synchronization.yml");
    @Getter private final File alertsFile = new File(getDataFolder(), "alerts.yml");
    @Getter private final File debugFolder = new File(getDataFolder(), "debug");
    @Getter private final File logFolder = new File(getDataFolder(), "discord-console-logs");
    @Getter private final File linkedAccountsFile = new File(getDataFolder(), "linkedaccounts.json");

    // JDA & JDA related
    @Getter private JDA jda = null;
    private ExecutorService callbackThreadPool;
    private JdaFilter jdaFilter;

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
    public void reloadRegexes() {
        synchronized (consoleRegexes) {
            consoleRegexes.clear();
            loadRegexesFromConfig(config().dget("DiscordConsoleChannelFilters"), consoleRegexes);
        }
        synchronized (gameRegexes) {
            gameRegexes.clear();
            loadRegexesFromConfig(config().dget("DiscordChatChannelGameFilters"), gameRegexes);
        }
        synchronized (discordRegexes) {
            discordRegexes.clear();
            loadRegexesFromConfig(config().dget("DiscordChatChannelDiscordFilters"), discordRegexes);
        }
    }
    private void loadRegexesFromConfig(final Dynamic dynamic, final Map<Pattern, String> map) {
        dynamic.children().forEach(d -> {
            String key = d.key().convert().intoString();
            if (StringUtils.isEmpty(key)) return;
            try {
                Pattern pattern = Pattern.compile(key, Pattern.DOTALL);
                map.put(pattern, d.convert().intoString());
            } catch (PatternSyntaxException e) {
                error("Invalid regex pattern: " + key + " (" + e.getDescription() + ")");
            }
        });
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
        if (jda == null) return null;

        String consoleChannel = config.getString("DiscordConsoleChannelId");
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
        if (source == null) return null;
        return channels.entrySet().stream()
                .filter(entry -> source.getId().equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }
    public File getLogFile() {
        String fileName = config().getString("DiscordConsoleChannelUsageLog");
        if (StringUtils.isBlank(fileName)) return null;
        fileName = fileName.replace("%date%", TimeUtil.date());
        return new File(this.getLogFolder(), fileName);
    }

    // log messages
    private static void logThrowable(Throwable throwable, Consumer<String> logger) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));

        for (String line : stringWriter.toString().split("\n")) logger.accept(line);
    }
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
    public static void error(Throwable throwable) {
         logThrowable(throwable, DiscordSRV::error);
    }
    public static void error(String message, Throwable throwable) {
        error(message);
        error(throwable);
    }
    public static void debug(String message) {
        // return if plugin is not in debug mode
        if (DiscordSRV.config().getInt("DebugLevel") == 0) return;

        getPlugin().getLogger().info("[DEBUG] " + message + (DiscordSRV.config().getInt("DebugLevel") >= 2 ? "\n" + DebugUtil.getStackTrace() : ""));
    }
    public static void debug(Throwable throwable) {
        logThrowable(throwable, DiscordSRV::debug);
    }
    public static void debug(Throwable throwable, String message) {
        debug(throwable);
        debug(message);
    }
    public static void debug(Collection<String> message) {
        message.forEach(DiscordSRV::debug);
    }

    public DiscordSRV() {
        super();

        // load config
        getDataFolder().mkdirs();
        config = new DynamicConfig();
        config.addSource(DiscordSRV.class, "config", getConfigFile());
        config.addSource(DiscordSRV.class, "messages", getMessagesFile());
        config.addSource(DiscordSRV.class, "voice", getVoiceFile());
        config.addSource(DiscordSRV.class, "linking", getLinkingFile());
        config.addSource(DiscordSRV.class, "synchronization", getSynchronizationFile());
        config.addSource(DiscordSRV.class, "alerts", getAlertsFile());
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
                            lang.getName().equalsIgnoreCase(forcedLanguage)
                    )
                    .findFirst().ifPresent(config::setLanguage);
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
        if (++DebugUtil.initializationCount > 1) {
            DiscordSRV.error(ChatColor.RED + LangUtil.InternalMessage.PLUGIN_RELOADED.toString());
            PlayerUtil.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("discordsrv.admin"))
                    .forEach(player -> player.sendMessage(ChatColor.RED + LangUtil.InternalMessage.PLUGIN_RELOADED.toString()));
        }

        ConfigUtil.migrate();
        ConfigUtil.logMissingOptions();
        DiscordSRV.debug("Language is " + config.getLanguage().getName());

        version = getDescription().getVersion();
        Thread initThread = new Thread(this::init, "DiscordSRV - Initialization");
        initThread.setUncaughtExceptionHandler((t, e) -> {
            // make DiscordSRV go red in /plugins
            disablePlugin();
            error(e);
            getLogger().severe("DiscordSRV failed to load properly: " + e.getMessage() + ". See " + github.scarsz.discordsrv.util.DebugUtil.run("DiscordSRV") + " for more information. Can't figure it out? Go to https://discordsrv.com/discord for help");
        });
        initThread.start();
    }

    public void disablePlugin() {
        Bukkit.getScheduler().runTask(
                this,
                () -> Bukkit.getPluginManager().disablePlugin(this)
        );

        PluginCommand pluginCommand = getCommand("discordsrv");
        if (pluginCommand != null && pluginCommand.getPlugin() == this) {
            try {
                Field owningPlugin = pluginCommand.getClass().getDeclaredField("owningPlugin");
                if (!owningPlugin.isAccessible()) owningPlugin.setAccessible(true);

                // make the command's owning plugin always enabled (give a better error to the user)
                owningPlugin.set(pluginCommand, new PluginBase() {
                    @Override
                    public @NotNull File getDataFolder() {
                        return DiscordSRV.this.getDataFolder();
                    }

                    @Override
                    public @NotNull PluginDescriptionFile getDescription() {
                        return DiscordSRV.this.getDescription();
                    }

                    @Override
                    public @NotNull FileConfiguration getConfig() {
                        return DiscordSRV.this.getConfig();
                    }

                    @Override
                    public @Nullable InputStream getResource(@NotNull String filename) {
                        return DiscordSRV.this.getResource(filename);
                    }

                    @Override
                    public void saveConfig() {
                        DiscordSRV.this.saveConfig();
                    }

                    @Override
                    public void saveDefaultConfig() {
                        DiscordSRV.this.saveDefaultConfig();
                    }

                    @Override
                    public void saveResource(@NotNull String resourcePath, boolean replace) {
                        DiscordSRV.this.saveResource(resourcePath, replace);
                    }

                    @Override
                    public void reloadConfig() {
                        DiscordSRV.this.reloadConfig();
                    }

                    @Override
                    public @NotNull PluginLoader getPluginLoader() {
                        return DiscordSRV.this.getPluginLoader();
                    }

                    @Override
                    public @NotNull Server getServer() {
                        return DiscordSRV.this.getServer();
                    }

                    @Override
                    public boolean isEnabled() {
                        // otherwise PluginCommand throws a exception
                        return true;
                    }

                    @Override
                    public void onDisable() {
                        DiscordSRV.this.onDisable();
                    }

                    @Override
                    public void onLoad() {
                        DiscordSRV.this.onLoad();
                    }

                    @Override
                    public void onEnable() {
                        DiscordSRV.this.onEnable();
                    }

                    @Override
                    public boolean isNaggable() {
                        return DiscordSRV.this.isNaggable();
                    }

                    @Override
                    public void setNaggable(boolean canNag) {
                        DiscordSRV.this.setNaggable(canNag);
                    }

                    @Override
                    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
                        return DiscordSRV.this.getDefaultWorldGenerator(worldName, id);
                    }

                    @Override
                    public @NotNull Logger getLogger() {
                        return DiscordSRV.this.getLogger();
                    }

                    @Override
                    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
                        return DiscordSRV.this.onCommand(sender, command, label, args);
                    }

                    @Override
                    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
                        return DiscordSRV.this.onTabComplete(sender, command, alias, args);
                    }
                });
            } catch (Throwable ignored) {}
        }
    }

    public void init() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlugMan")) {
            Plugin plugMan = Bukkit.getPluginManager().getPlugin("PlugMan");
            try {
                List<String> ignoredPlugins = (List<String>) plugMan.getClass().getMethod("getIgnoredPlugins").invoke(plugMan);
                if (!ignoredPlugins.contains("DiscordSRV")) {
                    ignoredPlugins.add("DiscordSRV");
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {}
        }

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
            error(e);
        }

        requireLinkModule = new RequireLinkModule();

        // start the update checker (will skip if disabled)
        if (!isUpdateCheckDisabled()) {
            if (updateChecker == null) {
                final ThreadFactory gatewayThreadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordSRV - Update Checker").build();
                updateChecker = Executors.newScheduledThreadPool(1);
            }
            DiscordSRV.updateIsAvailable = UpdateUtil.checkForUpdates();
            DiscordSRV.updateChecked = true;
            updateChecker.scheduleAtFixedRate(() ->
                    DiscordSRV.updateIsAvailable = UpdateUtil.checkForUpdates(false),
                    6, 6, TimeUnit.HOURS
            );
        }

        // shutdown previously existing jda if plugin gets reloaded
        if (jda != null) try { jda.shutdown(); jda = null; } catch (Exception e) { error(e); }

        // set default mention types to never ping everyone/here
        MessageAction.setDefaultMentions(config().getStringList("DiscordChatChannelAllowedMentions").stream()
                .map(s -> {
                    try {
                        return Message.MentionType.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        DiscordSRV.error("Unknown mention type \"" + s + "\" defined in DiscordChatChannelAllowedMentions");
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toSet()));
        DiscordSRV.debug("Allowed chat mention types: " + MessageAction.getDefaultMentions().stream().map(Enum::name).collect(Collectors.joining(", ")));

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
                debug("JdaFilter applied");
            } catch (Exception e) {
                error("Failed to attach JDA message filter to root logger", e);
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
                                            error(e);
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
                    } catch (InterruptedException ignored) {}

                    UnknownHostException exception = new UnknownHostException("All DNS resolvers failed to resolve hostname " + host + ". Not good.");
                    exception.setStackTrace(new StackTraceElement[]{exception.getStackTrace()[0]});
                    throw exception;
                }
            };
        } catch (Exception e) {
            DiscordSRV.error("Failed to make custom DNS client: " + e.getMessage());
        }

        Optional<Boolean> noopHostnameVerifier = config().getOptionalBoolean("NoopHostnameVerifier");
        OkHttpClient httpClient = IOUtil.newHttpClientBuilder()
                .dns(dns)
                // more lenient timeouts (normally 10 seconds for these 3)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .hostnameVerifier(noopHostnameVerifier.isPresent() && noopHostnameVerifier.get()
                        ? (hostname, sslSession) -> true
                        : OkHostnameVerifier.INSTANCE
                )
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
                error(cause);
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
            disablePlugin();
            error("No bot token has been set in the config; a bot token is required to connect to Discord.");
            invalidBotToken = true;
            return;
        } else if (token.length() < 59) {
            disablePlugin();
            error("An invalid length bot token (" + token.length() + ") has been set in the config; a valid bot token is required to connect to Discord."
                    + (token.length() == 32 ? " Did you copy the \"Client Secret\" instead of the \"Bot Token\" into the config?" : ""));
            invalidBotToken = true;
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
        if (config.getBooleanElse("EnablePresenceInformation", false)) {
            DiscordSRV.api.requireIntent(GatewayIntent.GUILD_PRESENCES);
            DiscordSRV.api.requireCacheFlag(CacheFlag.ACTIVITY);
            DiscordSRV.api.requireCacheFlag(CacheFlag.CLIENT_STATUS);
        }
        try {
            // see ApiManager for our default intents & cache flags
            jda = JDABuilder.create(api.getIntents())
                    // we disable anything that isn't enabled (everything is enabled by default)
                    .disableCache(Arrays.stream(CacheFlag.values()).filter(cacheFlag -> !api.getCacheFlags().contains(cacheFlag)).collect(Collectors.toList()))
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
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
                    .addEventListeners(new DiscordDisconnectListener())
                    .addEventListeners(groupSynchronizationManager)
                    .setContextEnabled(false)
                    .build();
            jda.awaitReady(); // let JDA be assigned as soon as we can, but wait until it's ready

            for (Guild guild : jda.getGuilds()) {
                getMainGuild().retrieveOwner(true).queue();
                getMainGuild().loadMembers()
                        .onSuccess(members -> DiscordSRV.debug("Loaded " + members.size() + " members in guild " + guild))
                        .onError(throwable -> DiscordSRV.error("Failed to retrieve members of guild " + guild, throwable))
                        .get(); // block DiscordSRV startup until members are loaded
            }
        } catch (LoginException e) {
            DiscordSRV.error(LangUtil.InternalMessage.FAILED_TO_CONNECT_TO_DISCORD + ": " + e.getMessage());
            return;
        } catch (Exception e) {
            if (e instanceof IllegalStateException && e.getMessage().equals("Was shutdown trying to await status")) {
                // already logged by JDA
                return;
            }
            DiscordSRV.error("An unknown error occurred building JDA...", e);
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
            DiscordSRV.error(jda.getInviteUrl(Permission.ADMINISTRATOR));
            return;
        }

        // see if console channel exists; if it does, tell user where it's been assigned & add console appender
        if (serverIsLog4jCapable) {
            DiscordSRV.info(getConsoleChannel() != null
                    ? LangUtil.InternalMessage.CONSOLE_FORWARDING_ASSIGNED_TO_CHANNEL + " " + getConsoleChannel()
                    : LangUtil.InternalMessage.NOT_FORWARDING_CONSOLE_OUTPUT.toString());

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
        }

        reloadChannels();
        reloadRegexes();

        // warn if the console channel is connected to a chat channel
        if (getMainTextChannel() != null && getConsoleChannel() != null && getMainTextChannel().getId().equals(getConsoleChannel().getId())) DiscordSRV.warning(LangUtil.InternalMessage.CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL);

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

                String message = stringBuilder.toString()
                        .replace(config.getString("Experiment_JdbcAccountLinkBackend"), "<jdbc url>")
                        .replace(config.getString("Experiment_JdbcUsername"), "<jdbc username>");
                if (!StringUtils.isEmpty(config.getString("Experiment_JdbcPassword"))) {
                    message = message.replace(config.getString("Experiment_JdbcPassword"), "");
                }

                DiscordSRV.warning(message);
                DiscordSRV.warning("Account link manager falling back to flat file");
                accountLinkManager = new AccountLinkManager();
            }
        } else {
            accountLinkManager = new AccountLinkManager();
        }
        Bukkit.getPluginManager().registerEvents(accountLinkManager, this);

        // register events
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
        for (String hookClassName : Arrays.asList(
                // chat plugins
                "github.scarsz.discordsrv.hooks.chat.ChattyChatHook",
                "github.scarsz.discordsrv.hooks.chat.FancyChatHook",
                "github.scarsz.discordsrv.hooks.chat.HerochatHook",
                "github.scarsz.discordsrv.hooks.chat.LegendChatHook",
                "github.scarsz.discordsrv.hooks.chat.LunaChatHook",
                "github.scarsz.discordsrv.hooks.chat.TownyChatHook",
                "github.scarsz.discordsrv.hooks.chat.UltimateChatHook",
                "github.scarsz.discordsrv.hooks.chat.VentureChatHook",
                // vanish plugins
                "github.scarsz.discordsrv.hooks.vanish.EssentialsHook",
                "github.scarsz.discordsrv.hooks.vanish.PhantomAdminHook",
                "github.scarsz.discordsrv.hooks.vanish.SuperVanishHook",
                "github.scarsz.discordsrv.hooks.vanish.VanishNoPacketHook",
                // dynmap
                "github.scarsz.discordsrv.hooks.DynmapHook",
                // luckperms
                "github.scarsz.discordsrv.hooks.permissions.LuckPermsHook"
        )) {
            try {
                Class<?> hookClass = Class.forName(hookClassName);

                PluginHook pluginHook = (PluginHook) hookClass.getDeclaredConstructor().newInstance();
                if (pluginHook.isEnabled()) {
                    DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", pluginHook.getPlugin().getName()));
                    Bukkit.getPluginManager().registerEvents(pluginHook, this);
                    try {
                        pluginHook.hook();
                        pluginHooks.add(pluginHook);
                    } catch (Throwable t) {
                        error("Failed to hook " + hookClassName, t);
                    }
                }
            } catch (Throwable e) {
                // ignore class not found errors
                if (!(e instanceof ClassNotFoundException) && !(e instanceof NoClassDefFoundError)) {
                    DiscordSRV.error("Failed to load " + hookClassName, e);
                }
            }
        }
        if (pluginHooks.stream().noneMatch(pluginHook -> pluginHook instanceof ChatHook)) {
            DiscordSRV.info(LangUtil.InternalMessage.NO_CHAT_PLUGIN_HOOKED);
            getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        }
        pluginHooks.add(new VanishHook() {
            @Override
            public boolean isVanished(Player player) {
                boolean vanished = false;
                for (MetadataValue metadataValue : player.getMetadata("vanished")) {
                    if (metadataValue.asBoolean()) {
                        vanished = true;
                        break;
                    }
                }
                return vanished;
            }

            @Override
            public Plugin getPlugin() {
                return null;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        if (PluginUtil.pluginHookIsEnabled("PlaceholderAPI", false)) {
            try {
                DiscordSRV.info(LangUtil.InternalMessage.PLUGIN_HOOK_ENABLING.toString().replace("{plugin}", "PlaceholderAPI"));
                Bukkit.getScheduler().runTask(this, () -> {
                    try {
                        if (me.clip.placeholderapi.PlaceholderAPIPlugin.getInstance().getLocalExpansionManager().findExpansionByIdentifier("discordsrv").isPresent()) {
                            getLogger().warning("The DiscordSRV PlaceholderAPI expansion is no longer required.");
                            getLogger().warning("The expansion is now integrated in DiscordSRV.");
                        }
                        new github.scarsz.discordsrv.hooks.PlaceholderAPIExpansion().register();
                    } catch (Throwable ignored) {
                        getLogger().severe("Failed to hook into PlaceholderAPI, please check your PlaceholderAPI version");
                    }
                });
            } catch (Exception e) {
                if (!(e instanceof ClassNotFoundException)) {
                    DiscordSRV.error("Failed to load PlaceholderAPI expansion", e);
                }
            }
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
                        Plugin plugin = hookedPlugin.getPlugin();
                        if (plugin == null) continue;
                        put(plugin.getName(), 1);
                    }
                }
            }}));
            bStats.addCustomChart(new BStats.SingleLineChart("minecraft-discord_account_links", () -> accountLinkManager.getLinkedAccounts().size()));
            bStats.addCustomChart(new BStats.SimplePie("server_language", () -> DiscordSRV.config().getLanguage().getName()));
            bStats.addCustomChart(new BStats.AdvancedPie("features", () -> new HashMap<String, Integer>() {{
                if (getConsoleChannel() != null) put("Console channel", 1);
                if (StringUtils.isNotBlank(config().getString("DiscordChatChannelPrefixRequiredToProcessMessage"))) put("Chatting prefix", 1);
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
                groupSynchronizationManager.resync(GroupSynchronizationManager.SyncDirection.AUTHORITATIVE, GroupSynchronizationManager.SyncCause.TIMER);
            } catch (Exception e) {
                error("Failed to resync\n" + ExceptionUtils.getMessage(e));
            }
            Bukkit.getPluginManager().registerEvents(groupSynchronizationManager, this);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                    () -> groupSynchronizationManager.resync(
                            GroupSynchronizationManager.SyncDirection.TO_DISCORD,
                            GroupSynchronizationManager.SyncCause.TIMER
                    ),
                    cycleTime,
                    cycleTime
            );
        }

        voiceModule = new VoiceModule();

        PluginCommand discordCommand = getCommand("discord");
        if (discordCommand != null && discordCommand.getPlugin() != this) {
            DiscordSRV.warning("/discord command is being handled by plugin other than DiscordSRV. You must use /discordsrv instead.");
        }

        alertListener = new AlertListener();
        jda.addEventListener(alertListener);

        // set ready status
        if (jda.getStatus() == JDA.Status.CONNECTED) {
            isReady = true;
            api.callEvent(new DiscordReadyEvent());
        }
    }

    @Override
    public void onDisable() {
        final long shutdownStartTime = System.currentTimeMillis();

        // prepare the shutdown message
        String shutdownFormat = LangUtil.Message.SERVER_SHUTDOWN_MESSAGE.toString();

        // Check if the format contains a placeholder (Takes long to do cause the server is shutting down)
        // need to run this on the main thread
        if (Pattern.compile("%[^%]+%").matcher(shutdownFormat).find()) {
            shutdownFormat = PlaceholderUtil.replacePlaceholdersToDiscord(shutdownFormat);
        }

        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordSRV - Shutdown").build();
        final ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        try {
            String finalShutdownFormat = shutdownFormat;
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

                // we're no longer ready
                isReady = false;

                // unregister event listeners because of garbage reloading plugins
                HandlerList.unregisterAll(this);

                // shutdown scheduler tasks
                Bukkit.getScheduler().cancelTasks(this);
                for (BukkitWorker activeWorker : Bukkit.getScheduler().getActiveWorkers()) {
                    if (activeWorker.getOwner().equals(this)) {
                        List<String> stackTrace = Arrays.stream(activeWorker.getThread().getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toList());
                        warning("a DiscordSRV scheduler task still active during onDisable: " + stackTrace.remove(0));
                        debug(stackTrace);
                    }
                }

                // stop alerts
                if (alertListener != null) alertListener.unregister();

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
                                debug("JdaFilter removed");
                            }
                        }
                    } catch (Throwable t) {
                        getLogger().warning("Could not remove JDA Filter: " + t.toString());
                    }
                }

                // Clear JDA listeners
                if (jda != null) jda.getEventManager().getRegisteredListeners().forEach(listener -> jda.getEventManager().unregister(listener));

                // send server shutdown message
                DiscordUtil.sendMessageBlocking(getMainTextChannel(), finalShutdownFormat);

                // try to shut down jda gracefully
                if (jda != null) {
                    CompletableFuture<Void> shutdownTask = new CompletableFuture<>();
                    jda.addEventListener(new ListenerAdapter() {
                        @Override
                        public void onShutdown(@NotNull ShutdownEvent event) {
                            shutdownTask.complete(null);
                        }
                    });
                    jda.shutdownNow();
                    jda = null;
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
            error(e);
        }
        executor.shutdownNow();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        Supplier<Boolean> handle = () -> commandManager.handle(sender, args[0], Arrays.stream(args).skip(1).toArray(String[]::new));
        if (!isEnabled()) {
            if (args.length > 0 && args[0].equalsIgnoreCase("debug")) return handle.get(); // allow using debug

            sender.sendMessage(ChatColor.RED + "DiscordSRV is disabled, check your log for errors during DiscordSRV's startup to find out why");
            return true;
        }

        if (args.length == 0) {
            return commandManager.handle(sender, null, new String[] {});
        } else {
            return handle.get();
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command bukkitCommand, @NotNull String alias, String[] args) {
        if (!isEnabled()) return Collections.emptyList();

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
            DiscordSRV.debug(LangUtil.InternalMessage.CHAT_CANCELLATION_DETECTOR_ENABLED.toString());
        }
    }

    public void processChatMessage(Player player, String message, String channel, boolean cancelled) {
        // log debug message to notify that a chat message was being processed
        debug("Chat message received, canceled: " + cancelled + ", channel: " + channel);

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
                .replace("%usernamenoescapes%", DiscordUtil.strip(player.getName()))
                .replace("%world%", player.getWorld().getName())
                .replace("%worldalias%", DiscordUtil.strip(MultiverseCoreHook.getWorldAlias(player.getWorld().getName())));
        discordMessage = PlaceholderUtil.replacePlaceholdersToDiscord(discordMessage, player);

        String displayName = DiscordUtil.strip(player.getDisplayName());
        if (reserializer) {
            message = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.INSTANCE.deserialize(message));
        } else {
            displayName = DiscordUtil.escapeMarkdown(displayName);
        }

        discordMessage = discordMessage
                .replace("%displayname%", displayName)
                .replace("%displaynamenoescapes%", DiscordUtil.strip(player.getDisplayName()))
                .replace("%message%", message);

        for (Map.Entry<Pattern, String> entry : getGameRegexes().entrySet()) {
            discordMessage = entry.getKey().matcher(discordMessage).replaceAll(entry.getValue());
            if (StringUtils.isBlank(discordMessage)) {
                DiscordSRV.debug("Not processing Minecraft message because it was cleared by a filter: " + entry.getKey().pattern());
                return;
            }
        }

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
                DiscordUtil.sendMessage(getOptionalTextChannel("global"), discordMessage);
            } else {
                DiscordUtil.sendMessage(getDestinationTextChannelForGameChannelName(channel), discordMessage);
            }
        } else {
            if (channel == null) channel = getOptionalChannel("global");

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
                message = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.INSTANCE.deserialize(message));
            }

            if (config().getBoolean("DiscordChatChannelTranslateMentions")) message = DiscordUtil.convertMentionsFromNames(message, getMainGuild());

            WebhookUtil.deliverMessage(destinationChannel, player, message);
        }
    }

    public void broadcastMessageToMinecraftServer(String channel, String message, User author) {
        // apply placeholder API values
        Player authorPlayer = null;
        UUID authorLinkedUuid = accountLinkManager.getUuid(author.getId());
        if (authorLinkedUuid != null) authorPlayer = Bukkit.getPlayer(authorLinkedUuid);

        message = PlaceholderUtil.replacePlaceholders(message, authorPlayer);

        if (pluginHooks.size() == 0 || channel == null) {
            for (Player player : PlayerUtil.getOnlinePlayers()) player.sendMessage(message);
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

    /**
     * Triggers a join message for the given player to be sent to Discord. Useful for fake join messages.
     *
     * @param player the player
     * @param joinMessage the join message (that is usually provided by Bukkit's {@link PlayerJoinEvent#getJoinMessage()})
     * @see #sendLeaveMessage(Player, String)
     */
    public void sendJoinMessage(Player player, String joinMessage) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");

        MessageFormat messageFormat = player.hasPlayedBefore()
                ? getMessageFromConfiguration("MinecraftPlayerJoinMessage")
                : getMessageFromConfiguration("MinecraftPlayerFirstJoinMessage");

        TextChannel textChannel = getOptionalTextChannel("join");
        if (textChannel == null) {
            DiscordSRV.debug("Not sending join message, text channel is null");
            return;
        }

        final String displayName = StringUtils.isNotBlank(player.getDisplayName()) ? DiscordUtil.strip(player.getDisplayName()) : "";
        final String message = StringUtils.isNotBlank(joinMessage) ? joinMessage : "";
        final String name = player.getName();
        final String avatarUrl = getEmbedAvatarUrl(player);
        final String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        String botName = getMainGuild() != null ? getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();

        BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
            if (content == null) return null;
            content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%message%", DiscordUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(message) : message))
                    .replace("%username%", needsEscape ? DiscordUtil.escapeMarkdown(name) : name)
                    .replace("%displayname%", needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName)
                    .replace("%usernamenoescapes%", name)
                    .replace("%displaynamenoescapes%", displayName)
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl)
                    .replace("%botname%", botName);
            content = DiscordUtil.translateEmotes(content, textChannel.getGuild());
            content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
            return content;
        };

        Message discordMessage = translateMessage(messageFormat, translator);
        if (discordMessage == null) return;

        String webhookName = translator.apply(messageFormat.getWebhookName(), false);
        String webhookAvatarUrl = translator.apply(messageFormat.getWebhookAvatarUrl(), false);

        if (messageFormat.isUseWebhooks()) {
            WebhookUtil.deliverMessage(textChannel, webhookName, webhookAvatarUrl,
                    discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
        } else {
            DiscordUtil.queueMessage(textChannel, discordMessage, true);
        }
    }

    /**
     * Triggers a leave message for the given player to be sent to Discord. Useful for fake leave messages.
     *
     * @param player the player
     * @param quitMessage the leave/quit message (that is usually provided by Bukkit's {@link PlayerQuitEvent#getQuitMessage()})
     * @see #sendJoinMessage(Player, String)
     */
    public void sendLeaveMessage(Player player, String quitMessage) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");

        MessageFormat messageFormat = getMessageFromConfiguration("MinecraftPlayerLeaveMessage");

        TextChannel textChannel = getOptionalTextChannel("leave");
        if (textChannel == null) {
            DiscordSRV.debug("Not sending quit message, text channel is null");
            return;
        }

        final String displayName = StringUtils.isNotBlank(player.getDisplayName()) ? DiscordUtil.strip(player.getDisplayName()) : "";
        final String message = StringUtils.isNotBlank(quitMessage) ? quitMessage : "";
        final String name = player.getName();

        String avatarUrl = getEmbedAvatarUrl(player);
        String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        String botName = getMainGuild() != null ? getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();

        BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
            if (content == null) return null;
            content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%message%", DiscordUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(message) : message))
                    .replace("%username%", DiscordUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(name) : name))
                    .replace("%displayname%", needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName)
                    .replace("%usernamenoescapes%", name)
                    .replace("%displaynamenoescapes%", displayName)
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl)
                    .replace("%botname%", botName);
            content = DiscordUtil.translateEmotes(content, textChannel.getGuild());
            content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
            return content;
        };

        Message discordMessage = translateMessage(messageFormat, translator);
        if (discordMessage == null) return;

        String webhookName = translator.apply(messageFormat.getWebhookName(), false);
        String webhookAvatarUrl = translator.apply(messageFormat.getWebhookAvatarUrl(), false);

        if (messageFormat.isUseWebhooks()) {
            WebhookUtil.deliverMessage(textChannel, webhookName, webhookAvatarUrl,
                    discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
        } else {
            DiscordUtil.queueMessage(textChannel, discordMessage, true);
        }
    }

    /**
     * Gives an "online" role to a player after joining. The role id can be added in the config.
     *
     * @param player the player
     * @see #removePlayerOnlineRole(Player, String)
     */
    public void givePlayerOnlineRole(Player player, String RoleId) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");

        String DiscordId = accountLinkManager.getDiscordId(player.getUniqueId());
        if (DiscordId == null) {
            getLogger().severe("Online role is enabled, but discord linking is not.");
            return;
        }
        getMainGuild().addRoleToMember(DiscordId, jda.getRoleById(RoleId)).queue();
    }

    /**
     * Removes an "online" role to a player after leaving. The role id can be added in the config.
     *
     * @param player the player
     * @see #givePlayerOnlineRole(Player, String)
     */
    public void removePlayerOnlineRole(Player player, String RoleId) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");

        String DiscordId = accountLinkManager.getDiscordId(player.getUniqueId());
        if (DiscordId == null) {
            getLogger().severe("Online role is enabled, but discord linking is not.");
            return;
        }
        getMainGuild().removeRoleFromMember(DiscordId, jda.getRoleById(RoleId)).queue();
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

        if (config().getOptional(key + ".Embed").isPresent() && config().getOptionalBoolean(key + ".Embed.Enabled").orElse(true)) {
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
                } else {
                    DiscordSRV.debug("Invalid color hex: " + hex + " (in " + key + ".Embed.Color)");
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

        if (config().getOptional(key + ".Webhook").isPresent() && config().getOptionalBoolean(key + ".Webhook.Enable").orElse(false)) {
            messageFormat.setUseWebhooks(true);
            config.getOptionalString(key + ".Webhook.AvatarUrl")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setWebhookAvatarUrl);
            config.getOptionalString(key + ".Webhook.Name")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setWebhookName);
        }

        Optional<String> content = config().getOptionalString(key + ".Content");
        if (content.isPresent() && StringUtils.isNotBlank(content.get())) {
            messageFormat.setContent(content.get());
        }

        return messageFormat.isAnyContent() ? messageFormat : null;
    }

    @CheckReturnValue
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
        if (messageFormat.getFields() != null) messageFormat.getFields().forEach(field ->
                embedBuilder.addField(translator.apply(field.getName(), true), translator.apply(field.getValue(), true), field.isInline()));
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
                .replace("{timestamp}", String.valueOf(System.currentTimeMillis() / 1000))
                .replace("{username}", playerUsername)
                .replace("{uuid}", playerUniqueId != null ? playerUniqueId.toString() : "")
                .replace("{uuid-nodashes}", playerUniqueId != null ? playerUniqueId.toString().replace("-", "") : "")
                .replace("{size}", "128");
        avatarUrl = PlaceholderUtil.replacePlaceholders(avatarUrl, playerUniqueId != null ? Bukkit.getPlayer(playerUniqueId) : null);

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
                .forEach(dynamic -> {
                    String trigger = dynamic.key().convert().intoString();
                    if (StringUtils.isEmpty(trigger)) {
                        DiscordSRV.debug("Skipping canned response with empty trigger");
                        return;
                    }
                    responses.put(trigger, dynamic.convert().intoString());
                });
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
        return System.getenv("LimitFS") != null || System.getProperty("LimitFS") != null
                || !config().getBooleanElse("DiscordConsoleChannelAllowPluginUpload", false);
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

    public String getOptionalChannel(String name) {
        return getChannels().containsKey(name)
                ? name
                : getMainChatChannel();
    }
    public TextChannel getOptionalTextChannel(String gameChannel) {
        return getDestinationTextChannelForGameChannelName(getOptionalChannel(gameChannel));
    }

}
