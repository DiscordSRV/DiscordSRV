/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2017 Austin Shapiro AKA Scarsz
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
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.GroupSynchronizationUtil;
import github.scarsz.discordsrv.util.LangUtil;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AccountLinkManager {

    @Getter
    private final Map<String, UUID> linkingCodes = new HashMap<>();
    @Getter
    private final Map<String, UUID> linkedAccounts = new HashMap<>();

    public AccountLinkManager() {
        if (!DiscordSRV.getPlugin().getLinkedAccountsFile().exists()) return;
        linkedAccounts.clear();

        try {
            DiscordSRV.getPlugin().getGson().fromJson(FileUtils.readFileToString(DiscordSRV.getPlugin().getLinkedAccountsFile(), Charset.forName("UTF-8")), JsonObject.class).entrySet().forEach(entry -> {
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
        int code = 0;
        while (code < 1000) code = DiscordSRV.getPlugin().getRandom().nextInt(10000);
        linkingCodes.put(String.valueOf(code), playerUuid);
        return String.valueOf(code);
    }

    public String process(String linkCode, String discordId) {
        // strip the code to get rid of non-numeric characters
        linkCode = linkCode.replaceAll("[^0-9]", "");

        if (linkingCodes.containsKey(linkCode)) {
            link(discordId, linkingCodes.get(linkCode));
            linkingCodes.remove(linkCode);

            if (Bukkit.getPlayer(getUuid(discordId)).isOnline())
                Bukkit.getPlayer(getUuid(discordId)).sendMessage(LangUtil.Message.MINECRAFT_ACCOUNT_LINKED.toString()
                        .replace("%username%", DiscordUtil.getUserById(discordId).getName())
                        .replace("%id%", DiscordUtil.getUserById(discordId).getId())
                );

            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", Bukkit.getOfflinePlayer(getUuid(discordId)).getName())
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

        // call link event
        DiscordSRV.api.callEvent(new AccountLinkedEvent(DiscordUtil.getUserById(discordId), uuid));

        // trigger server commands
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        for (String command : DiscordSRV.config().getStringList("MinecraftDiscordAccountLinkedConsoleCommands")) {
            if (offlinePlayer == null) continue;

            command = command
                    .replace("%minecraftplayername%", offlinePlayer.getName())
                    .replace("%minecraftdisplayname%", offlinePlayer.getPlayer() == null ? offlinePlayer.getName() : offlinePlayer.getPlayer().getDisplayName())
                    .replace("%minecraftuuid%", uuid.toString())
                    .replace("%discordid%", discordId)
                    .replace("%discordname%", DiscordUtil.getUserById(discordId).getName())
                    .replace("%discorddisplayname%", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getUserById(discordId)).getEffectiveName())
            ;

            if (StringUtils.isBlank(command)) continue;

            String finalCommand = command;
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }

        // add user to role
        Role roleToAdd = DiscordUtil.getRole(DiscordSRV.getPlugin().getMainGuild(), DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
        if (roleToAdd != null) DiscordUtil.addRolesToMember(DiscordUtil.getMemberById(discordId), roleToAdd);
        else DiscordSRV.debug("Couldn't add user to null role");

        // set user's discord nickname as their in-game name
        if (DiscordSRV.config().getBoolean("MinecraftDiscordAccountLinkedSetDiscordNicknameAsInGameName"))
            DiscordUtil.setNickname(DiscordUtil.getMemberById(discordId), Bukkit.getOfflinePlayer(uuid).getName());
    }

    public void unlink(UUID uuid) {
        Map.Entry<String, UUID> linkedAccount = linkedAccounts.entrySet().stream().filter(entry -> entry.getValue().equals(uuid)).findAny().orElse(null);
        if (linkedAccount == null) return;

        synchronized (linkedAccounts) {
            if (DiscordSRV.config().getBoolean("GroupRoleSynchronizationRemoveRolesOnUnlink")) {
                GroupSynchronizationUtil.reSyncGroups(Bukkit.getPlayer(uuid), true);
            }

            List<Map.Entry<String, UUID>> entriesToRemove = linkedAccounts.entrySet().stream().filter(entry -> entry.getValue().equals(uuid)).collect(Collectors.toList());
            entriesToRemove.forEach(entry -> linkedAccounts.remove(entry.getKey()));
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        for (String command : DiscordSRV.config().getStringList("MinecraftDiscordAccountUnlinkedConsoleCommands")) {
            if (offlinePlayer == null) continue;

            command = command
                    .replace("%minecraftplayername%", offlinePlayer.getName())
                    .replace("%minecraftdisplayname%", offlinePlayer.getPlayer() == null ? offlinePlayer.getName() : offlinePlayer.getPlayer().getDisplayName())
                    .replace("%minecraftuuid%", uuid.toString())
                    .replace("%discordid%", linkedAccount.getKey())
                    .replace("%discordname%", DiscordUtil.getUserById(linkedAccount.getKey()).getName())
                    .replace("%discorddisplayname%", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getUserById(linkedAccount.getKey())).getEffectiveName())
            ;

            if (StringUtils.isBlank(command)) continue;

            String finalCommand = command;
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
    }

    public void unlink(String discordId) {
        UUID uuid = linkedAccounts.get(discordId);
        User user = DiscordUtil.getUserById(discordId);
        linkedAccounts.remove(discordId);

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        for (String command : DiscordSRV.config().getStringList("MinecraftDiscordAccountUnlinkedConsoleCommands")) {
            if (offlinePlayer == null) continue;

            command = command
                    .replace("%minecraftplayername%", offlinePlayer.getName())
                    .replace("%minecraftdisplayname%", offlinePlayer.getPlayer() == null ? offlinePlayer.getName() : offlinePlayer.getPlayer().getDisplayName())
                    .replace("%minecraftuuid%", uuid.toString())
                    .replace("%discordid%", user.getId())
                    .replace("%discordname%", DiscordUtil.getUserById(user.getId()).getName())
                    .replace("%discorddisplayname%", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getUserById(user.getId())).getEffectiveName())
            ;

            if (StringUtils.isBlank(command)) continue;

            String finalCommand = command;
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
    }

    public void save() {
        if (linkedAccounts.size() == 0) {
            DiscordSRV.info(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVE_SKIPPED);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            JsonObject map = new JsonObject();
            linkedAccounts.forEach((discordId, uuid) -> map.addProperty(discordId, String.valueOf(uuid)));
            FileUtils.writeStringToFile(DiscordSRV.getPlugin().getLinkedAccountsFile(), map.toString(), Charset.forName("UTF-8"));
        } catch (IOException e) {
            DiscordSRV.error(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVE_FAILED + ": " + e.getMessage());
            return;
        }

        DiscordSRV.info(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVED.toString()
                .replace("{ms}", String.valueOf(System.currentTimeMillis() - startTime))
        );
    }

}
