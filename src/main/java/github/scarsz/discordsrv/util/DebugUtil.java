/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2018 Austin "Scarsz" Shapiro
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

import com.google.common.io.CharStreams;
import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DebugReportedEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class DebugUtil {

    public static String run(String requester) {
        Map<String, String> files = new LinkedHashMap<>();
        try {
            files.put("discordsrv-info.txt", String.join("\n", new String[]{
                    "Requested by " + requester,
                    "",
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
                    "main chat channel: " + DiscordSRV.getPlugin().getMainChatChannelPair(),
                    "discord guild roles: " + (DiscordSRV.getPlugin().getMainGuild() == null ? "invalid main guild" : DiscordSRV.getPlugin().getMainGuild().getRoles().stream().map(Role::toString).collect(Collectors.toList())),
                    "colors: " + DiscordSRV.getPlugin().getColors(),
                    "PlaceholderAPI expansions: " + getInstalledPlaceholderApiExpansions(),
                    "threads:",
                    "    channel topic updater -> alive: " + (DiscordSRV.getPlugin().getChannelTopicUpdater() != null && DiscordSRV.getPlugin().getChannelTopicUpdater().isAlive()),
                    "    console message queue worker -> alive: " + (DiscordSRV.getPlugin().getConsoleMessageQueueWorker() != null && DiscordSRV.getPlugin().getConsoleMessageQueueWorker().isAlive()),
                    "    server watchdog -> alive: " + (DiscordSRV.getPlugin().getServerWatchdog() != null && DiscordSRV.getPlugin().getServerWatchdog().isAlive()),
                    "hooked plugins: " + DiscordSRV.getPlugin().getHookedPlugins()
            }));
            files.put("relevant-lines-from-server.log", getRelevantLinesFromServerLog());
            files.put("config.yml", FileUtils.readFileToString(DiscordSRV.getPlugin().getConfigFile(), Charset.forName("UTF-8")));
            files.put("config-parsed.yml", DiscordSRV.config().getValues(true).entrySet().stream()
                    .map(entry -> {
                        if (entry.getValue() instanceof MemorySection) {
                            return entry.getKey() + ": " + ((MemorySection) entry.getValue()).getValues(true);
                        } else {
                            return entry.getKey() + ": " + entry.getValue();
                        }
                    })
                    .collect(Collectors.joining("\n"))
            );
            files.put("messages.yml", FileUtils.readFileToString(DiscordSRV.getPlugin().getMessagesFile(), Charset.forName("UTF-8")));
            files.put("server-info.txt", getServerInfo());
            files.put("channel-permissions.txt", getChannelPermissions());
            files.put("threads.txt", String.join("\n", new String[]{
                    "current stack:",
                    PrettyUtil.beautify(Thread.currentThread().getStackTrace()),
                    "",
                    "server stack:",
                    PrettyUtil.beautify(getServerThread().getStackTrace())
            }));
            files.put("system-info.txt", getSystemInfo());
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to collect debug information: " + e.getMessage() + ". Check the console for further details.";
        }

        return uploadReport(files, requester);
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
                if (line != null && line.toLowerCase().contains("discordsrv")) output.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return String.join("\n", output);
    }

    private static String getServerInfo() {
        List<String> output = new LinkedList<>();

        List<String> plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(Object::toString).sorted().collect(Collectors.toList());

        output.add("server name: " + DiscordUtil.strip(Bukkit.getServerName()));
        output.add("server motd: " + DiscordUtil.strip(Bukkit.getMotd()));
        output.add("server players: " + PlayerUtil.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        output.add("server plugins: " + plugins);
        output.add("");
        output.add("Minecraft version: " + Bukkit.getVersion());
        output.add("Bukkit API version: " + Bukkit.getBukkitVersion());

        return String.join("\n", output);
    }

    private static String getChannelPermissions() {
        List<String> output = new LinkedList<>();
        DiscordSRV.getPlugin().getChannels().forEach((ingameChannelName, textChannel) -> {
            if (textChannel != null) {
                List<String> outputForChannel = new LinkedList<>();
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_READ)) outputForChannel.add("read");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_WRITE)) outputForChannel.add("write");
                if (DiscordUtil.checkPermission(textChannel, Permission.MANAGE_CHANNEL)) outputForChannel.add("channel-manage");
                if (DiscordUtil.checkPermission(textChannel, Permission.MESSAGE_MANAGE)) outputForChannel.add("message-manage");
                output.add(textChannel + " (<- " + ingameChannelName + "): " + String.join(", ", outputForChannel));
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
        File[] roots = File.listRoots();
        for (File root : roots) {
            output.add("file system " + root.getAbsolutePath());
            output.add("- total space (MB): " + root.getTotalSpace() / 1024 / 1024);
            output.add("- free space (MB): " + root.getFreeSpace() / 1024 / 1024);
            output.add("- usable space (MB): " + root.getUsableSpace() / 1024 / 1024);
        }
        output.add("");

        // system properties
        output.add("System properties:");
        ManagementFactory.getRuntimeMXBean().getSystemProperties().forEach((key, value) -> output.add("    " + key + "=" + value));

        return String.join("\n", output);
    }

    /**
     * Upload the given file map to the current reporting service
     * @param filesToUpload A Map representing a structure of file name & it's contents
     * @param requester Person who requested the debug report
     * @return A user-friendly message of how the report went
     */
    private static String uploadReport(Map<String, String> filesToUpload, String requester) {
        if (filesToUpload.size() == 0) {
            return "ERROR/Failed to collect debug information: files list == 0... How???";
        }

        Map<String, String> files = new LinkedHashMap<>();
        filesToUpload.forEach((fileName, fileContent) -> files.put((files.size() + 1) + "-" + fileName, StringUtils.isNotBlank(fileContent)
                ? fileContent.replace(DiscordSRV.config().getString("BotToken"), "BOT-TOKEN-REDACTED")
                : "blank")
        );

        try {
            // Scarsz debug @ https://debug.scarsz.me
            String url = uploadToDebug(files);
            DiscordSRV.api.callEvent(new DebugReportedEvent(requester, url));
            return url;
        } catch (Exception e) {
            return "ERROR/Failed to send debug report: " + e.getMessage();
        }
    }

    private static String uploadToDebug(Map<String, String> files) {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL("https://debug.scarsz.me/post").openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "DiscordSRV/" + DiscordSRV.getPlugin().getDescription().getVersion());
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            OutputStream out = connection.getOutputStream();
            JsonObject payload = new JsonObject();
            payload.addProperty("description", "DiscordSRV Debug Report");

            JsonObject filesJson = new JsonObject();
            files.forEach((fileName, fileContent) -> {
                JsonObject file = new JsonObject();
                file.addProperty("content", fileContent);
                filesJson.add(fileName, file);
            });
            payload.add("files", filesJson);

            out.write(DiscordSRV.getPlugin().getGson().toJson(payload).getBytes(Charset.forName("UTF-8")));
            out.close();

            String rawOutput = CharStreams.toString(new InputStreamReader(connection.getInputStream()));
            connection.getInputStream().close();
            JsonObject output = DiscordSRV.getPlugin().getGson().fromJson(rawOutput, JsonObject.class);

            if (!output.has("url")) throw new RuntimeException("URL was not received, reporting failed");
            return output.get("url").getAsString();
        } catch (Exception e) {
            if (connection != null) connection.disconnect();
            throw new RuntimeException(e);
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

}
