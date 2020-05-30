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

package github.scarsz.discordsrv.util;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import github.scarsz.configuralize.Language;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DebugReportedEvent;
import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.hooks.VaultHook;
import github.scarsz.discordsrv.hooks.chat.ChatHook;
import github.scarsz.discordsrv.hooks.chat.TownyChatHook;
import github.scarsz.discordsrv.modules.voice.VoiceModule;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.RegisteredListener;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DebugUtil {

    public static final List<String> SENSITIVE_OPTIONS = Arrays.asList(
            "BotToken", "Experiment_JdbcAccountLinkBackend", "Experiment_JdbcUsername", "Experiment_JdbcPassword"
    );
    public static boolean disabledOnce = false;

    public static String run(String requester) {
        return run(requester, 256);
    }

    public static String run(String requester, int aesBits) {
        List<Map<String, String>> files = new LinkedList<>();
        try {
            files.add(fileMap("debug-info.txt", "Potential issues in the installation", getDebugInformation()));
            files.add(fileMap("discordsrv-info.txt", "general information about the plugin", String.join("\n", new String[]{
                    "plugin version: " + DiscordSRV.getPlugin(),
                    "config version: " + DiscordSRV.config().getString("ConfigVersion"),
                    "build date: " + ManifestUtil.getManifestValue("Build-Date"),
                    "build git revision: " + ManifestUtil.getManifestValue("Git-Revision"),
                    "build number: " + ManifestUtil.getManifestValue("Build-Number"),
                    "build origin: " + ManifestUtil.getManifestValue("Build-Origin"),
                    "jda status: " + (DiscordUtil.getJda() != null && DiscordUtil.getJda().getGatewayPing() != -1 ? DiscordUtil.getJda().getStatus().name() + " / " + DiscordUtil.getJda().getGatewayPing() + "ms" : "build not finished"),
                    "channels: " + DiscordSRV.getPlugin().getChannels(),
                    "console channel: " + DiscordSRV.getPlugin().getConsoleChannel(),
                    "main chat channel: " + DiscordSRV.getPlugin().getMainChatChannel() + " -> " + DiscordSRV.getPlugin().getMainTextChannel(),
                    "discord guild roles: " + (DiscordSRV.getPlugin().getMainGuild() == null ? "invalid main guild" : DiscordSRV.getPlugin().getMainGuild().getRoles().stream().map(Role::toString).collect(Collectors.toList())),
                    "vault groups: " + Arrays.toString(VaultHook.getGroups()),
                    "PlaceholderAPI expansions: " + getInstalledPlaceholderApiExpansions(),
                    "/discord command executor: " + (Bukkit.getServer().getPluginCommand("discord") != null ? Bukkit.getServer().getPluginCommand("discord").getPlugin() : ""),
                    "threads:",
                    "    channel topic updater -> alive: " + (DiscordSRV.getPlugin().getChannelTopicUpdater() != null && DiscordSRV.getPlugin().getChannelTopicUpdater().isAlive()),
                    "    console message queue worker -> alive: " + (DiscordSRV.getPlugin().getConsoleMessageQueueWorker() != null && DiscordSRV.getPlugin().getConsoleMessageQueueWorker().isAlive()),
                    "    server watchdog -> alive: " + (DiscordSRV.getPlugin().getServerWatchdog() != null && DiscordSRV.getPlugin().getServerWatchdog().isAlive()),
                    "hooked plugins: " + DiscordSRV.getPlugin().getPluginHooks().stream().map(PluginHook::getPlugin).map(Object::toString).collect(Collectors.joining(", "))
            })));
            files.add(fileMap("relevant-lines-from-server.log", "lines from the server console containing \"discordsrv\"", getRelevantLinesFromServerLog()));
            files.add(fileMap("config.yml", "raw plugins/DiscordSRV/config.yml", FileUtils.readFileToString(DiscordSRV.getPlugin().getConfigFile(), StandardCharsets.UTF_8)));
            files.add(fileMap("config-parsed.yml", "parsed plugins/DiscordSRV/config.yml", DiscordSRV.config().getProvider("config").getValues().allChildren()
                    .map(child -> {
                        long childCount = child.allChildren().count();
                        if (childCount == 0) {
                            return child.key().asObject() + ": " + child.asObject();
                        } else {
                            return child.key().asString() + ": " + child.allChildren()
                                    .map(dynamic -> "- " + dynamic.asObject().toString())
                                    .collect(Collectors.joining(", "));
                        }
                    })
                    .collect(Collectors.joining("\n"))
            ));
            files.add(fileMap("messages.yml", "raw plugins/DiscordSRV/messages.yml", FileUtils.readFileToString(DiscordSRV.getPlugin().getMessagesFile(), StandardCharsets.UTF_8)));
            files.add(fileMap("voice.yml", "raw plugins/DiscordSRV/voice.yml", FileUtils.readFileToString(DiscordSRV.config().getProvider("voice").getSource().getFile(), StandardCharsets.UTF_8)));
            files.add(fileMap("linking.yml", "raw plugins/DiscordSRV/linking.yml", FileUtils.readFileToString(DiscordSRV.config().getProvider("linking").getSource().getFile(), StandardCharsets.UTF_8)));
            files.add(fileMap("synchronization.yml", "raw plugins/DiscordSRV/synchronization.yml", FileUtils.readFileToString(DiscordSRV.config().getProvider("synchronization").getSource().getFile(), StandardCharsets.UTF_8)));
            files.add(fileMap("server-info.txt", null, getServerInfo()));
            files.add(fileMap("registered-listeners.txt", "list of registered listeners for Bukkit events DiscordSRV uses", getRegisteredListeners()));
            files.add(fileMap("permissions.txt", null, getPermissions()));
            files.add(fileMap("threads.txt", null, String.join("\n", new String[]{
                    "current stack:",
                    PrettyUtil.beautify(Thread.currentThread().getStackTrace()),
                    "",
                    "server stack:",
                    PrettyUtil.beautify(getServerThread().getStackTrace())
            })));
            files.add(fileMap("system-info.txt", null, getSystemInfo()));
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to collect debug information: " + e.getMessage() + ". Check the console for further details.";
        }

        return uploadReport(files, aesBits, requester);
    }

    private static Map<String, String> fileMap(String name, String description, String content) {
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("content", content);
        map.put("type", "text/plain");
        return map;
    }

    private static Thread getServerThread() {
        return Thread.getAllStackTraces().keySet().stream().filter(thread -> thread.getName().equals("Server thread")).collect(Collectors.toList()).get(0);
    }

    private static String getInstalledPlaceholderApiExpansions() {
        if (!PluginUtil.pluginHookIsEnabled("placeholderapi")) return "PlaceholderAPI not hooked/no expansions installed";
        File[] extensionFiles = new File(DiscordSRV.getPlugin().getDataFolder().getParentFile(), "PlaceholderAPI/expansions").listFiles();
        if (extensionFiles == null) return "PlaceholderAPI/expansions is not directory/IO error";
        return Arrays.stream(extensionFiles).map(File::getName).collect(Collectors.joining(", "));
    }

    private static String getRelevantLinesFromServerLog() {
        List<String> output = new LinkedList<>();
        try {
            FileReader fr = new FileReader(new File("logs/latest.log"));
            BufferedReader br = new BufferedReader(fr);
            boolean done = false;
            while (!done) {
                String line = br.readLine();
                if (line == null) done = true;
                if (line != null && line.toLowerCase().contains("discordsrv")) output.add(DiscordUtil.aggressiveStrip(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return String.join("\n", output);
    }

    private static String getServerInfo() {
        List<String> output = new LinkedList<>();

        List<String> plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(Object::toString).sorted().collect(Collectors.toList());

        output.add("server players: " + PlayerUtil.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        output.add("server plugins: " + plugins);
        output.add("");
        output.add("Minecraft version: " + Bukkit.getVersion());
        output.add("Bukkit API version: " + Bukkit.getBukkitVersion());

        return String.join("\n", output);
    }

    private static String getDebugInformation() {
        List<Message> messages = new ArrayList<>();

        if (DiscordUtil.getJda() == null) {
            messages.add(new Message(Message.Type.NOT_CONNECTED));
        } else if (DiscordUtil.getJda().getGuilds().isEmpty()) {
            messages.add(new Message(Message.Type.NOT_IN_ANY_SERVERS));
        }

        if (DiscordSRV.getPlugin().getMainTextChannel() == null) {
            if (DiscordSRV.getPlugin().getConsoleChannel() == null) {
                messages.add(new Message(Message.Type.NO_CHANNELS_LINKED));
            } else {
                messages.add(new Message(Message.Type.NO_CHAT_CHANNELS_LINKED));
            }
        }

        for (Map.Entry<String, String> entry : DiscordSRV.getPlugin().getChannels().entrySet()) {
            TextChannel textChannel = DiscordUtil.getTextChannelById(entry.getValue());
            if (textChannel == null) {
                messages.add(new Message(Message.Type.INVALID_CHANNEL, "{" + entry.getKey() + ":" + entry.getValue() + "}"));
                continue;
            }

            if (textChannel.getName().equals(entry.getKey())) {
                messages.add(new Message(Message.Type.SAME_CHANNEL_NAME, entry.getKey()));
            }
            if (textChannel.equals(DiscordSRV.getPlugin().getConsoleChannel())) {
                messages.add(new Message(Message.Type.CONSOLE_AND_CHAT_SAME_CHANNEL));
            }
        }

        if (DiscordSRV.getPlugin().getChannels().size() > 1 && DiscordSRV.getPlugin().getPluginHooks().stream().noneMatch(hook -> hook instanceof ChatHook) && !DiscordSRV.api.isAnyHooked()) {
            messages.add(new Message(Message.Type.MULTIPLE_CHANNELS_NO_HOOKS));
        }

        if (PluginUtil.pluginHookIsEnabled("TownyChat")) {
            try {
                String mainChannelName = TownyChatHook.getMainChannelName();
                if (mainChannelName != null && !DiscordSRV.getPlugin().getChannels().containsKey(mainChannelName)) {
                    messages.add(new Message(Message.Type.NO_TOWNY_MAIN_CHANNEL, mainChannelName));
                }
            } catch (Throwable ignored) {
                // didn't work
            }
        }

        if (!DiscordSRV.config().getBoolean("RespectChatPlugins")) {
            messages.add(new Message(Message.Type.RESPECT_CHAT_PLUGINS));
        }

        if (DiscordSRV.config().getInt("DebugLevel") == 0) {
            messages.add(new Message(Message.Type.DEBUG_MODE_NOT_ENABLED));
        }

        if (DiscordSRV.updateIsAvailable) {
            messages.add(new Message(Message.Type.UPDATE_AVAILABLE));
        } else if (!DiscordSRV.updateChecked || DiscordSRV.isUpdateCheckDisabled()) {
            messages.add(new Message(Message.Type.UPDATE_CHECK_DISABLED));
        }
        
        StringBuilder stringBuilder = new StringBuilder();
        if (messages.isEmpty()) {
            stringBuilder.append("No issues detected automatically");
        } else {
            messages.stream().sorted((one, two) -> Boolean.compare(one.isWarning(), two.isWarning())).forEach(message ->
                    stringBuilder.append(message.isWarning() ? "[Warn] " : "[Error] ").append(message.getMessage()).append("\n"));
        }

        return stringBuilder.toString();
    }

    private static String getRegisteredListeners() {
        List<String> output = new LinkedList<>();

        List<Class<?>> listenedClasses = new ArrayList<>(Arrays.asList(
                AsyncPlayerChatEvent.class,
                PlayerJoinEvent.class,
                PlayerQuitEvent.class,
                PlayerDeathEvent.class,
                AsyncPlayerPreLoginEvent.class,
                PlayerLoginEvent.class
        ));

        try {
            Class.forName("org.bukkit.event.player.PlayerAdvancementDoneEvent");
            listenedClasses.add(org.bukkit.event.player.PlayerAdvancementDoneEvent.class);
        } catch (ClassNotFoundException ignored) {
            //noinspection deprecation
            listenedClasses.add(org.bukkit.event.player.PlayerAchievementAwardedEvent.class);
        }

        for (Class<?> listenedClass : listenedClasses) {
            try {
                Class<?> effectiveClass = null;
                Method getHandlerList;
                try {
                    getHandlerList = listenedClass.getDeclaredMethod("getHandlerList");
                } catch (NoSuchMethodException ignored) {
                    // Try super class
                    Class<?> superClass = listenedClass.getSuperclass();
                    getHandlerList = superClass.getDeclaredMethod("getHandlerList");
                    effectiveClass = superClass;
                }

                HandlerList handlerList = (HandlerList) getHandlerList.invoke(null);
                List<RegisteredListener> registeredListeners = Arrays.stream(handlerList.getRegisteredListeners())
                        .filter(registeredListener -> !registeredListener.getPlugin().getName().equalsIgnoreCase("DiscordSRV"))
                        .sorted(Comparator.comparing(RegisteredListener::getPriority)).collect(Collectors.toList());

                if (registeredListeners.isEmpty()) {
                    output.add("No " + listenedClass + " listeners registered.");
                } else {
                    output.add("Registered " + listenedClass.getSimpleName() +
                            (effectiveClass != null ? " (" + effectiveClass.getSimpleName() + ")" : "")
                            + " listeners (" + registeredListeners.size() + "):");

                    for (RegisteredListener registeredListener : registeredListeners) {
                        output.add(" - " + registeredListener.getPlugin().getName()
                                + ": " + registeredListener.getListener().getClass().getName()
                                + " at " + registeredListener.getPriority());
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                output.add("Error with " + listenedClass.getSimpleName() + ": " + e.getClass().getName() + ": " + e.getMessage());
            }
            output.add("");
        }

        return String.join("\n", output);
    }

    private static String getPermissions() {
        List<String> output = new LinkedList<>();

        if (DiscordUtil.getJda() == null) {
            return "JDA == null";
        }

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
        if (mainGuild == null) {
            output.add("main guild -> null");
        } else {
            List<String> guildPermissions = new ArrayList<>();
            if (DiscordUtil.checkPermission(mainGuild, Permission.ADMINISTRATOR)) guildPermissions.add("administrator");
            if (DiscordUtil.checkPermission(mainGuild, Permission.MANAGE_ROLES)) guildPermissions.add("manage-roles");
            if (DiscordUtil.checkPermission(mainGuild, Permission.NICKNAME_MANAGE)) guildPermissions.add("nickname-manage");
            if (DiscordUtil.checkPermission(mainGuild, Permission.MANAGE_WEBHOOKS)) guildPermissions.add("manage-webhooks");
            output.add("main guild -> " + mainGuild + " [" + String.join(", ", guildPermissions) + "]");
        }

        VoiceChannel lobbyChannel = VoiceModule.getLobbyChannel();
        if (lobbyChannel == null) {
            output.add("voice lobby -> null");
        } else {
            List<String> channelPermissions = new ArrayList<>();
            if (DiscordUtil.checkPermission(lobbyChannel, Permission.VOICE_MOVE_OTHERS)) channelPermissions.add("move-members");
            output.add("voice lobby -> " + lobbyChannel + " [" + String.join(", ", channelPermissions) + "]");

            Category category = lobbyChannel.getParent();
            if (category == null) {
                output.add("voice category -> null");
            } else {
                List<String> categoryPermissions = new ArrayList<>();
                if (DiscordUtil.checkPermission(category, Permission.VOICE_MOVE_OTHERS)) categoryPermissions.add("move-members");
                if (DiscordUtil.checkPermission(category, Permission.MANAGE_CHANNEL)) categoryPermissions.add("manage-channel");
                if (DiscordUtil.checkPermission(category, Permission.MANAGE_PERMISSIONS)) categoryPermissions.add("manage-permissions");
                output.add("voice category -> " + category + " [" + String.join(", ", categoryPermissions) + "]");
            }
        }

        TextChannel consoleChannel = DiscordSRV.getPlugin().getConsoleChannel();
        if (consoleChannel == null) {
            output.add("console channel -> null");
        } else {
            List<String> consolePermissions = new ArrayList<>();
            if (DiscordUtil.checkPermission(consoleChannel, Permission.MESSAGE_READ)) consolePermissions.add("read");
            if (DiscordUtil.checkPermission(consoleChannel, Permission.MESSAGE_WRITE)) consolePermissions.add("write");
            if (DiscordUtil.checkPermission(consoleChannel, Permission.MANAGE_CHANNEL)) consolePermissions.add("channel-manage");
            output.add("console channel -> " + consoleChannel + " [" + String.join(", ", consolePermissions) + "]");
        }

        DiscordSRV.getPlugin().getChannels().forEach((channel, textChannelId) -> {
            TextChannel textChannel = StringUtils.isNotBlank(textChannelId) ? DiscordSRV.getPlugin().getJda().getTextChannelById(textChannelId) : null;
            if (textChannel != null) {
                List<String> outputForChannel = new LinkedList<>();
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_READ)) outputForChannel.add("read");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_WRITE)) outputForChannel.add("write");
                if (DiscordUtil.checkPermission(textChannel, Permission.MANAGE_CHANNEL)) outputForChannel.add("channel-manage");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_MANAGE)) outputForChannel.add("message-manage");
                if (DiscordUtil.checkPermission(textChannel, Permission.MANAGE_WEBHOOKS)) outputForChannel.add("manage-webhooks");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_ADD_REACTION)) outputForChannel.add("add-reactions");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_HISTORY)) outputForChannel.add("history");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_ATTACH_FILES)) outputForChannel.add("attach-files");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_MENTION_EVERYONE)) outputForChannel.add("mention-everyone");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_EXT_EMOJI)) outputForChannel.add("external-emotes");
                output.add(channel + " -> " + textChannel + " [" + String.join(", ", outputForChannel) + "]");
            } else {
                output.add(channel + " -> null");
            }
        });

        return String.join("\n", output);
    }

    private static String getSystemInfo() {
        List<String> output = new LinkedList<>();

        // total number of processors or cores available to the JVM
        output.add("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
        output.add("");

        // memory
        output.add("Free memory for JVM (MB): " + Runtime.getRuntime().freeMemory() / 1024 / 1024);
        output.add("Maximum memory for JVM (MB): " + (Runtime.getRuntime().maxMemory() == Long.MAX_VALUE ? "no limit" : Runtime.getRuntime().maxMemory() / 1024 / 1024));
        output.add("Total memory available for JVM (MB): " + Runtime.getRuntime().totalMemory() / 1024 / 1024);
        output.add("");

        // drive space
        File serverRoot = DiscordSRV.getPlugin().getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
        output.add("Server storage:");
        output.add("- total space (MB): " + serverRoot.getTotalSpace() / 1024 / 1024);
        output.add("- free space (MB): " + serverRoot.getFreeSpace() / 1024 / 1024);
        output.add("- usable space (MB): " + serverRoot.getUsableSpace() / 1024 / 1024);
        output.add("");

        // java version
        Map<String, String> systemProperties = ManagementFactory.getRuntimeMXBean().getSystemProperties();
        output.add("Java version: " + systemProperties.get("java.version"));
        output.add("Java vendor: " + systemProperties.get("java.vendor") + " " + systemProperties.get("java.vendor.url"));
        output.add("Java home: " + systemProperties.get("java.home"));
        output.add("Command line: " + systemProperties.get("sun.java.command"));
        output.add("Time zone: " + systemProperties.get("user.timezone"));

        return String.join("\n", output);
    }

    /**
     * Upload the given file map to the current reporting service
     * @param files A Map representing a structure of file name & it's contents
     * @param requester Person who requested the debug report
     * @return A user-friendly message of how the report went
     */
    private static String uploadReport(List<Map<String, String>> files, int aesBits, String requester) {
        if (files.size() == 0) {
            return "ERROR/Failed to collect debug information: files list == 0... How???";
        }

        // Remove sensitive data and set the file content to "blank" if the file is blank
        files.forEach(map -> {
            String content = map.get("content");
            if (StringUtils.isNotBlank(content)) {
                // remove sensitive options from files
                for (String option : DebugUtil.SENSITIVE_OPTIONS) {
                    String value = DiscordSRV.config().getString(option);
                    if (StringUtils.isNotBlank(value) && !value.equalsIgnoreCase("username")) {
                        content = content.replace(value, "REDACTED");
                    }
                }

                // extra regex replace for bot tokens
                content = content.replaceAll("[MN][A-Za-z\\d]{23}\\.[\\w-]{6}\\.[\\w-]{27}", "REDACTED");
            } else {
                // put "blank" for null file contents
                content = "blank";
            }
            map.put("content", content);
        });

        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordSRV - Debug Report Upload").build();
        final ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        try {
            return executor.invokeAny(Collections.singletonList(() -> {
                try {
                    String url = uploadToBin("https://bin.scarsz.me", aesBits, files, "Requested by " + requester);
                    DiscordSRV.api.callEvent(new DebugReportedEvent(requester, url));
                    return url;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }), 20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            DiscordSRV.error("Interrupted while uploading a debug report");
            return "ERROR/Interrupted while uploading the debug report";
        } catch (ExecutionException | TimeoutException e) {
            if (e instanceof ExecutionException && e.getCause().getMessage().toLowerCase().contains("illegal key size")) {
                return "ERROR/" + e.getCause().getMessage() + ". Try using /discordsrv debug 128";
            }

            File debugFolder = DiscordSRV.getPlugin().getDebugFolder();
            if (!debugFolder.exists()) debugFolder.mkdir();

            String debugName = "debug-" + System.currentTimeMillis() + ".zip";
            File zipFile = new File(debugFolder, debugName);

            try {
                ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));
                for (Map<String, String> file : files) {
                    zipOutputStream.putNextEntry(new ZipEntry(file.get("name")));

                    byte[] data = file.get("content").getBytes();
                    zipOutputStream.write(data, 0, data.length);
                    zipOutputStream.closeEntry();
                }

                zipOutputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                return "ERROR/Failed to upload to bin, and write to disk. (Unable to store debug report). Caused by "
                        + e.getCause().getMessage() + " and " + ex.getClass().getName() + ": " + ex.getMessage();
            }

            return "GENERATED TO FILE/Failed to upload to bin.scarsz.me, placed into plugins/DiscordSRV/debug/" + debugName
                    + ". Caused by " + (e instanceof ExecutionException ? e.getCause().getMessage() : e.getMessage());
        }
    }

    private static final Gson GSON = new Gson();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static String uploadToBin(String binHost, int aesBits, List<Map<String, String>> files, String description) {
        String key = RandomStringUtils.randomAlphanumeric(aesBits == 256 ? 32 : 16);
        byte[] keyBytes = key.getBytes();

        // decode to bytes, encrypt, base64
        List<Map<String, String>> encryptedFiles = new ArrayList<>();
        for (Map<String, String> file : files) {
            Map<String, String> encryptedFile = new HashMap<>(file);
            encryptedFile.entrySet().removeIf(entry -> StringUtils.isBlank(entry.getValue()));
            encryptedFile.replaceAll((k, v) -> b64(encrypt(keyBytes, file.get(k))));
            encryptedFiles.add(encryptedFile);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", b64(encrypt(keyBytes, description)));
        payload.put("expiration", TimeUnit.DAYS.toMinutes(1));
        payload.put("files", encryptedFiles);
        HttpRequest request = HttpRequest.post(binHost + "/v1/post")
                .userAgent("DiscordSRV " + DiscordSRV.version)
                .send(GSON.toJson(payload));
        if (request.code() == 200) {
            Map json = GSON.fromJson(request.body(), Map.class);
            if (json.get("status").equals("ok")) {
                return binHost + "/" + json.get("bin") + "#" + key;
            } else {
                String reason = "";
                if (json.containsKey("error")) {
                    Map error = (Map) json.get("error");
                    reason = ": " + error.get("type") + " " + error.get("message");
                }
                throw new RuntimeException("Bin upload status wasn't ok" + reason);
            }
        } else {
            throw new RuntimeException("Got bad HTTP status from Bin: " + request.code());
        }
    }

    public static String getStackTrace() {
        List<String> stackTrace = new LinkedList<>();
        stackTrace.add("Stack trace @ debug call (THIS IS NOT AN ERROR)");
        Arrays.stream(ExceptionUtils.getStackTrace(new Throwable()).split("\n"))
                .filter(s -> s.toLowerCase().contains("discordsrv"))
                .filter(s -> !s.contains("DebugUtil.getStackTrace"))
                .forEach(stackTrace::add);
        return String.join("\n", stackTrace);
    }

    public static String b64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Encrypt the given `data` UTF-8 String with the given `key` (16 bytes, 128-bit)
     * @param key the key to encrypt data with
     * @param data the UTF-8 string to encrypt
     * @return the randomly generated IV + the encrypted data with no separator ([iv..., encryptedData...])
     */
    public static byte[] encrypt(byte[] key, String data) {
        return encrypt(key, data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encrypt the given `data` byte array with the given `key` (16 bytes, 128-bit)
     * @param key the key to encrypt data with
     * @param data the data to encrypt
     * @return the randomly generated IV + the encrypted data with no separator ([iv..., encryptedData...])
     */
    public static byte[] encrypt(byte[] key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            byte[] iv = new byte[cipher.getBlockSize()];
            RANDOM.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(data);
            return ArrayUtils.addAll(iv, encrypted);
        } catch (InvalidKeyException e) {
            if (e.getMessage().toLowerCase().contains("illegal key size")) {
                throw new RuntimeException(e.getMessage(), e);
            } else {
                e.printStackTrace();
            }
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static class Message {

        private final Type type;
        private final String[] args;

        public Message(Type type, String... args) {
            this.type = type;
            this.args = args;
        }

        private boolean isWarning() {
            return type.warning;
        }

        @SuppressWarnings("RedundantCast") // it in fact isn't
        public String getMessage() {
            return String.format(type.message, (Object[]) args);
        }

        public String getTypeName() {
            return type.toString();
        }

        public enum Type {
            // Warnings
            NO_CHAT_CHANNELS_LINKED(true, "No chat channels linked"),
            NO_CHANNELS_LINKED(true, "No channels linked (chat & console)"),
            SAME_CHANNEL_NAME(true, "Channel %s has the same in-game and Discord channel name"),
            MULTIPLE_CHANNELS_NO_HOOKS(true, "Multiple chat channels, but no (chat) plugin hooks"),
            RESPECT_CHAT_PLUGINS(true, "You have RespectChatPlugins set to false. This means DiscordSRV will completely ignore " +
                    "any other plugin's attempts to cancel a chat message from being broadcasted to the server. " +
                    "Disabling this is NOT a valid solution to your chat messages not being sent to Discord."
            ),
            UPDATE_CHECK_DISABLED(true, "Update checking is disabled"),
            RELOADED(true, "DiscordSRV has been reloaded (has already disabled once)"),

            // Errors
            INVALID_CHANNEL(false, "Invalid Channel %s (not found)"),
            NO_TOWNY_MAIN_CHANNEL(false, "No channel hooked to Towny's default channel: %s"),
            CONSOLE_AND_CHAT_SAME_CHANNEL(false, LangUtil.InternalMessage.CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL.getDefinitions().get(Language.EN)),
            NOT_IN_ANY_SERVERS(false, LangUtil.InternalMessage.BOT_NOT_IN_ANY_SERVERS.getDefinitions().get(Language.EN)),
            NOT_CONNECTED(false, "Not connected to Discord!"),
            DEBUG_MODE_NOT_ENABLED(false, "You do not have debug mode on. Set DebugLevel to 1 in config.yml, run /discordsrv reload, " +
                    "try to reproduce your problem and create another debug report."
            ),
            UPDATE_AVAILABLE(false, "Update available. Download: https://get.discordsrv.com");

            private final boolean warning;
            private final String message;

            Type(boolean warning, String message) {
                this.warning = warning;
                this.message = message;
            }
        }
    }

}
