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

package github.scarsz.discordsrv.objects.managers.link;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PrettyUtil;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FileAccountLinkManager extends AbstractAccountLinkManager {

    private final DualHashBidiMap<String, UUID> linkedAccounts = new DualHashBidiMap<>();

    @SuppressWarnings("ConstantConditions") // MalformedJsonException is a checked exception
    public FileAccountLinkManager() {
        if (!DiscordSRV.getPlugin().getLinkedAccountsFile().exists() ||
                DiscordSRV.getPlugin().getLinkedAccountsFile().length() == 0) return;
        linkedAccounts.clear();

        try {
            String fileContent = FileUtils.readFileToString(DiscordSRV.getPlugin().getLinkedAccountsFile(), StandardCharsets.UTF_8);
            if (fileContent == null || StringUtils.isBlank(fileContent)) fileContent = "{}";
            JsonObject jsonObject;
            try {
                jsonObject = DiscordSRV.getPlugin().getGson().fromJson(fileContent, JsonObject.class);
            } catch (Throwable t) {
                if (!(t instanceof MalformedJsonException) && !(t instanceof JsonSyntaxException) || !t.getMessage().contains("JsonPrimitive")) {
                    DiscordSRV.error("Failed to load linkedaccounts.json", t);
                    return;
                } else {
                    jsonObject = new JsonObject();
                }
            }

            jsonObject.entrySet().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue().getAsString();
                if (key.isEmpty() || value.isEmpty()) {
                    // empty values are not allowed.
                    return;
                }

                try {
                    linkedAccounts.put(key, UUID.fromString(value));
                } catch (Exception e) {
                    try {
                        linkedAccounts.put(value, UUID.fromString(key));
                    } catch (Exception f) {
                        DiscordSRV.warning("Failed to load linkedaccounts.json file. It's extremely recommended to delete your linkedaccounts.json file.");
                    }
                }
            });
        } catch (IOException e) {
            DiscordSRV.error("Failed to load linkedaccounts.json", e);
        }
    }

    @Override
    public boolean isInCache(UUID uuid) {
        // always in cache
        return true;
    }

    @Override
    public boolean isInCache(String discordId) {
        // always in cache
        return true;
    }

    @Override
    public Map<String, UUID> getLinkedAccounts() {
        return linkedAccounts;
    }

    @Override
    public String getDiscordIdFromCache(UUID uuid) {
        return getDiscordId(uuid);
    }

    @Override
    public UUID getUuidFromCache(String discordId) {
        return getUuid(discordId);
    }

    @Override
    public int getLinkedAccountCount() {
        return linkedAccounts.size();
    }

    @Override
    public String process(String linkCode, String discordId) {
        boolean contains;
        synchronized (linkedAccounts) {
            contains = linkedAccounts.containsKey(discordId);
        }
        if (contains) {
            if (DiscordSRV.config().getBoolean("MinecraftDiscordAccountLinkedAllowRelinkBySendingANewCode")) {
                unlink(discordId);
            } else {
                UUID uuid;
                synchronized (linkedAccounts) {
                    uuid = linkedAccounts.get(discordId);
                }
                OfflinePlayer offlinePlayer = DiscordSRV.getPlugin().getServer().getOfflinePlayer(uuid);
                return LangUtil.Message.ALREADY_LINKED.toString()
                        .replace("%username%", PrettyUtil.beautifyUsername(offlinePlayer, "<Unknown>", false))
                        .replace("%uuid%", uuid.toString());
            }
        }

        // strip the code to get rid of non-numeric characters
        linkCode = linkCode.replaceAll("[^0-9]", "");

        if (linkingCodes.containsKey(linkCode)) {
            link(discordId, linkingCodes.get(linkCode));
            linkingCodes.remove(linkCode);

            OfflinePlayer player = Bukkit.getOfflinePlayer(getUuid(discordId));
            if (player.isOnline()) {
                MessageUtil.sendMessage(Bukkit.getPlayer(getUuid(discordId)), LangUtil.Message.MINECRAFT_ACCOUNT_LINKED.toString()
                        .replace("%username%", DiscordUtil.getUserById(discordId).getName())
                        .replace("%id%", DiscordUtil.getUserById(discordId).getId())
                );
            }

            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", PrettyUtil.beautifyUsername(player, "<Unknown>", false))
                    .replace("%displayname%", PrettyUtil.beautifyNickname(player, "<Unknown>", false))
                    .replace("%uuid%", getUuid(discordId).toString());
        }

        return linkCode.length() == 4
                ? LangUtil.Message.UNKNOWN_CODE.toString()
                : LangUtil.Message.INVALID_CODE.toString();
    }

    @Override
    public String getDiscordId(UUID uuid) {
        synchronized (linkedAccounts) {
            return linkedAccounts.getKey(uuid);
        }
    }

    @Override
    public String getDiscordIdBypassCache(UUID uuid) {
        return getDiscordId(uuid);
    }

    @Override
    public Map<UUID, String> getManyDiscordIds(Set<UUID> uuids) {
        Map<UUID, String> results = new HashMap<>();
        for (UUID uuid : uuids) {
            String discordId;
            synchronized (linkedAccounts) {
                discordId = linkedAccounts.getKey(uuid);
            }
            if (discordId != null) results.put(uuid, discordId);
        }
        return results;
    }

    @Override
    public UUID getUuid(String discordId) {
        synchronized (linkedAccounts) {
            return linkedAccounts.get(discordId);
        }
    }

    @Override
    public UUID getUuidBypassCache(String discordId) {
        return getUuid(discordId);
    }

    @Override
    public Map<String, UUID> getManyUuids(Set<String> discordIds) {
        Map<String, UUID> results = new HashMap<>();
        for (String discordId : discordIds) {
            UUID uuid;
            synchronized (linkedAccounts) {
                uuid = linkedAccounts.get(discordId);
            }
            if (uuid != null) results.put(discordId, uuid);
        }
        return results;
    }

    @Override
    public void link(String discordId, UUID uuid) {
        if (discordId.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty discord id's are not allowed");
        }
        DiscordSRV.debug(Debug.ACCOUNT_LINKING, "File backed link: " + discordId + ": " + uuid);

        // make sure the user isn't linked
        unlink(discordId);
        unlink(uuid);

        synchronized (linkedAccounts) {
            linkedAccounts.put(discordId, uuid);
        }
        afterLink(discordId, uuid);
    }

    @Override
    public void unlink(UUID uuid) {
        String discordId;
        synchronized (linkedAccounts) {
            discordId = linkedAccounts.getKey(uuid);
        }
        if (discordId == null) return;

        synchronized (linkedAccounts) {
            beforeUnlink(uuid, discordId);
            linkedAccounts.removeValue(uuid);
        }

        afterUnlink(uuid, discordId);
    }

    @Override
    public void unlink(String discordId) {
        UUID uuid;
        synchronized (linkedAccounts) {
            uuid = linkedAccounts.get(discordId);
        }
        if (uuid == null) return;

        synchronized (linkedAccounts) {
            beforeUnlink(uuid, discordId);
            linkedAccounts.remove(discordId);
        }
        afterUnlink(uuid, discordId);
    }

    @Override
    public void save() {
        long startTime = System.currentTimeMillis();

        try {
            JsonObject map = new JsonObject();
            synchronized (linkedAccounts) {
                linkedAccounts.forEach((discordId, uuid) -> map.addProperty(discordId, String.valueOf(uuid)));
            }
            FileUtils.writeStringToFile(DiscordSRV.getPlugin().getLinkedAccountsFile(), map.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            DiscordSRV.error(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVE_FAILED + ": " + e.getMessage());
            return;
        }

        DiscordSRV.info(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVED.toString()
                .replace("{ms}", String.valueOf(System.currentTimeMillis() - startTime))
        );
    }

}
