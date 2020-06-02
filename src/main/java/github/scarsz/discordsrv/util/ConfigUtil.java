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

import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import github.scarsz.configuralize.ParseException;
import github.scarsz.configuralize.Provider;
import github.scarsz.configuralize.Source;
import github.scarsz.discordsrv.DiscordSRV;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigUtil {

    public static void migrate() {
        String configVersionRaw = DiscordSRV.config().getString("ConfigVersion");
        if (configVersionRaw.contains("/")) configVersionRaw = configVersionRaw.substring(0, configVersionRaw.indexOf("/"));
        String pluginVersionRaw = DiscordSRV.getPlugin().getDescription().getVersion();
        if (configVersionRaw.equals(pluginVersionRaw)) return;

        Version configVersion = configVersionRaw.split("\\.").length == 3
                ? Version.valueOf(configVersionRaw.replace("-SNAPSHOT", ""))
                : Version.valueOf("1." + configVersionRaw.replace("-SNAPSHOT", ""));
        Version pluginVersion = Version.valueOf(pluginVersionRaw.replace("-SNAPSHOT", ""));

        if (configVersion.equals(pluginVersion)) return; // no migration necessary
        if (configVersion.greaterThan(pluginVersion)) {
            DiscordSRV.warning("You're attempting to use a higher config version than the plugin. Things probably won't work correctly.");
            return;
        }

        String oldVersionName = configVersion.toString();
        DiscordSRV.info("Your DiscordSRV config file was outdated; attempting migration...");

        try {
            Provider configProvider = DiscordSRV.config().getProvider("config");
            Provider messageProvider = DiscordSRV.config().getProvider("messages");
            Provider voiceProvider = DiscordSRV.config().getProvider("voice");
            Provider linkingProvider = DiscordSRV.config().getProvider("linking");
            Provider synchronizationProvider = DiscordSRV.config().getProvider("synchronization");

            if (configVersion.greaterThanOrEqualTo(Version.forIntegers(1, 13, 0))) {
                migrate("messages.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getMessagesFile(), messageProvider);
                migrate("config.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getConfigFile(), configProvider, false);
                migrate("voice.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getVoiceFile(), voiceProvider);
                migrate("linking.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getLinkingFile(), linkingProvider, true);
                migrate("synchronization.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getSynchronizationFile(), synchronizationProvider);
            } else {
                // legacy migration <1.13.0
                // messages
                File messagesFrom = new File(DiscordSRV.getPlugin().getDataFolder(), "config.yml");
                File messagesTo = DiscordSRV.getPlugin().getMessagesFile();
                messageProvider.saveDefaults();
                copyYmlValues(messagesFrom, messagesTo, false);
                messageProvider.load();

                // config
                File configFrom = new File(DiscordSRV.getPlugin().getDataFolder(), "config.yml-build." + oldVersionName + ".old");
                File configTo = DiscordSRV.getPlugin().getConfigFile();
                FileUtils.moveFile(configTo, configFrom);
                configProvider.saveDefaults();
                copyYmlValues(configFrom, configTo, false);
                configProvider.load();

                // channels
                File channelsFile = new File(DiscordSRV.getPlugin().getDataFolder(), "channels.json");
                if (channelsFile.exists()) {
                    List<Map<String, String>> channels = new ArrayList<>();
                    JsonArray jsonElements = DiscordSRV.getPlugin().getGson().fromJson(FileUtils.readFileToString(channelsFile, StandardCharsets.UTF_8), JsonArray.class);
                    for (JsonElement jsonElement : jsonElements) {
                        channels.add(new HashMap<String, String>() {{
                            put(jsonElement.getAsJsonObject().get("channelname").getAsString(), jsonElement.getAsJsonObject().get("channelid").getAsString());
                        }});
                    }
                    String channelsString = "{" + channels.stream()
                            .map(stringStringMap -> "\"" + stringStringMap.keySet().iterator().next() + "\": \"" + stringStringMap.values().iterator().next() + "\"")
                            .collect(Collectors.joining(", ")) + "}";
                    FileUtils.writeStringToFile(channelsFile, "Channels: " + channelsString, StandardCharsets.UTF_8);
                    copyYmlValues(channelsFile, configTo, false);
                    channelsFile.delete();
                }

                // colors
                File colorsFile = new File(DiscordSRV.getPlugin().getDataFolder(), "colors.json");
                FileUtils.moveFile(colorsFile, new File(colorsFile.getParent(), "colors.json.old"));
            }
            DiscordSRV.info("Successfully migrated configuration files to version " + pluginVersionRaw);
        } catch (Exception e) {
            DiscordSRV.error("Failed migrating configs: " + e.getMessage());
            DiscordSRV.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    private static void migrate(String fromFileName, File to, Provider provider) throws IOException, ParseException {
        migrate(fromFileName, to, provider, false);
    }

    private static void migrate(String fromFileName, File to, Provider provider, boolean allowSpacedOptions) throws IOException, ParseException {
        File from = new File(DiscordSRV.getPlugin().getDataFolder(), fromFileName);
        if (from.exists()) from = new File(DiscordSRV.getPlugin().getDataFolder(), fromFileName + "-" + System.currentTimeMillis());
        FileUtils.moveFile(to, from);
        provider.saveDefaults();
        copyYmlValues(from, to, allowSpacedOptions);
        provider.load();
    }

    private static void copyYmlValues(File from, File to, boolean allowSpacedOptions) {
        try {
            List<String> oldConfigLines = Arrays.stream(FileUtils.readFileToString(from, StandardCharsets.UTF_8).split(System.lineSeparator() + "|\n")).collect(Collectors.toList());
            List<String> newConfigLines = Arrays.stream(FileUtils.readFileToString(to, StandardCharsets.UTF_8).split(System.lineSeparator() + "|\n")).collect(Collectors.toList());

            Map<String, String> oldConfigMap = new HashMap<>();
            for (String line : oldConfigLines) {
                if (line.startsWith("#") || line.startsWith("-") || line.isEmpty() || StringUtils.isBlank(line.substring(0, 1))) continue;
                String[] lineSplit = line.split(":", 2);
                if (lineSplit.length != 2) continue;
                String key = lineSplit[0];
                String value = lineSplit[1].trim();
                oldConfigMap.put(key, value);
            }

            Map<String, String> newConfigMap = new HashMap<>();
            for (String line : newConfigLines) {
                if (line.startsWith("#") || line.startsWith("-") || line.isEmpty() || (!allowSpacedOptions && StringUtils.isBlank(line.substring(0, 1)))) continue;
                String[] lineSplit = line.split(":", 2);
                if (lineSplit.length != 2) continue;
                String key = lineSplit[0];
                String value = lineSplit[1].trim();
                newConfigMap.put(key, value);
            }

            for (String key : oldConfigMap.keySet()) {
                if (newConfigMap.containsKey(key) && !key.startsWith("ConfigVersion")) {
                    DiscordSRV.debug("Migrating config option " + key + " with value " + (DebugUtil.SENSITIVE_OPTIONS.stream().anyMatch(key::equalsIgnoreCase) ? "OMITTED" : oldConfigMap.get(key)) + " to new config");
                    newConfigMap.put(key, oldConfigMap.get(key));
                }
            }

            for (String line : newConfigLines) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                if (line.startsWith("ConfigVersion")) {
                    newConfigLines.set(newConfigLines.indexOf(line), line);
                    continue;
                }
                String key = line.split(":")[0];
                if (oldConfigMap.containsKey(key)) {
                    newConfigLines.set(newConfigLines.indexOf(line), key + ": " + newConfigMap.get(key));
                }
            }

            FileUtils.writeStringToFile(to, String.join(System.lineSeparator(), newConfigLines), StandardCharsets.UTF_8);
        } catch (Exception e) {
            DiscordSRV.warning("Failed to migrate config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void logMissingOptions() {
        for (Map.Entry<Source, Provider> entry : DiscordSRV.config().getSources().entrySet()) {
            Set<String> keys = getAllKeys(entry.getValue().getDefaults().asMap());
            keys.removeAll(getAllKeys(entry.getValue().getValues().asMap()));

            for (String missing : keys) {
                // ignore map entries
                if (missing.contains(".")) continue;

                DiscordSRV.warning("Config key " + missing + " is missing from the " + entry.getKey().getResourceName() + ".yml. Using the default value of " + entry.getValue().getDefaults().dget(missing).asObject());
            }
        }
    }

    public static Set<String> getAllKeys(Map<String, Object> map) {
        return getAllKeys(map, null);
    }
    public static Set<String> getAllKeys(Map<String, Object> map, String prefix) {
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = (prefix != null ? prefix + "." : "") + entry.getKey();
            keys.add(key);

            if (entry.getValue() instanceof Map) keys.addAll(getAllKeys((Map) entry.getValue(), key));
        }
        return keys;
    }

}
