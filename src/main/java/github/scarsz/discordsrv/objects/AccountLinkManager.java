package github.scarsz.discordsrv.objects;

import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Role;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 6:30 PM
 */
public class AccountLinkManager {

    @Getter private Map<String, UUID> linkingCodes = new HashMap<>();
    @Getter private Map<String, UUID> linkedAccounts = new HashMap<>();

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
        String code = String.valueOf(DiscordSRV.getPlugin().getRandom().nextInt(10000));
        linkingCodes.put(code, playerUuid);
        return code;
    }
    public String process(String linkCode, String discordId) {
        if (linkingCodes.containsKey(linkCode)) {
            link(discordId, linkingCodes.get(linkCode));
            linkingCodes.remove(linkCode);

            if (Bukkit.getPlayer(getUuid(discordId)).isOnline())
                Bukkit.getPlayer(getUuid(discordId)).sendMessage(LangUtil.Message.MINECRAFT_ACCOUNT_LINKED.toString()
                        .replace("%username%", DiscordUtil.getJda().getUserById(discordId).getName())
                        .replace("%id%", DiscordUtil.getJda().getUserById(discordId).getId())
                );

            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", Bukkit.getOfflinePlayer(getUuid(discordId)).getName())
                    .replace("%uuid%", getUuid(discordId).toString());
        }

        if (StringUtils.isNumeric(linkCode)) return linkCode.length() == 4
                ? LangUtil.InternalMessage.UNKNOWN_CODE.toString()
                : LangUtil.InternalMessage.INVALID_CODE.toString();

        return null;
    }

    public String getDiscordId(UUID uuid) {
        for (Map.Entry<String, UUID> linkedAccountSet : linkedAccounts.entrySet())
            if (linkedAccountSet.getValue().equals(uuid))
                return linkedAccountSet.getKey();
        return null;
    }
    public UUID getUuid(String discordId) {
        return linkedAccounts.get(discordId);
    }

    public void link(String discordId, UUID uuid) {
        linkedAccounts.put(discordId, uuid);

        // call link event
        DiscordSRV.api.callEvent(new AccountLinkedEvent(DiscordUtil.getJda().getUserById(discordId), uuid));

        // trigger server commands
        for (String command : DiscordSRV.getPlugin().getConfig().getStringList("MinecraftDiscordAccountLinkedConsoleCommands")) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            command = command
                    .replace("%minecraftplayername%", offlinePlayer.getName())
                    .replace("%minecraftdisplayname%", offlinePlayer.getPlayer() == null ? offlinePlayer.getName() : offlinePlayer.getPlayer().getDisplayName())
                    .replace("%minecraftuuid%", uuid.toString())
                    .replace("%discordid%", discordId)
                    .replace("%discordname%", DiscordUtil.getJda().getUserById(discordId).getName())
                    .replace("%discorddisplayname%", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getJda().getUserById(discordId)).getEffectiveName())
            ;

            if (StringUtils.isBlank(command)) continue;

            String finalCommand = command;
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
            DiscordSRV.getPlugin().getMetrics().increment("console_commands_processed");
        }

        // add user to role
        Role roleToAdd = DiscordUtil.getRole(DiscordSRV.getPlugin().getMainGuild(), DiscordSRV.getPlugin().getConfig().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
        if (roleToAdd != null) DiscordUtil.addRolesToMember(DiscordSRV.getPlugin().getMainGuild().getMemberById(discordId), roleToAdd);
        else DiscordSRV.debug("Couldn't add user to null roll");

        // set user's discord nickname as their in-game name
        if (DiscordSRV.getPlugin().getConfig().getBoolean("MinecraftDiscordAccountLinkedSetDiscordNicknameAsInGameName"))
            DiscordUtil.setNickname(DiscordSRV.getPlugin().getMainGuild().getMemberById(discordId), Bukkit.getOfflinePlayer(uuid).getName());
    }
    public void unlink(UUID uuid) {
        linkedAccounts.entrySet().stream().filter(entry -> entry.getValue().equals(uuid)).forEach(entry -> linkedAccounts.remove(entry.getKey()));
    }
    public void unlink(String discordId) {
        linkedAccounts.remove(discordId);
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
