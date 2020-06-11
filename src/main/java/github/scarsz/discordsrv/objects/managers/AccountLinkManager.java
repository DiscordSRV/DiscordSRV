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

package github.scarsz.discordsrv.objects.managers;

import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import github.scarsz.discordsrv.util.PrettyUtil;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AccountLinkManager {

    @Getter private final Map<String, UUID> linkingCodes = new ConcurrentHashMap<>();
    @Getter private final Map<String, UUID> linkedAccounts = new ConcurrentHashMap<>();

    public AccountLinkManager() {
        if (!DiscordSRV.getPlugin().getLinkedAccountsFile().exists() ||
                DiscordSRV.getPlugin().getLinkedAccountsFile().length() == 0) return;
        linkedAccounts.clear();

        try {
            DiscordSRV.getPlugin().getGson().fromJson(FileUtils.readFileToString(DiscordSRV.getPlugin().getLinkedAccountsFile(), StandardCharsets.UTF_8), JsonObject.class).entrySet().forEach(entry -> {
                try {
                    linkedAccounts.put(entry.getKey(), UUID.fromString(entry.getValue().getAsString()));
                } catch (Exception e) {
                    try {
                        linkedAccounts.put(entry.getValue().getAsString(), UUID.fromString(entry.getKey()));
                    } catch (Exception f) {
                        DiscordSRV.warning("Failed to load linkedaccounts.json file. It's extremely recommended to delete your linkedaccounts.json file.");
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateCode(UUID playerUuid) {
        String codeString;
        do {
            int code = DiscordSRV.getPlugin().getRandom().nextInt(10000);
            codeString = String.format("%04d", code);
        } while (linkingCodes.putIfAbsent(codeString, playerUuid) != null);
        return codeString;
    }

    public String process(String linkCode, String discordId) {
        if (linkedAccounts.containsKey(discordId)) {
            if (DiscordSRV.config().getBoolean("MinecraftDiscordAccountLinkedAllowRelinkBySendingANewCode")) {
                unlink(discordId);
            } else {
                UUID uuid = linkedAccounts.get(discordId);
                OfflinePlayer offlinePlayer = DiscordSRV.getPlugin().getServer().getOfflinePlayer(uuid);
                return LangUtil.InternalMessage.ALREADY_LINKED.toString()
                        .replace("{username}", PrettyUtil.beautifyUsername(offlinePlayer))
                        .replace("{uuid}", uuid.toString());
            }
        }

        // strip the code to get rid of non-numeric characters
        linkCode = linkCode.replaceAll("[^0-9]", "");

        if (linkingCodes.containsKey(linkCode)) {
            link(discordId, linkingCodes.get(linkCode));
            linkingCodes.remove(linkCode);

            OfflinePlayer player = Bukkit.getOfflinePlayer(getUuid(discordId));
            if (player.isOnline())
                Bukkit.getPlayer(getUuid(discordId)).sendMessage(LangUtil.Message.MINECRAFT_ACCOUNT_LINKED.toString()
                        .replace("%username%", DiscordUtil.getUserById(discordId).getName())
                        .replace("%id%", DiscordUtil.getUserById(discordId).getId())
                );

            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", PrettyUtil.beautifyUsername(player))
                    .replace("%displayname%", PrettyUtil.beautifyNickname(player))
                    .replace("%uuid%", getUuid(discordId).toString());
        }

        return linkCode.length() == 4
                ? LangUtil.InternalMessage.UNKNOWN_CODE.toString()
                : LangUtil.InternalMessage.INVALID_CODE.toString();
    }

    public String getDiscordId(UUID uuid) {
        Map.Entry<String, UUID> match = linkedAccounts.entrySet().stream().filter(entry -> entry.getValue().equals(uuid)).findFirst().orElse(null);
        return match == null ? null : match.getKey();
    }

    public Map<UUID, String> getManyDiscordIds(Set<UUID> uuids) {
        Map<UUID, String> results = new HashMap<>();
        linkedAccounts.entrySet().stream()
                .filter(entry -> uuids.contains(entry.getValue()))
                .forEach(entry -> results.put(entry.getValue(), entry.getKey()));
        return results;
    }

    public UUID getUuid(String discordId) {
        return linkedAccounts.get(discordId);
    }

    public Map<String, UUID> getManyUuids(Set<String> discordIds) {
        Map<String, UUID> results = new HashMap<>();
        linkedAccounts.entrySet().stream()
                .filter(entry -> discordIds.contains(entry.getKey()))
                .forEach(entry -> results.put(entry.getKey(), entry.getValue()));
        return results;
    }

    public void link(String discordId, UUID uuid) {
        DiscordSRV.debug("File backed link: " + discordId + ": " + uuid);

        // make sure the user isn't linked
        unlink(discordId);
        unlink(uuid);

        linkedAccounts.put(discordId, uuid);
        afterLink(discordId, uuid);
    }

    public void afterLink(String discordId, UUID uuid) {
        // call link event
        DiscordSRV.api.callEvent(new AccountLinkedEvent(DiscordUtil.getUserById(discordId), uuid));

        // trigger server commands
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        User user = DiscordUtil.getUserById(discordId);
        for (String command : DiscordSRV.config().getStringList("MinecraftDiscordAccountLinkedConsoleCommands")) {
            DiscordSRV.debug("Parsing command /" + command + " for linked commands...");
            command = command
                    .replace("%minecraftplayername%", PrettyUtil.beautifyUsername(offlinePlayer, "[Unknown Player]", false))
                    .replace("%minecraftdisplayname%", PrettyUtil.beautifyNickname(offlinePlayer, "[Unknown Player]", false))
                    .replace("%minecraftuuid%", uuid.toString())
                    .replace("%discordid%", discordId)
                    .replace("%discordname%", user != null ? user.getName() : "")
                    .replace("%discorddisplayname%", PrettyUtil.beautify(user, "", false));
            if (StringUtils.isBlank(command)) {
                DiscordSRV.debug("Command was blank, skipping");
                continue;
            }
            if (PluginUtil.pluginHookIsEnabled("placeholderapi")) command = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(uuid), command);

            String finalCommand = command;
            DiscordSRV.debug("Final command to be run: /" + finalCommand);
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }

        // group sync using the authoritative side
        if (DiscordSRV.config().getBoolean("GroupRoleSynchronizationOnLink") && DiscordSRV.getPlugin().getGroupSynchronizationManager().getPermissions() != null) {
            DiscordSRV.getPlugin().getGroupSynchronizationManager().resync(offlinePlayer, GroupSynchronizationManager.SyncDirection.AUTHORITATIVE, true);
        } else {
            String roleName = DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo");
            try {
                Role roleToAdd = DiscordUtil.getJda().getRolesByName(roleName, true).stream().findFirst().orElse(null);
                if (roleToAdd != null) {
                    Member member = roleToAdd.getGuild().getMemberById(discordId);
                    if (member != null) {
                        DiscordUtil.addRoleToMember(member, roleToAdd);
                    } else {
                        DiscordSRV.debug("Couldn't find member for " + offlinePlayer.getName() + " in " + roleToAdd.getGuild());
                    }
                } else {
                    DiscordSRV.debug("Couldn't find \"account linked\" role " + roleName + " to add to " + offlinePlayer.getName() + "'s linked Discord account");
                }
            } catch (Throwable t) {
                DiscordSRV.debug("Couldn't add \"account linked\" role \"" + roleName + "\" due to exception: " + ExceptionUtils.getMessage(t));
            }
        }

        // set user's discord nickname as their in-game name
        if (DiscordSRV.config().getBoolean("NicknameSynchronizationEnabled")) {
            DiscordSRV.getPlugin().getNicknameUpdater().setNickname(DiscordUtil.getMemberById(discordId), offlinePlayer);
        }
    }

    public void beforeUnlink(UUID uuid, String discordId) {
        if (DiscordSRV.getPlugin().isGroupRoleSynchronizationEnabled() && DiscordSRV.getPlugin().getGroupSynchronizationManager().getPermissions() != null) {
            DiscordSRV.getPlugin().getGroupSynchronizationManager().removeSynchronizedRoles(Bukkit.getOfflinePlayer(uuid));
        } else {
            try {
                // remove user from linked role
                Role role = DiscordUtil.getJda().getRolesByName(DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"), true).stream().findFirst().orElse(null);
                if (role != null) {
                    Member member = role.getGuild().getMemberById(discordId);
                    if (member != null) {
                        role.getGuild().removeRoleFromMember(member, role).queue();
                    } else {
                        DiscordSRV.debug("Couldn't remove \"linked\" role from null member: " + uuid);
                    }
                } else {
                    DiscordSRV.debug("Couldn't remove user from null \"linked\" role");
                }
            } catch (Throwable t) {
                DiscordSRV.debug("Failed to remove \"linked\" role from [" + uuid + ":" + discordId + "] during unlink: " + ExceptionUtils.getMessage(t));
            }
        }
    }

    public void unlink(UUID uuid) {
        String discordId = linkedAccounts.entrySet().stream()
                .filter(entry -> entry.getValue().equals(uuid))
                .map(Map.Entry::getKey)
                .findAny().orElse(null);
        if (discordId == null) return;

        synchronized (linkedAccounts) {
            beforeUnlink(uuid, discordId);

            linkedAccounts.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(uuid))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet()) // this needs to be collected in order to not modify array while iterating
                    .forEach(linkedAccounts::remove);
        }

        afterUnlink(uuid, discordId);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            DiscordSRV.getPlugin().getRequireLinkModule().noticePlayerUnlink(player);
        }
    }

    public void unlink(String discordId) {
        UUID uuid = linkedAccounts.get(discordId);
        if (uuid == null) return;

        synchronized (linkedAccounts) {
            beforeUnlink(uuid, discordId);
            linkedAccounts.remove(discordId);
        }
        afterUnlink(uuid, discordId);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            DiscordSRV.getPlugin().getRequireLinkModule().noticePlayerUnlink(player);
        }
    }

    public void afterUnlink(UUID uuid, String discordId) {
        Member member = DiscordUtil.getMemberById(discordId);

        DiscordSRV.api.callEvent(new AccountUnlinkedEvent(discordId, uuid));

        // run unlink console commands
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        User user = DiscordUtil.getUserById(discordId);
        for (String command : DiscordSRV.config().getStringList("MinecraftDiscordAccountUnlinkedConsoleCommands")) {
            command = command
                    .replace("%minecraftplayername%", PrettyUtil.beautifyUsername(offlinePlayer, "[Unknown player]", false))
                    .replace("%minecraftdisplayname%", PrettyUtil.beautifyNickname(offlinePlayer, "<Unknown name>", false))
                    .replace("%minecraftuuid%", uuid.toString())
                    .replace("%discordid%", discordId)
                    .replace("%discordname%", user != null ? user.getName() : "")
                    .replace("%discorddisplayname%", PrettyUtil.beautify(user, "", false));
            if (StringUtils.isBlank(command)) continue;
            if (PluginUtil.pluginHookIsEnabled("placeholderapi")) command = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(uuid), command);

            String finalCommand = command;
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }

        if (member != null) {
            if (member.getGuild().getSelfMember().canInteract(member)) {
                member.modifyNickname(null).queue();
            } else {
                DiscordSRV.debug("Can't remove nickname from " + member + ", bot is lower in hierarchy");
            }
        }
    }

    public void save() {
        long startTime = System.currentTimeMillis();

        try {
            JsonObject map = new JsonObject();
            linkedAccounts.forEach((discordId, uuid) -> map.addProperty(discordId, String.valueOf(uuid)));
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
