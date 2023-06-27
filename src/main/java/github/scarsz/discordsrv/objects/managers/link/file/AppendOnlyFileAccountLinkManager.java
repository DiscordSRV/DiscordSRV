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
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppendOnlyFileAccountLinkManager extends AbstractFileAccountLinkManager {

    // matches "discordId uuid" with anything after https://regex101.com/r/oRiDUP
    private static final Pattern LINK_PATTERN = Pattern.compile("^(?<discord>\\d+) (?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}).*");
    // matches "-discordId" or "-uuid" or "-discord uuid" or "-uuid discord" https://regex101.com/r/IkDT4K
    private static final Pattern MODIFICATION_PATTERN = Pattern.compile("^-(?>(?>(?<discord>\\d+)|(?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})) ?){1,2}.*");

    public AppendOnlyFileAccountLinkManager() {
        super();
    }

    @Override
    void load() throws IOException {
        File linkedAccountsJsonFile = new File(DiscordSRV.getPlugin().getDataFolder(), "linkedaccounts.json");
        if (linkedAccountsJsonFile.exists()) {
            @SuppressWarnings("deprecation") JsonFileAccountLinkManager manager = new JsonFileAccountLinkManager();
            manager.getLinkedAccounts().forEach(this::link);
            int count = manager.getLinkedAccountCount();

            File newFile = new File(DiscordSRV.getPlugin().getDataFolder(), "linkedaccounts.json.delete");
            if (!linkedAccountsJsonFile.renameTo(newFile)) {
                DiscordSRV.error("Failed to rename " + linkedAccountsJsonFile.getName() + " to " + newFile.getName());
            }
            DiscordSRV.info("Migrated " + count + " linked accounts to AOF file backend");
        }

        File file = getFile();
        if (!file.exists() || file.length() == 0) return;
        String fileContent = FileUtils.readFileToString(file, "UTF-8");
        if (fileContent == null || StringUtils.isBlank(fileContent)) return;
        String[] split = fileContent.split("\n");
        boolean clean = split[split.length - 1].equals("");
        int modifications = 0;
        for (String line : split) {
            Matcher matcher;

            matcher = LINK_PATTERN.matcher(line);
            if (matcher.matches()) {
                linkedAccounts.put(
                        matcher.group("discord"),
                        UUID.fromString(matcher.group("uuid"))
                );
                continue;
            }

            matcher = MODIFICATION_PATTERN.matcher(line);
            if (matcher.matches()) {
                String discord = matcher.group("discord");
                if (discord != null) linkedAccounts.remove(discord);

                UUID uuid = matcher.group("uuid") != null ? UUID.fromString(matcher.group("uuid")) : null;
                if (uuid != null) linkedAccounts.removeValue(uuid);

                modifications++;
            }

            // line doesn't match our formats, will force a save after loading to restore file integrity
            clean = false;
            DiscordSRV.error("Invalid line in linked accounts file: " + line);
        }

        if ((double) modifications / split.length >= .10) {
            // 10% of files are modifications, force a clean save
            clean = false;
        }

        if (!clean) save();
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
        User user = DiscordUtil.getJda().getUserById(discordId);
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        FileUtils.writeStringToFile(
                getFile(),
                String.format("%s %s // %s %s\n",
                        discordId,
                        uuid,
                        user != null ? user.getName() : "<discord username unknown>",
                        player.getName() != null ? player.getName() : "<player username unknown>"
                ),
                "UTF-8",
                true
        );
    }

    @Override
    @SneakyThrows
    public void unlink(UUID uuid) {
        String discordId;
        synchronized (linkedAccounts) {
            discordId = linkedAccounts.getKey(uuid);
        }
        if (discordId == null) return;

        synchronized (linkedAccounts) {
            beforeUnlink(uuid, discordId);
            linkedAccounts.removeValue(uuid);
            FileUtils.writeStringToFile(getFile(), "-" + discordId + " " + uuid + "\n", "UTF-8", true);
        }

        afterUnlink(uuid, discordId);
    }
    @Override
    @SneakyThrows
    public void unlink(String discordId) {
        UUID uuid;
        synchronized (linkedAccounts) {
            uuid = linkedAccounts.get(discordId);
        }
        if (uuid == null) return;

        synchronized (linkedAccounts) {
            beforeUnlink(uuid, discordId);
            linkedAccounts.remove(discordId);
            FileUtils.writeStringToFile(getFile(), "-" + discordId + " " + uuid + "\n", "UTF-8", true);
        }
        afterUnlink(uuid, discordId);
    }

    @Override
    File getFile() {
        return new File(DiscordSRV.getPlugin().getDataFolder(), "accounts.aof");
    }

}
