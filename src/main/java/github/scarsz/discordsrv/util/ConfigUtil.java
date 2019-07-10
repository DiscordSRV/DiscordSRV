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

import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import github.scarsz.discordsrv.DiscordSRV;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigUtil {

    // When I wrote this, only God and I understood what I was doing. Now, God only knows.
    // If this code works, it was written by Scarsz. If not, I don’t know who wrote it.

    public static void migrate() {
        Version configVersion = DiscordSRV.config().getString("ConfigVersion").split("\\.").length == 3 ? Version.valueOf(DiscordSRV.config().getString("ConfigVersion")) : Version.valueOf("1." + DiscordSRV.config().getString("ConfigVersion"));
        Version pluginVersion = Version.valueOf(DiscordSRV.getPlugin().getDescription().getVersion());

        if (configVersion.equals(pluginVersion)) return; // no migration necessary
        if (configVersion.greaterThan(pluginVersion)) {
            DiscordSRV.warning("You're attempting to use a higher config version than the plugin. Things probably won't work correctly.");
            return;
        }

        DiscordSRV.info("Your DiscordSRV config file was outdated; attempting migration...");
        try {
            if (configVersion.greaterThanOrEqualTo(Version.forIntegers(1, 13, 0))) {
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
            } else {
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

                // channels
                File channelsFile = new File(DiscordSRV.getPlugin().getDataFolder(), "channels.json");
                if (channelsFile.exists()) {
                    List<Map<String, String>> channels = new ArrayList<>();
                    JsonArray jsonElements = DiscordSRV.getPlugin().getGson().fromJson(FileUtils.readFileToString(channelsFile, Charset.forName("UTF-8")), JsonArray.class);
                    for (JsonElement jsonElement : jsonElements) {
                        channels.add(new HashMap<String, String>() {{
                            put(jsonElement.getAsJsonObject().get("channelname").getAsString(), jsonElement.getAsJsonObject().get("channelid").getAsString());
                        }});
                    }
                    String channelsString = "{" + channels.stream()
                            .map(stringStringMap -> "\"" + stringStringMap.keySet().iterator().next() + "\": \"" + stringStringMap.values().iterator().next() + "\"")
                            .collect(Collectors.joining(", ")) + "}";
                    FileUtils.writeStringToFile(channelsFile, "Channels: " + channelsString, Charset.forName("UTF-8"));
                    copyYmlValues(channelsFile, configTo);
                    channelsFile.delete();
                }

                // colors
                File colorsFile = new File(DiscordSRV.getPlugin().getDataFolder(), "colors.json");
                FileUtils.moveFile(colorsFile, new File(colorsFile.getParent(), "colors.json.old"));
            }
            DiscordSRV.info("Successfully migrated configuration files to version " + DiscordSRV.config().getString("ConfigVersion"));
        } catch(Exception e){
            DiscordSRV.error("Failed migrating configs: " + e.getMessage());
        }
    }

    private static void copyYmlValues(File from, File to) {
        try {
            List<String> oldConfigLines = Arrays.stream(FileUtils.readFileToString(from, Charset.forName("UTF-8")).split(System.lineSeparator() + "|\n")).collect(Collectors.toList());
            List<String> newConfigLines = Arrays.stream(FileUtils.readFileToString(to, Charset.forName("UTF-8")).split(System.lineSeparator() + "|\n")).collect(Collectors.toList());

            Map<String, String> oldConfigMap = new HashMap<>();
            for (String line : oldConfigLines) {
                if (line.startsWith("#") || line.startsWith("-") || line.isEmpty()) continue;
                String[] lineSplit = line.split(":", 2);
                if (lineSplit.length != 2) continue;
                String key = lineSplit[0];
                String value = lineSplit[1].trim();
                oldConfigMap.put(key, value);
            }

            Map<String, String> newConfigMap = new HashMap<>();
            for (String line : newConfigLines) {
                if (line.startsWith("#") || line.startsWith("-") || line.isEmpty()) continue;
                String[] lineSplit = line.split(":", 2);
                if (lineSplit.length != 2) continue;
                String key = lineSplit[0];
                String value = lineSplit[1].trim();
                newConfigMap.put(key, value);
            }

            for (String key : oldConfigMap.keySet()) {
                if (newConfigMap.containsKey(key) && !key.startsWith("ConfigVersion")) {
                    DebugUtil.SENSITIVE_OPTIONS.contains(key.toLowerCase());

                    DiscordSRV.debug("Migrating config option " + key + " with value " + (key.toLowerCase().equals("bottoken")
                            || key.toLowerCase().equals("experiment_jdbcaccountlinkbackend") ? "OMITTED" : oldConfigMap.get(key)) + " to new config");
                    newConfigMap.put(key, oldConfigMap.get(key));
                }
            }

            for (String line : newConfigLines) {
                if (line.startsWith("#") || line.startsWith("ConfigVersion") || line.isEmpty()) continue;
                String key = line.split(":")[0];
                if (oldConfigMap.containsKey(key))
                    newConfigLines.set(newConfigLines.indexOf(line), key + ": " + newConfigMap.get(key));
            }

            FileUtils.writeStringToFile(to, String.join(System.lineSeparator(), newConfigLines), Charset.forName("UTF-8"));
        } catch (Exception e) {
            DiscordSRV.warning("Failed to migrate config: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
