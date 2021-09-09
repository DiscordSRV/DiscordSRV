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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordBanListener extends ListenerAdapter {

    @SuppressWarnings("deprecation") // something something paper component
    @Override
    public void onGuildBan(GuildBanEvent event) {
        UUID linkedUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
        if (linkedUuid == null) {
            DiscordSRV.debug(Debug.BAN_SYNCHRONIZATION, "Not handling ban for user " + event.getUser() + " because they didn't have a linked account");
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(linkedUuid);
        if (!offlinePlayer.hasPlayedBefore()) return;

        if (!DiscordSRV.config().getBoolean("BanSynchronizationDiscordToMinecraft")) {
            DiscordSRV.debug(Debug.BAN_SYNCHRONIZATION, "Not handling ban for user " + event.getUser() + " because doing so is disabled in the config");
            return;
        }

        String reason = LangUtil.Message.BAN_DISCORD_TO_MINECRAFT.toString();
        Bukkit.getBanList(BanList.Type.NAME).addBan(offlinePlayer.getName(), reason, null, "Discord");
        if (offlinePlayer.isOnline()) {
            // also kick them because adding them to the BanList isn't enough
            Player player = offlinePlayer.getPlayer();
            if (player != null) Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () -> player.kickPlayer(reason));
        }
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        UUID linkedUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
        if (linkedUuid == null) {
            DiscordSRV.debug(Debug.BAN_SYNCHRONIZATION, "Not handling unban for user " + event.getUser() + " because they didn't have a linked account");
            return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(linkedUuid);
        if (!offlinePlayer.hasPlayedBefore()) return;

        if (!DiscordSRV.config().getBoolean("BanSynchronizationDiscordToMinecraft")) {
            DiscordSRV.debug(Debug.BAN_SYNCHRONIZATION, "Not handling unban for user " + event.getUser() + " because doing so is disabled in the config");
            return;
        }

        String playerName = offlinePlayer.getName();

        if (StringUtils.isNotBlank(playerName)) //this literally should not happen but intellij likes bitching about not null checking
            Bukkit.getBanList(BanList.Type.NAME).pardon(playerName);
    }

}
