package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class ConfigUtil {

    // When I wrote this, only God and I understood what I was doing. Now, God only knows.
    // If this code works, it was written by Scarsz. If not, I don’t know who wrote it.

    public static void migrate() {
        double configVersion = DiscordSRV.getPlugin().getConfig().getDouble("ConfigVersion");
        double pluginVersion = Double.parseDouble(DiscordSRV.getPlugin().getDescription().getVersion());

        if (configVersion >= pluginVersion) return; // no migration necessary
        else DiscordSRV.info("Your DiscordSRV config file was outdated; attempting migration...");

        try {
            if (configVersion < 13) {
                // messages
                File messagesFrom = new File(DiscordSRV.getPlugin().getDataFolder(), "config.yml");
                File messagesTo = DiscordSRV.getPlugin().getMessagesFile();
                LangUtil.saveMessages();
                copyYmlValues(messagesFrom, messagesTo);
                LangUtil.reloadMessages();

                // config
                File configFrom = new File(DiscordSRV.getPlugin().getDataFolder(), "config.yml-build." + configVersion + ".old");
                File configTo = DiscordSRV.getPlugin().getConfigFile();
                FileUtils.moveFile(configTo, configFrom);
                LangUtil.saveConfig();
                copyYmlValues(configFrom, configTo);
                DiscordSRV.getPlugin().reloadConfig();
            } else {
                // messages
                File messagesFrom = new File(DiscordSRV.getPlugin().getDataFolder(), "messages.yml-build." + configVersion + ".old");
                File messagesTo = DiscordSRV.getPlugin().getMessagesFile();
                FileUtils.moveFile(messagesTo, messagesFrom);
                LangUtil.saveMessages();
                copyYmlValues(messagesFrom, messagesTo);
                LangUtil.reloadMessages();

                // config
                File configFrom = new File(DiscordSRV.getPlugin().getDataFolder(), "config.yml-build." + configVersion + ".old");
                File configTo = DiscordSRV.getPlugin().getConfigFile();
                FileUtils.moveFile(configTo, configFrom);
                LangUtil.saveConfig();
                copyYmlValues(configFrom, configTo);
                DiscordSRV.getPlugin().reloadConfig();
            }
        } catch(Exception e){
            DiscordSRV.error("Failed migrating configs: " + e.getMessage());
        }
    }

    private static void copyYmlValues(File from, File to) {
        try {
            Scanner s1 = new Scanner(from);
            List<String> oldConfigLines = new ArrayList<>();
            while (s1.hasNextLine()) oldConfigLines.add(s1.nextLine());
            s1.close();

            Scanner s2 = new Scanner(to);
            List<String> newConfigLines = new ArrayList<>();
            while (s2.hasNextLine()) newConfigLines.add(s2.nextLine());
            s2.close();

            Map<String, String> oldConfigMap = new HashMap<>();
            for (String line : oldConfigLines) {
                if (line.startsWith("#") || line.startsWith("-") || line.isEmpty()) continue;
                String[] lineSplit = line.split(": +|:", 2);
                String key = lineSplit[0];
                String value = lineSplit[1];
                oldConfigMap.put(key, value);
            }

            Map<String, String> newConfigMap = new HashMap<>();
            for (String line : newConfigLines) {
                if (line.startsWith("#") || line.startsWith("-") || line.isEmpty()) continue;
                String[] lineSplit = line.split(": +|:", 2);
                if (lineSplit.length == 2) newConfigMap.put(lineSplit[0], lineSplit[1]);
            }

            for (String key : oldConfigMap.keySet()) {
                if (newConfigMap.containsKey(key) && !key.startsWith("ConfigVersion")) {
                    DiscordSRV.debug("Migrating config option " + key + " with value " + (key.toLowerCase().equals("bottoken") ? "OMITTED" : oldConfigMap.get(key)) + " to new config");
                    newConfigMap.put(key, oldConfigMap.get(key));
                }
            }

            for (String line : newConfigLines) {
                if (line.startsWith("#") || line.startsWith("ConfigVersion")) continue;
                String key = line.split(":")[0];
                if (oldConfigMap.containsKey(key))
                    newConfigLines.set(newConfigLines.indexOf(line), key + ": " + oldConfigMap.get(key));
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(to));
            for (String line : newConfigLines) writer.write(line + System.lineSeparator());
            writer.flush();
            writer.close();
        } catch (Exception ignored) {}
    }

}
