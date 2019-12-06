/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
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
import github.scarsz.discordsrv.util.GroupSynchronizationUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AccountLinkManager {

    @Getter private final Map<String, UUID> linkingCodes = new HashMap<>();
    @Getter private final Map<String, UUID> linkedAccounts = new HashMap<>();

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
                        .replace("{username}", String.valueOf(offlinePlayer != null ? offlinePlayer.getName() : "[Unknown]"))
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
                    .replace("%name%", player != null && player.getName() != null ? player.getName() : "<Unknown>")
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

    public UUID getUuid(String discordId) {
        return linkedAccounts.get(discordId);
    }

    public void link(String discordId, UUID uuid) {
        linkedAccounts.put(discordId, uuid);
        afterLink(discordId, uuid);
    }

    public void afterLink(String discordId, UUID uuid) {
        // call link event
        DiscordSRV.api.callEvent(new AccountLinkedEvent(DiscordUtil.getUserById(discordId), uuid));

        // trigger server commands
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        for (String command : DiscordSRV.config().getStringList("MinecraftDiscordAccountLinkedConsoleCommands")) {
            //noinspection ConstantConditions (never know with bukkit)
            if (offlinePlayer != null) {
                command = command
                        .replace("%minecraftplayername%", offlinePlayer != null && offlinePlayer.getName() != null ? offlinePlayer.getName() : "[Unknown Player]")
                        .replace("%minecraftdisplayname%", offlinePlayer != null ? offlinePlayer.getPlayer() == null
                                ? offlinePlayer.getName() != null
                                ? offlinePlayer.getName() : "[Unknown Player]"
                                : offlinePlayer.getPlayer().getDisplayName() : "[Unknown Player]");
            } else {
                command = command.replaceAll("%minecraftplayername%|%minecraftdisplayname%", "");
            }
            command = command
                    .replace("%minecraftuuid%", uuid.toString())
                    .replace("%discordid%", discordId)
                    .replace("%discordname%", DiscordUtil.getUserById(discordId) != null ? DiscordUtil.getUserById(discordId).getName() : "")
                    .replace("%discorddisplayname%", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getUserById(discordId)).getEffectiveName());
            if (StringUtils.isBlank(command)) continue;
            if (PluginUtil.pluginHookIsEnabled("placeholderapi")) command = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(uuid), command);

            String finalCommand = command;
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }

        // add user to role
        Role roleToAdd = DiscordUtil.getRole(DiscordSRV.getPlugin().getMainGuild(), DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
        if (roleToAdd != null) DiscordUtil.addRolesToMember(DiscordUtil.getMemberById(discordId), roleToAdd);
        else DiscordSRV.debug("Couldn't add user to null role");

        // set user's discord nickname as their in-game name
        if (DiscordSRV.config().getBoolean("MinecraftDiscordAccountLinkedSetDiscordNicknameAsInGameName")) {
            String nickname;
            if (offlinePlayer != null) {
                Player player = offlinePlayer.getPlayer();
                if (player != null) {
                    String displayName = player.getDisplayName();
                    if (!StringUtils.isEmpty(displayName)) {
                        nickname = DiscordUtil.strip(displayName);
                    } else {
                        nickname = player.getName();
                    }
                } else {
                    nickname = offlinePlayer.getName();
                }
            } else {
                nickname = "[Unknown]";
            }

            DiscordUtil.setNickname(DiscordUtil.getMemberById(discordId), nickname);
        }
    }

    public void beforeUnlink(UUID uuid, String discord) {
        if (DiscordSRV.config().getBoolean("GroupRoleSynchronizationRemoveRolesOnUnlink")) {
            GroupSynchronizationUtil.reSyncGroups(Bukkit.getPlayer(uuid), true);
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
    }

    public void afterUnlink(UUID uuid, String discordId) {
        Member member = DiscordUtil.getMemberById(discordId);

        // remove user from linked role
        Role roleToRemove = DiscordUtil.getRole(DiscordSRV.getPlugin().getMainGuild(), DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
        if (roleToRemove != null) DiscordUtil.removeRolesFromMember(member, roleToRemove);
        else DiscordSRV.debug("Couldn't remove user from null role");

        DiscordSRV.api.callEvent(new AccountUnlinkedEvent(discordId, uuid));

        // run unlink console commands
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        for (String command : DiscordSRV.config().getStringList("MinecraftDiscordAccountUnlinkedConsoleCommands")) {
            command = command
                    .replace("%minecraftplayername%", offlinePlayer.getName() != null ? offlinePlayer.getName() : "[Unknown player]")
                    .replace("%minecraftdisplayname%", offlinePlayer.getPlayer() == null ? (offlinePlayer.getName() != null ? offlinePlayer.getName() : "<Unknown name>") : offlinePlayer.getPlayer().getDisplayName())
                    .replace("%minecraftuuid%", uuid.toString())
                    .replace("%discordid%", discordId)
                    .replace("%discordname%", DiscordUtil.getUserById(discordId) != null ? DiscordUtil.getUserById(discordId).getName() : "")
                    .replace("%discorddisplayname%", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getUserById(discordId)).getEffectiveName());
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
