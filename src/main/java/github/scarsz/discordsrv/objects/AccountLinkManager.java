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
import org.bukkit.ChatColor;

import java.io.File;
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

    private final File linkFile;
    public AccountLinkManager(File linkFile) {
        this.linkFile = linkFile;

        if (!linkFile.exists()) return;
        linkedAccounts.clear();

        try {
            DiscordSRV.getPlugin().getGson().fromJson(FileUtils.readFileToString(linkFile, Charset.defaultCharset()), JsonObject.class).entrySet().forEach(entry -> {
                linkedAccounts.put(entry.getKey(), UUID.fromString(entry.getValue().getAsString()));
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
                Bukkit.getPlayer(getUuid(discordId)).sendMessage(ChatColor.AQUA + "Your UUID has been linked to Discord ID " + DiscordUtil.getJda().getUserById(discordId));

            return "Your Discord account has been linked to UUID " + getUuid(discordId) + " (" + Bukkit.getOfflinePlayer(getUuid(discordId)).getName() + ")";
        }

        if (StringUtils.isNumeric(linkCode)) return linkCode.length() == 4 ? "I don't know of such a code, try again." : "Are you sure that's your code? Link codes are 4 numbers long.";

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

        // trigger server command
        if (StringUtils.isNotBlank(DiscordSRV.getPlugin().getConfig().getString("MinecraftDiscordAccountLinkedConsoleCommand"))) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    DiscordSRV.getPlugin().getConfig().getString("MinecraftDiscordAccountLinkedConsoleCommand")
                            .replace("%minecraftplayername%", Bukkit.getOfflinePlayer(uuid).getName())
                            .replace("%minecraftuuid%", uuid.toString())
                            .replace("%discordid%", discordId)
                            .replace("%discordname%", DiscordUtil.getJda().getUserById(discordId).getName())
                            .replace("%discorddisplayname%", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getJda().getUserById(discordId)).getEffectiveName())
            ));

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
            FileUtils.writeStringToFile(linkFile, map.toString(), Charset.defaultCharset());
        } catch (IOException e) {
            DiscordSRV.error(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVE_FAILED + ": " + e.getMessage());
            return;
        }

        DiscordSRV.info(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVED.toString()
            .replace("{ms}", String.valueOf(System.currentTimeMillis() - startTime))
        );
    }

}
