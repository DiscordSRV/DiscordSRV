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

package github.scarsz.discordsrv.util;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DebugReportedEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DebugUtil {

    public static final List<String> SENSITIVE_OPTIONS = Arrays.asList(
            "BotToken", "Experiment_JdbcAccountLinkBackend", "Experiment_JdbcUsername", "Experiment_JdbcPassword"
    );

    public static String run(String requester) {
        return run(requester, 256);
    }

    public static String run(String requester, int aesBits) {
        List<Map<String, String>> files = new LinkedList<>();
        try {
            files.add(fileMap("discordsrv-info.txt", "general information about the plugin", String.join("\n", new String[]{
                    getRandomPhrase(),
                    "",
                    "plugin version: " + DiscordSRV.getPlugin(),
                    "config version: " + DiscordSRV.config().getString("ConfigVersion"),
                    "build date: " + ManifestUtil.getManifestValue("Build-Date"),
                    "build git revision: " + ManifestUtil.getManifestValue("Git-Revision"),
                    "build number: " + ManifestUtil.getManifestValue("Build-Number"),
                    "build origin: " + ManifestUtil.getManifestValue("Build-Origin"),
                    "jda status: " + (DiscordUtil.getJda() != null && DiscordUtil.getJda().getStatus() != null && DiscordUtil.getJda().getPing() != -1 ? DiscordUtil.getJda().getStatus().name() + " / " + DiscordUtil.getJda().getPing() + "ms" : "build not finished"),
                    "channels: " + DiscordSRV.getPlugin().getChannels(),
                    "console channel: " + DiscordSRV.getPlugin().getConsoleChannel(),
                    "main chat channel: " + DiscordSRV.getPlugin().getMainChatChannel() + " -> " + DiscordSRV.getPlugin().getMainTextChannel(),
                    "discord guild roles: " + (DiscordSRV.getPlugin().getMainGuild() == null ? "invalid main guild" : DiscordSRV.getPlugin().getMainGuild().getRoles().stream().map(Role::toString).collect(Collectors.toList())),
                    "colors: " + DiscordSRV.getPlugin().getColors(),
                    "PlaceholderAPI expansions: " + getInstalledPlaceholderApiExpansions(),
                    "threads:",
                    "    channel topic updater -> alive: " + (DiscordSRV.getPlugin().getChannelTopicUpdater() != null && DiscordSRV.getPlugin().getChannelTopicUpdater().isAlive()),
                    "    console message queue worker -> alive: " + (DiscordSRV.getPlugin().getConsoleMessageQueueWorker() != null && DiscordSRV.getPlugin().getConsoleMessageQueueWorker().isAlive()),
                    "    server watchdog -> alive: " + (DiscordSRV.getPlugin().getServerWatchdog() != null && DiscordSRV.getPlugin().getServerWatchdog().isAlive()),
                    "hooked plugins: " + DiscordSRV.getPlugin().getHookedPlugins()
            })));
            files.add(fileMap("relevant-lines-from-server.log", "lines from the server console containing \"discordsrv\"", getRelevantLinesFromServerLog()));
            files.add(fileMap("config.yml", "raw plugins/DiscordSRV/config.yml", FileUtils.readFileToString(DiscordSRV.getPlugin().getConfigFile(), Charset.forName("UTF-8"))));
            files.add(fileMap("config-parsed.yml", "parsed plugins/DiscordSRV/config.yml", DiscordSRV.config().getValues(true).entrySet().stream()
                    .map(entry -> {
                        if (entry.getValue() instanceof MemorySection) {
                            return entry.getKey() + ": " + ((MemorySection) entry.getValue()).getValues(true);
                        } else {
                            return entry.getKey() + ": " + entry.getValue();
                        }
                    })
                    .collect(Collectors.joining("\n"))
            ));
            files.add(fileMap("messages.yml", "raw plugins/DiscordSRV/messages.yml", FileUtils.readFileToString(DiscordSRV.getPlugin().getMessagesFile(), Charset.forName("UTF-8"))));
            files.add(fileMap("server-info.txt", null, getServerInfo()));
            files.add(fileMap("channel-permissions.txt", null, getChannelPermissions()));
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

    private static String getRandomPhrase() {
        return DiscordSRV.getPlugin().getRandomPhrases().size() > 0
                ? DiscordSRV.getPlugin().getRandomPhrases().get(DiscordSRV.getPlugin().getRandom().nextInt(DiscordSRV.getPlugin().getRandomPhrases().size()))
                : "";
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

    private static String getChannelPermissions() {
        List<String> output = new LinkedList<>();
        DiscordSRV.getPlugin().getChannels().forEach((channel, textChannelId) -> {
            TextChannel textChannel = textChannelId != null ? DiscordSRV.getPlugin().getJda().getTextChannelById(textChannelId) : null;
            if (textChannel != null) {
                List<String> outputForChannel = new LinkedList<>();
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_READ)) outputForChannel.add("read");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_WRITE)) outputForChannel.add("write");
                if (DiscordUtil.checkPermission(textChannel, Permission.MANAGE_CHANNEL)) outputForChannel.add("channel-manage");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_MANAGE)) outputForChannel.add("message-manage");
                output.add(channel + " -> " + textChannelId + " [" + String.join(", ", outputForChannel) + "]");
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
        output.add("server directory " + serverRoot.getAbsolutePath());
        output.add("- total space (MB): " + serverRoot.getTotalSpace() / 1024 / 1024);
        output.add("- free space (MB): " + serverRoot.getFreeSpace() / 1024 / 1024);
        output.add("- usable space (MB): " + serverRoot.getUsableSpace() / 1024 / 1024);
        output.add("");

        // system properties
        output.add("System properties:");
        ManagementFactory.getRuntimeMXBean().getSystemProperties().forEach((key, value) -> output.add("    " + key + "=" + value));

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

        files.forEach(map -> {
            String content = map.get("content");
            if (StringUtils.isNotBlank(content)) {
                // remove sensitive options from files
                for (String option : DebugUtil.SENSITIVE_OPTIONS) {
                    String value = DiscordSRV.config().getString(option);
                    if (StringUtils.isNotBlank(value)) {
                        content = content.replace(value, "REDACTED");
                    }
                }
            } else {
                // put "blank" for null file contents
                content = "blank";
            }
            map.put("content", content);
        });

        try {
            String url = uploadToBin("https://bin.scarsz.me", aesBits, files, "Requested by " + requester);
            DiscordSRV.api.callEvent(new DebugReportedEvent(requester, url));
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR/Failed to send debug report: " + e.getMessage();
        }
    }

    private static final Gson GSON = new Gson();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static String uploadToBin(String binHost, int aesBits, List<Map<String, String>> files, String description) {
        String key = RandomStringUtils.randomAlphanumeric(aesBits == 256 ? 32 : 16);
        byte[] keyBytes = key.getBytes();

        // decode to bytes, encrypt, base64
        for (Map<String, String> file : files) {
            file.entrySet().removeIf(entry -> StringUtils.isBlank(entry.getValue()));
            for (String mapKey : file.keySet()) {
                file.put(mapKey, b64(encrypt(keyBytes, file.get(mapKey))));
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", b64(encrypt(keyBytes, description)));
        payload.put("expiration", TimeUnit.DAYS.toMinutes(1));
        payload.put("files", files);
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
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
