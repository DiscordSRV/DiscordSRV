package github.scarsz.discordsrv.objects;

import com.google.gson.internal.LinkedTreeMap;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
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
import java.util.Random;
import java.util.UUID;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 6:30 PM
 */
public class AccountLinkManager {

    private Map<String, UUID> linkingCodes = new HashMap<>();
    private Map<String, UUID> linkedAccounts = new HashMap<>();
    private Random random = new Random();

    private final File linkFile;
    public AccountLinkManager(File linkFile) {
        this.linkFile = linkFile;

        load();
    }

    public String generateCode(UUID playerUuid) {
        String code = String.valueOf(random.nextInt(10000));
        linkingCodes.put(code, playerUuid);
        return code;
    }
    public String process(String linkCode, String discordId) {
        if (linkingCodes.containsKey(linkCode)) {
            link(discordId, linkingCodes.get(linkCode));
            linkingCodes.remove(linkCode);

            if (Bukkit.getPlayer(getUuid(discordId)).isOnline())
                Bukkit.getPlayer(getUuid(discordId)).sendMessage(ChatColor.AQUA + "Your UUID has been linked to Discord ID " + DiscordSRV.getPlugin().getJda().getUserById(discordId));

            return "Your Discord account has been linked to UUID " + getUuid(discordId) + " (" + Bukkit.getOfflinePlayer(getUuid(discordId)).getName() + ")";
        }

        if (StringUtils.isNumeric(linkCode) && linkCode.length() != 4) return "Are you sure that's your code? Link codes are 4 characters long.";
        if (StringUtils.isNumeric(linkCode)) return "I don't know of such a code, try again.";

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

        // trigger server command if wanted
        if (StringUtils.isNotBlank(DiscordSRV.getPlugin().getConfig().getString("MinecraftDiscordAccountLinkedConsoleCommand"))) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    DiscordSRV.getPlugin().getConfig().getString("MinecraftDiscordAccountLinkedConsoleCommand")
                            .replace("%minecraftplayername%", Bukkit.getOfflinePlayer(uuid).getName())
                            .replace("%minecraftuuid%", uuid.toString())
                            .replace("%discordid%", discordId)
                            .replace("%discordname%", DiscordUtil.getJda().getUserById(discordId).getName())
                            .replace("%discorddisplayname%", DiscordSRV.getPlugin().getMainTextChannel().getGuild().getMember(DiscordUtil.getJda().getUserById(discordId)).getEffectiveName())
            ));
        }

        // add user to role if wanted
        Role roleToAdd = DiscordSRV.getPlugin().getMainTextChannel().getGuild().getRoleById(DiscordSRV.getPlugin().getConfig().getString("MinecraftDiscordAccountLinkedRoleIdToAddUserTo"));
        if (roleToAdd != null) DiscordUtil.addRolesToMember(DiscordSRV.getPlugin().getMainTextChannel().getGuild().getMemberById(discordId), roleToAdd);
    }
    public void unlink(UUID uuid) {
        linkedAccounts.remove(uuid);
    }
    public void unlink(String discordId) {
        linkedAccounts.entrySet().stream().filter(entry -> entry.getValue().equals(discordId)).forEach(entry -> linkedAccounts.remove(entry.getKey()));
    }

    public void load() {
        if (!linkFile.exists()) return;
        linkedAccounts.clear();

        try {
            LinkedTreeMap<String, String> mapFromFile = DiscordSRV.getPlugin().getGson().fromJson(FileUtils.readFileToString(linkFile, Charset.defaultCharset()), LinkedTreeMap.class);
            mapFromFile.forEach((discordId, uuid) -> linkedAccounts.put(discordId, UUID.fromString(uuid)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void save() {
        if (linkedAccounts.size() == 0) {
            DiscordSRV.info("Skipped saving linked accounts because there were none");
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            HashMap<String, String> linkedAccountsStringMap = new HashMap<>();
            linkedAccounts.forEach((discordId, uuid) -> linkedAccountsStringMap.put(discordId, String.valueOf(uuid)));
            FileUtils.writeStringToFile(linkFile, DiscordSRV.getPlugin().getGson().toJson(linkedAccountsStringMap), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        DiscordSRV.info("Saved linked accounts in " + (System.currentTimeMillis() - startTime) + "ms");
    }

}
