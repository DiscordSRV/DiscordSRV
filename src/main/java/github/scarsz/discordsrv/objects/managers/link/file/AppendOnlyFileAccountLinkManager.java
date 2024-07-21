/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
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

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppendOnlyFileAccountLinkManager extends AbstractFileAccountLinkManager {

    // matches "discordId uuid" with anything after https://regex101.com/r/oRiDUP
    private static final Pattern LINK_PATTERN = Pattern.compile("^(?<discord>\\d+) (?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}).*");
    // matches "-discordId" or "-uuid" or "-discord uuid" or "-uuid discord" https://regex101.com/r/IkDT4K/3
    private static final Pattern MODIFICATION_PATTERN = Pattern.compile("^-(?>(?>(?<discord>\\d{17,}+)|(?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})) ?){1,2}.*");

    public AppendOnlyFileAccountLinkManager() {
        super();
    }

    @Override
    void load() throws IOException {
        readAOF();
        importJsonFile();
    }

    private void readAOF() throws IOException {
        File file = getFile();
        if (!file.exists()) {
            File temporaryFile = getTemporaryFile();
            if (temporaryFile.exists() && temporaryFile.length() > 0) {
                DiscordSRV.warning("AOF linked accounts file didn't exist but the temporary one does. Did the server die while saving?");
                file = temporaryFile;
            } else {
                return;
            }
        } else if (file.length() == 0) {
            return;
        }

        String fileContent = FileUtils.readFileToString(file, "UTF-8");
        if (fileContent == null || StringUtils.isBlank(fileContent)) return;

        DiscordSRV.debug(Debug.ACCOUNT_LINKING, "Reading accounts.aof file...");

        String[] split = fileContent.split("\n");
        boolean clean = split[split.length - 1].equals("");
        int modifications = 0;
        for (String line : split) {
            Matcher matcher;

            matcher = LINK_PATTERN.matcher(line);
            if (matcher.matches()) {
                String discordId = matcher.group("discord");
                String uuid = matcher.group("uuid");

                //DiscordSRV.debug(Debug.ACCOUNT_LINKING, "Adding a link for " + uuid + " to " + discordId);
                linkedAccounts.put(
                        discordId,
                        UUID.fromString(uuid)
                );
                continue;
            }

            matcher = MODIFICATION_PATTERN.matcher(line);
            if (matcher.matches()) {
                String discordId = matcher.group("discord");
                if (discordId != null) {
                    //DiscordSRV.debug(Debug.ACCOUNT_LINKING, "Removing " + discordId + " since it was found to be modified");
                    linkedAccounts.remove(discordId);
                }

                String uuid = matcher.group("uuid");
                if (uuid != null) {
                    //DiscordSRV.debug(Debug.ACCOUNT_LINKING, "Removing " + uuid + " since it was found to be modified");
                    linkedAccounts.removeValue(UUID.fromString(uuid));
                }

                modifications++;
                continue;
            }

            // line doesn't match our formats, will force a save after loading to restore file integrity
            clean = false;
            DiscordSRV.error("Invalid line in linked accounts file: " + line);
        }
        DiscordSRV.debug(Debug.ACCOUNT_LINKING, "Finished reading accounts.aof file");

        if ((double) modifications / split.length >= .10) {
            // 10% of files are modifications, force a clean save
            clean = false;
        }

        if (!clean) save();
    }

    private void importJsonFile() throws IOException {
        File linkedAccountsJsonFile = new File(DiscordSRV.getPlugin().getDataFolder(), "linkedaccounts.json");
        if (linkedAccountsJsonFile.exists()) {
            @SuppressWarnings("deprecation") JsonFileAccountLinkManager manager = new JsonFileAccountLinkManager();
            AtomicInteger count = new AtomicInteger();
            manager.linkedAccounts.forEach((discordId, uuid) -> {
                if (!linkedAccounts.containsKey(discordId)) {
                    linkedAccounts.put(discordId, uuid);
                    count.getAndIncrement();
                }
            });
            save();

            File newFile = new File(DiscordSRV.getPlugin().getDataFolder(), "linkedaccounts.json.delete");
            if (!linkedAccountsJsonFile.renameTo(newFile)) {
                DiscordSRV.error("Failed to rename " + linkedAccountsJsonFile.getName() + " to " + newFile.getName());
            }
            DiscordSRV.info("Migrated " + count + " linked accounts to AOF file backend");
        }
    }

    @Override
    public void save() {
        File file = getFile();
        File tmpFile = getTemporaryFile();
        tmpFile.deleteOnExit();


        DiscordSRV.debug(Debug.ACCOUNT_LINKING, "Saving accounts.aof file...");

        long startTime = System.currentTimeMillis();
        try {
            try (FileWriter fileWriter = new FileWriter(tmpFile);
                 BufferedWriter writer = new BufferedWriter(fileWriter)) {
                for (Map.Entry<String, UUID> entry : linkedAccounts.entrySet()) {
                    String discordId = entry.getKey();
                    UUID uuid = entry.getValue();
                    writer.write(discordId + " " + uuid + "\n");
                }
            } catch (IOException e) {
                DiscordSRV.error(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVE_FAILED + ": " + e.getMessage());
                return;
            }
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            try {
                FileUtils.moveFile(tmpFile, file);
            } catch (IOException e) {
                DiscordSRV.error("Failed moving accounts.aof.tmp to accounts.aof: " + e.getMessage());
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpFile.delete();
        }
        DiscordSRV.info(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVED.toString()
                .replace("{ms}", String.valueOf(System.currentTimeMillis() - startTime))
        );
    }

    @Override
    @SneakyThrows
    public void link(String discordId, UUID uuid) {
        super.link(discordId, uuid);

        FileUtils.writeStringToFile(getFile(), discordId + " " + uuid + "\n", "UTF-8", true);
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
    File getTemporaryFile() {
        return new File(DiscordSRV.getPlugin().getDataFolder(), "accounts.aof.tmp");
    }

}
