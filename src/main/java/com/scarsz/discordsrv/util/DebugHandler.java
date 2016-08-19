package com.scarsz.discordsrv.util;

import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DebugHandler {

    public static String run() {
        List<String> info = new LinkedList<>();

        // system properties
        ManagementFactory.getRuntimeMXBean().getSystemProperties().forEach((key, value) -> info.add("sysprop - " + key + " = " + value));
        info.add("");

        // total number of processors or cores available to the JVM
        info.add("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
        info.add("");

        // memory
        info.add("Free memory (MB): " + Runtime.getRuntime().freeMemory() / 1024 / 1024);
        info.add("Maximum memory (MB): " + (Runtime.getRuntime().maxMemory() == Long.MAX_VALUE ? "no limit" : Runtime.getRuntime().maxMemory() / 1024 / 1024));
        info.add("Total memory available to JVM (MB): " + Runtime.getRuntime().totalMemory() / 1024 / 1024);
        info.add("");

        // drive space
        File[] roots = File.listRoots();
        for (File root : roots) {
            info.add("File system root: " + root.getAbsolutePath());
            info.add("- Total space (MBM): " + root.getTotalSpace() / 1024 / 1024);
            info.add("- Free space (MB): " + root.getFreeSpace() / 1024 / 1024);
            info.add("- Usable space (MB): " + root.getUsableSpace() / 1024 / 1024);
        }
        info.add("");

        info.add("Plugin version: " + DiscordSRV.plugin.getDescription().getVersion());
        info.add("Version: " + Bukkit.getVersion());
        info.add("Bukkit version: " + Bukkit.getBukkitVersion());
        info.add("");

        // config.yml
        info.add("config.yml");
        FileConfiguration config = DiscordSRV.plugin.getConfig();
        info.addAll(config.getKeys(true).stream().filter(s -> !s.equals("BotToken")).map(s -> s + ": " + config.get(s)).collect(Collectors.toList()));
        info.add("");

        // channels.json
        info.add("channels.json");
        info.add(String.valueOf(DiscordSRV.channels));
        info.add("");

        // discordsrv info
        info.add("discordsrv info");
        info.add("consoleChannel: " + DiscordSRV.consoleChannel);
        info.add("mainChatChannel: " + DiscordSRV.chatChannel);
        info.add("pluginVersion: " + DiscordSRV.plugin.getDescription().getVersion());
        info.add("configVersion: " + DiscordSRV.plugin.getConfig().getString("ConfigVersion"));
        info.add("channels: " + DiscordSRV.channels);
        info.add("unsubscribedPlayers: " + DiscordSRV.unsubscribedPlayers);
        info.add("colors: " + DiscordSRV.colors);
        info.add("threads: " + Arrays.asList(
                "channelTopicUpdater -> alive: " + (DiscordSRV.channelTopicUpdater != null && DiscordSRV.channelTopicUpdater.isAlive()),
                "serverLogWatcher -> alive: " + (DiscordSRV.serverLogWatcher != null && DiscordSRV.serverLogWatcher.isAlive())));
        info.add("updateIsAvailable: " + DiscordSRV.updateIsAvailable);
        info.add("usingHerochat: " + DiscordSRV.usingHerochat);
        info.add("usingLegendChat: " + DiscordSRV.usingLegendChat);
        info.add("usingVentureChat: " + DiscordSRV.usingVentureChat);
        info.add("");

        // latest.log lines
        try {
            FileReader fr = new FileReader(new File(new File(".").getAbsolutePath() + "/logs/latest.log").getAbsolutePath());
            BufferedReader br = new BufferedReader(fr);
            info.add("Lines for DiscordSRV from latest.log:");
            boolean done = false;
            while (!done)
            {
                String line = null;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (line == null) done = true;
                if (line != null && line.toLowerCase().contains("discordsrv")) info.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // upload to hastebin
        StringBuilder response = new StringBuilder();
        try {
            URLConnection conn = new URL("http://hastebin.com/documents").openConnection();
            conn.setDoOutput(true);
            String str = String.join("\n", info);
            byte[] outputInBytes = str.getBytes("UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(outputInBytes);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) response.append(inputLine);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String hastebinUrl = "http://hastebin.com/" + response.toString().split("\"")[3];
        return hastebinUrl;
    }

}
