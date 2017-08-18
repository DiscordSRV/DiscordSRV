package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 10:27 PM
 */
public class DiscordBanListener extends ListenerAdapter {

    @Override
    public void onGuildBan(GuildBanEvent event) {
        UUID linkedUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
        if (linkedUuid == null) {
            DiscordSRV.debug("Not handling ban for user " + event.getUser() + " because they didn't have a linked account");
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(linkedUuid);
        if (!offlinePlayer.hasPlayedBefore()) return;

        if (!DiscordSRV.config().getBoolean("BanSynchronizationDiscordToMinecraft")) {
            DiscordSRV.debug("Not handling ban for user " + event.getUser() + " because doing so is disabled in the config");
            return;
        }

        Bukkit.getBanList(BanList.Type.NAME).addBan(offlinePlayer.getName(), LangUtil.Message.BAN_DISCORD_TO_MINECRAFT.toString(), null, "Discord");
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        UUID linkedUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
        if (linkedUuid == null) {
            DiscordSRV.debug("Not handling unban for user " + event.getUser() + " because they didn't have a linked account");
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(linkedUuid);
        if (!offlinePlayer.hasPlayedBefore()) return;

        if (!DiscordSRV.config().getBoolean("BanSynchronizationDiscordToMinecraft")) {
            DiscordSRV.debug("Not handling unban for user " + event.getUser() + " because doing so is disabled in the config");
            return;
        }

        Bukkit.getBanList(BanList.Type.NAME).pardon(offlinePlayer.getName());
    }

}
