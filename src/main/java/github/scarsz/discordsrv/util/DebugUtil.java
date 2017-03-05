package github.scarsz.discordsrv.util;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.Permission;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 6:21 PM
 */
public class DebugUtil {

    private static Gson gson = new Gson();

    public static String run(String requester) {
        Map<String, String> files = new LinkedHashMap<String, String>() {{
            try {
                put("discordsrv-info.txt", String.join("\n", new String[] {
                        "Requested by " + requester,
                        "",
                        getRandomPhrase(),
                        "",
                        "plugin version: " + DiscordSRV.getPlugin(),
                        "config version: " + DiscordSRV.getPlugin().getConfig().getString("ConfigVersion"),
                        "build date: " + ManifestUtil.getManifestValue("Build-Date"),
                        "build git revision: " + ManifestUtil.getManifestValue("Git-Revision"),
                        "build number: " + ManifestUtil.getManifestValue("Build-Number"),
                        "build origin: " + ManifestUtil.getManifestValue("Build-Origin"),
                        "channels: " + DiscordSRV.getPlugin().getChannels(),
                        "console channel: " + DiscordSRV.getPlugin().getConsoleChannel(),
                        "main chat channel: " + DiscordSRV.getPlugin().getMainChatChannelPair(),
                        "unsubscribed players: " + DiscordSRV.getPlugin().getUnsubscribedPlayers(),
                        "colors: " + DiscordSRV.getPlugin().getColors(),
                        "threads:",
                        "    channel topic updater -> alive: " + (DiscordSRV.getPlugin().getChannelTopicUpdater() != null && DiscordSRV.getPlugin().getChannelTopicUpdater().isAlive()),
                        "    console message queue worker -> alive: " + (DiscordSRV.getPlugin().getConsoleMessageQueueWorker() != null && DiscordSRV.getPlugin().getConsoleMessageQueueWorker().isAlive()),
                        "hooked plugins: " + DiscordSRV.getPlugin().getHookedPlugins()
                }));
                put("relevant-lines-from-server.log", getRelevantLinesFromServerLog());
                put("config.yml", FileUtils.readFileToString(DiscordSRV.getPlugin().getConfigFile(), Charset.defaultCharset())
                    .replace(DiscordSRV.getPlugin().getConfig().getString("BotToken"), "BOT-TOKEN-REDACTED"));
                put("server-info.txt", getServerInfo());
                put("channel-permissions.txt", getChannelPermissions());
                put("system-info.txt", getSystemInfo());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }};

        return uploadToGists(files);
    }

    private static String getRandomPhrase() {
        return DiscordSRV.getPlugin().getRandomPhrases().get(DiscordSRV.getPlugin().getRandom().nextInt(DiscordSRV.getPlugin().getRandomPhrases().size()));
    }

    private static String getRelevantLinesFromServerLog() {
        List<String> output = new LinkedList<>();
        try {
            FileReader fr = new FileReader(new File(new File("."), "logs/latest.log"));
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

        List<String> plugins = new LinkedList<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) plugins.add(plugin.toString());
        Collections.sort(plugins);

        output.add("server name: " + DiscordUtil.stripColor(Bukkit.getServerName()));
        output.add("server motd: " + DiscordUtil.stripColor(Bukkit.getMotd()));
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
        ManagementFactory.getRuntimeMXBean().getSystemProperties().forEach((key, value) -> output.add(key + ": " + value));

        return String.join("\n", output);
    }

    /**
     * Upload the given file map to GitHub Gists
     * @param files A Map representing a structure of file name & it's contents
     * @return A user-friendly message of how the report went
     */
    private static String uploadToGists(Map<String, String> files) {
        Map<String, String> newFilesMap = new LinkedHashMap<>();
        files.forEach((fileName, fileContent) -> newFilesMap.put((newFilesMap.size() + 1) + "-" + fileName, fileContent));

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://api.github.com/gists").openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "DiscordSRV v" + DiscordSRV.getPlugin().getDescription().getVersion());
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            OutputStream out = connection.getOutputStream();
            JsonObject payload = new JsonObject();
            payload.addProperty("description", "DiscordSRV Debug Report - Generated " + new Date());
            payload.addProperty("public", "false");

            JsonObject filesJson = new JsonObject();
            newFilesMap.forEach((fileName, fileContent) -> {
                JsonObject file = new JsonObject();
                file.addProperty("content", fileContent);
                filesJson.add(fileName, file);
            });
            payload.add("files", filesJson);

            out.write(gson.toJson(payload).getBytes(Charset.forName("UTF-8")));
            out.close();

            String rawOutput = CharStreams.toString(new InputStreamReader(connection.getInputStream()));
            connection.getInputStream().close();
            JsonObject output = gson.fromJson(rawOutput, JsonObject.class);

            if (!output.has("html_url")) throw new InvalidObjectException("HTML URL was not given in Gist output");

            return "We've made a debug report with useful information, report your issue and provide this url: " + output.get("html_url").getAsString();
        } catch (Exception e) {
            try {
                // check if 403 & due to rate limit being hit
                if (connection != null && connection.getResponseCode() == 403 && connection.getHeaderField("X-RateLimit-Remaining").equals("0"))
                    return "Failed to send debug report: you may only create 60 dumps per hour, please try again in a bit.";
            } catch (IOException e1) {
                return "Failed to send debug report: failed to connect to GitHub Gists: " + e1.getLocalizedMessage();
            }

            e.printStackTrace();
            return "Failed to send debug report: check the server console for further details";
        }
    }

    public static String getStackTrace() {
        List<String> stackTrace = new LinkedList<>();
        stackTrace.add("Stack trace @ debug call (THIS IS NOT AN ERROR)");
        for (String line : ExceptionUtils.getStackTrace(new Throwable()).split("\n"))
            if (line.toLowerCase().contains("discordsrv") && !line.contains("DebugUtil.getStackTrace")) stackTrace.add(line);
        return String.join("\n", stackTrace);
    }

}
