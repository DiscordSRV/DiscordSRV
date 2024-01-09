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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.BidiMultimap;
import github.scarsz.discordsrv.objects.managers.link.AbstractAccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PrettyUtil;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.ricetea.discordsrv.JBUser;
import org.ricetea.utils.SoftCache;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class AbstractFileAccountLinkManager extends AbstractAccountLinkManager {
    final ThreadLocal<SoftCache<StringBuilder>> stringBuilderCache = ThreadLocal.withInitial(() ->
            SoftCache.create(StringBuilder::new));
    final BidiMultimap<String, UUID> linkedAccounts = BidiMultimap.create(HashMultimap::create);

    public AbstractFileAccountLinkManager() {
        try {
            load();
            DiscordSRV.debug(Debug.ACCOUNT_LINKING, getClass().getSimpleName() + " loaded " + linkedAccounts.size() + " linked accounts");
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
    public Multimap<String, UUID> getLinkedAccounts() {
        return linkedAccounts;
    }

    @Override
    public String getDiscordIdFromCache(UUID uuid) {
        return getDiscordId(uuid);
    }

    @Override
    public Collection<UUID> getUuidsFromCache(String discordId) {
        return getUuids(discordId);
    }

    @Override
    public int getLinkedAccountCount() {
        return linkedAccounts.size();
    }

    @Override
    public String process(String linkCode, String discordId) {
        // strip the code to get rid of non-numeric characters
        linkCode = linkCode.replaceAll("[^0-9]", "");

        UUID codeUUID;
        Collection<UUID> uuids;
        synchronized (linkingCodes) {
            codeUUID = linkingCodes.get(linkCode);
        }
        synchronized (linkedAccounts) {
            uuids = linkedAccounts.get(discordId);
        }
        UUID uuidBeReplaced;
        boolean contains;
        if (uuids == null) {
            contains = false;
            uuidBeReplaced = null;
        }
        else {
            JBUser user = JBUser.of(uuids);
            uuidBeReplaced = user.testReplace(codeUUID);
            contains = uuidBeReplaced != null;
        }

        User user = DiscordUtil.getUserById(discordId);
        String mention = user == null ? "" : user.getAsMention();

        if (contains) {
            if (DiscordSRV.config().getBoolean("MinecraftDiscordAccountLinkedAllowRelinkBySendingANewCode")) {
                unlink(uuidBeReplaced);
            } else {
                StringBuilder stringBuilder = this.stringBuilderCache.get().get();
                stringBuilder.setLength(0);
                for (UUID uuid : uuids) {
                    OfflinePlayer offlinePlayer = DiscordSRV.getPlugin().getServer().getOfflinePlayer(uuid);
                    stringBuilder.append(LangUtil.Message.ALREADY_LINKED.toString()
                            .replace("%username%", PrettyUtil.beautifyUsername(offlinePlayer, "<Unknown>", false))
                            .replace("%uuid%", uuid.toString())
                            .replace("%mention%", mention));
                }
                return stringBuilder.toString();
            }
        }

        if (linkingCodes.containsKey(linkCode)) {
            link(discordId, codeUUID);
            linkingCodes.remove(linkCode);

            OfflinePlayer player = Bukkit.getOfflinePlayer(codeUUID);
            if (player.isOnline()) {
                MessageUtil.sendMessage(Bukkit.getPlayer(codeUUID), LangUtil.Message.MINECRAFT_ACCOUNT_LINKED.toString()
                        .replace("%username%", user == null ? "" : user.getName())
                        .replace("%id%", user == null ? "" : user.getId())
                );
            }

            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", PrettyUtil.beautifyUsername(player, "<Unknown>", false))
                    .replace("%displayname%", PrettyUtil.beautifyNickname(player, "<Unknown>", false))
                    .replace("%uuid%", codeUUID.toString())
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
    public Collection<UUID> getUuids(String discordId) {
        synchronized (linkedAccounts) {
            return linkedAccounts.get(discordId);
        }
    }

    @Override
    public Collection<UUID> getUuidsBypassCache(String discordId) {
        return getUuids(discordId);
    }

    @Override
    public Multimap<String, UUID> getManyUuids(Set<String> discordIds) {
        Multimap<String, UUID> results = HashMultimap.create();
        for (String discordId : discordIds) {
            Collection<UUID> uuids;
            synchronized (linkedAccounts) {
                uuids = linkedAccounts.get(discordId);
            }
            if (uuids != null) results.putAll(discordId, uuids);
        }
        return results;
    }

    @Override
    public void link(String discordId, UUID uuid) {
        if (discordId.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty Discord IDs are not allowed");
        }
        DiscordSRV.debug(Debug.ACCOUNT_LINKING, "File backed link: " + discordId + ": " + uuid);

        // make sure the user isn't linked
        unlink(uuid);

        JBUser user = JBUser.of(getUuids(discordId));
        UUID uuidNeedReplace = user.testReplace(uuid);
        if (uuidNeedReplace != null)
            unlink(uuidNeedReplace);


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
        Collection<UUID> uuids;
        synchronized (linkedAccounts) {
            uuids = linkedAccounts.get(discordId);
        }
        if (uuids == null || uuids.isEmpty()) return;

        synchronized (linkedAccounts) {
            for (UUID uuid : uuids) {
                beforeUnlink(uuid, discordId);
            }
            linkedAccounts.removeAll(discordId);
        }

        for (UUID uuid : uuids) {
            afterUnlink(uuid, discordId);
        }
    }

    abstract void load() throws IOException;

    abstract File getFile();

}
