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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.MatchResult;
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

        boolean clean = true;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNext(PATTERN)) {
                MatchResult match = scanner.match();
                linkedAccounts.put(
                        match.group(1),
                        UUID.fromString(match.group(2))
                );
            }

            if (scanner.hasNext()) {
                // scanner has more data, but it didn't match our pattern.
                // server probably died in middle of writing line... force a full save later to restore file integrity
                clean = false;
            }
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
