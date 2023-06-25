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

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.link.AbstractAccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PrettyUtil;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractFileAccountLinkManager extends AbstractAccountLinkManager {

    final DualHashBidiMap<String, UUID> linkedAccounts = new DualHashBidiMap<>();

    public AbstractFileAccountLinkManager() {
        try {
            File file = getFile();
            if (file.exists()) load();
        } catch (IOException e) {
            DiscordSRV.error("Failed to load linked accounts", e);
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

        User user = DiscordUtil.getUserById(discordId);
        String mention = user == null ? "" : user.getAsMention();

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
                        .replace("%uuid%", uuid.toString())
                        .replace("%mention%", mention);
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
                        .replace("%username%", user == null ? "" : user.getName())
                        .replace("%id%", user == null ? "" : user.getId())
                );
            }

            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", PrettyUtil.beautifyUsername(player, "<Unknown>", false))
                    .replace("%displayname%", PrettyUtil.beautifyNickname(player, "<Unknown>", false))
                    .replace("%uuid%", getUuid(discordId).toString())
                    .replace("%mention%", mention);
        }

        String reply = linkCode.length() == 4
                ? LangUtil.Message.UNKNOWN_CODE.toString()
                : LangUtil.Message.INVALID_CODE.toString();
        return reply
                .replace("%code%", linkCode)
                .replace("%mention%", mention);
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
            throw new IllegalArgumentException("Empty discord IDS are not allowed");
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

    abstract void load() throws IOException;
    abstract File getFile();

}
