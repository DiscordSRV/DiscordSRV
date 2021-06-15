/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.util;

import com.github.zafarkhaja.semver.Version;
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
            Provider alertsProvider = DiscordSRV.config().getProvider("alerts");

            migrate("config.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getConfigFile(), configProvider);
            migrate("messages.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getMessagesFile(), messageProvider);
            migrate("voice.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getVoiceFile(), voiceProvider);
            migrate("linking.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getLinkingFile(), linkingProvider);
            migrate("synchronization.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getSynchronizationFile(), synchronizationProvider);
            migrate("alerts.yml-build." + oldVersionName + ".old", DiscordSRV.getPlugin().getAlertsFile(), alertsProvider);
            DiscordSRV.info("Successfully migrated configuration files to version " + pluginVersionRaw);
        } catch (Exception e) {
            DiscordSRV.error("Failed migrating configs: " + e.getMessage());
            DiscordSRV.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    private static void migrate(String fromFileName, File to, Provider provider) throws IOException, ParseException {
        File from = new File(DiscordSRV.getPlugin().getDataFolder(), fromFileName);
        if (from.exists()) from = new File(DiscordSRV.getPlugin().getDataFolder(), fromFileName + "-" + System.currentTimeMillis());
        FileUtils.moveFile(to, from);
        provider.saveDefaults();

        List<String> oldConfigLines = Arrays.stream(FileUtils.readFileToString(from, StandardCharsets.UTF_8).split(System.lineSeparator() + "|\n")).collect(Collectors.toList());
        List<String> newConfigLines = Arrays.stream(FileUtils.readFileToString(to, StandardCharsets.UTF_8).split(System.lineSeparator() + "|\n")).collect(Collectors.toList());

        Map<String, String> options = new HashMap<>();

        String option = null;
        StringBuilder optionValue = null;
        StringBuilder buffer = new StringBuilder();
        for (String line : oldConfigLines) {
            boolean blank = StringUtils.isBlank(line);
            if (line.startsWith("#") || (blank && option == null)) continue;
            if (blank || line.startsWith("}")) {
                if (optionValue != null) {
                    optionValue.append(line);
                    buffer.append('\n');
                }
                continue;
            } else if (StringUtils.isBlank(line.substring(0, 1))) {
                if (optionValue != null) {
                    optionValue.append(buffer).append('\n').append(line);
                    buffer.setLength(0);
                }
                continue;
            }

            if (option != null) {
                options.put(option, optionValue.toString());
                option = null;
                buffer.setLength(0);
            }
            String[] lineSplit = line.split(":", 2);
            if (lineSplit.length != 2) {
                if (optionValue != null) optionValue.append('\n').append(line);
                continue;
            }
            String key = lineSplit[0].trim();
            if (key.equals("ConfigVersion")) continue;
            option = key;
            String value = lineSplit[1].trim();
            optionValue = new StringBuilder(value);
        }
        if (optionValue != null) options.put(option, optionValue.toString());

        StringBuilder newConfig = new StringBuilder();

        boolean sameOption = false;
        StringBuilder comments = new StringBuilder();
        for (String line : newConfigLines) {
            if (StringUtils.isBlank(line) || line.startsWith("#")) {
                comments.append(line).append('\n');
                continue;
            }

            if (sameOption) {
                if (StringUtils.isBlank(line.substring(0, 1))) {
                    continue;
                } else {
                    newConfig.append(option).append(": ").append(options.get(option)).append('\n').append(comments);
                    comments.setLength(0);
                    sameOption = false;
                }
            }
            String[] lineSplit = line.split(":", 2);
            if (lineSplit.length != 2) continue;
            newConfig.append(comments);
            comments.setLength(0);
            String key = lineSplit[0];
            if (!options.containsKey(key)) {
                newConfig.append(line).append('\n');
                continue;
            }
            option = key;
            DiscordSRV.debug("Migrating config option " + option + " with value " + (DebugUtil.SENSITIVE_OPTIONS.stream().anyMatch(option::equalsIgnoreCase) ? "OMITTED" : options.get(option)) + " to new config");
            sameOption = true;
        }
        if (option != null) newConfig.append(option).append(": ").append(options.get(option));
        newConfig.append(comments);

        FileUtils.writeStringToFile(to, newConfig.toString(), StandardCharsets.UTF_8);

        provider.load();
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
