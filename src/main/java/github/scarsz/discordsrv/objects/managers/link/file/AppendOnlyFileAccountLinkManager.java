/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.objects.managers.link.file;

import github.scarsz.discordsrv.DiscordSRV;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppendOnlyFileAccountLinkManager extends AbstractFileAccountLinkManager {

    // matches "discordId uuid" with anything after, terminated by a newline https://regex101.com/r/4ELoBM
    private static final Pattern PATTERN = Pattern.compile("^(?<discord>\\d+) (?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}).*\\n?");

    @Override
    void load() throws IOException {
        File linkedAccountsJsonFile = new File(DiscordSRV.getPlugin().getDataFolder(), "linkedaccounts.json");
        if (linkedAccountsJsonFile.exists()) {
            int count = importJsonFile(linkedAccountsJsonFile);
            File newFile = new File(DiscordSRV.getPlugin().getDataFolder(), "linkedaccounts.json.delete");
            if (linkedAccountsJsonFile.renameTo(newFile)) {
                if (!newFile.delete()) newFile.deleteOnExit();
            } else {
                if (!linkedAccountsJsonFile.delete()) linkedAccountsJsonFile.deleteOnExit();
            }
            DiscordSRV.info("Migrated " + count + " linked accounts to new AOF file backend");
        }

        File file = getFile();
        if (!file.exists() || file.length() == 0) return;
        String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        if (fileContent == null || StringUtils.isBlank(fileContent)) return;

        int fromIndex = 0;
        int toIndex = fileContent.indexOf('\n');
        boolean clean = true;
        while (toIndex != -1) {
            String line = fileContent.substring(fromIndex, toIndex + 1);
            Matcher matcher = PATTERN.matcher(line);
            if (matcher.matches()) {
                linkedAccounts.put(
                        matcher.group("discord"),
                        UUID.fromString(matcher.group("uuid"))
                );
            } else {
                // line doesn't match proper format, will force a save after loading
                clean = false;
            }
            fromIndex = toIndex + 1;
            toIndex = fileContent.indexOf('\n', fromIndex);
        }

        if (!clean) save();
    }

    @SuppressWarnings("deprecation")
    private int importJsonFile(File linkedAccountsJson) throws IOException {
        if (!linkedAccountsJson.exists()) throw new IOException("Linked accounts JSON file doesn't exist");
        JsonFileAccountLinkManager manager = new JsonFileAccountLinkManager();
        manager.getLinkedAccounts().forEach(this::link);
        return manager.getLinkedAccountCount();
    }

    @Override
    public void save() throws IOException {
        try (FileWriter fileWriter = new FileWriter(getFile())) {
            try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
                for (Map.Entry<String, UUID> entry : linkedAccounts.entrySet()) {
                    String discordId = entry.getKey();
                    UUID uuid = entry.getValue();
                    writer.write(discordId + " " + uuid + "\n");
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public void link(String discordId, UUID uuid) {
        super.link(discordId, uuid);
        FileUtils.writeStringToFile(getFile(), discordId + " " + uuid + "\n", "UTF-8", true);
    }

    @Override
    File getFile() {
        return new File(DiscordSRV.getPlugin().getDataFolder(), "accounts.aof");
    }

}
