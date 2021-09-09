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
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import github.scarsz.discordsrv.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveListener implements Listener {

    public PlayerJoinLeaveListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // if player is OP & update is available tell them
        if (GamePermissionUtil.hasPermission(player, "discordsrv.updatenotification") && DiscordSRV.updateIsAvailable) {
            MessageUtil.sendMessage(player, DiscordSRV.getPlugin().getDescription().getVersion().endsWith("-SNAPSHOT")
                    ? ChatColor.GRAY + "There is a newer development build of DiscordSRV available. Download it at https://snapshot.discordsrv.com/"
                    : ChatColor.AQUA + "An update to DiscordSRV is available. Download it at https://www.spigotmc.org/resources/discordsrv.18494/ or https://get.discordsrv.com"
            );
        }

        if (DiscordSRV.getPlugin().isGroupRoleSynchronizationEnabled()) {
            // trigger a synchronization for the player
            Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
                    DiscordSRV.getPlugin().getGroupSynchronizationManager().resync(
                            player,
                            GroupSynchronizationManager.SyncDirection.AUTHORITATIVE,
                            true,
                            GroupSynchronizationManager.SyncCause.PLAYER_JOIN
                    )
            );
        }

        if (PlayerUtil.isVanished(player)) {
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Not sending a join message for " + event.getPlayer().getName() + " because a vanish plugin reported them as vanished");
            return;
        }

        MessageFormat messageFormat = event.getPlayer().hasPlayedBefore()
                ? DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerJoinMessage")
                : DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerFirstJoinMessage");

        // make sure join messages enabled
        if (messageFormat == null) return;

        final String name = player.getName();

        // check if player has permission to not have join messages
        if (GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.silentjoin")) {
            DiscordSRV.info(LangUtil.InternalMessage.SILENT_JOIN.toString()
                    .replace("{player}", name)
            );
            return;
        }

        // player doesn't have silent join permission, send join message

        // schedule command to run in a second to be able to capture display name
        Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordSRV.getPlugin(), () ->
                DiscordSRV.getPlugin().sendJoinMessage(event.getPlayer(), event.getJoinMessage()), 20);

        // if enabled, set the player's discord nickname as their ign
        if (DiscordSRV.config().getBoolean("NicknameSynchronizationEnabled")) {
            Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
                final String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
                DiscordSRV.getPlugin().getNicknameUpdater().setNickname(DiscordUtil.getMemberById(discordId), player);
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) //priority needs to be different to MONITOR to avoid problems with permissions check when PEX is used, it is at lowest so that it executes before VanishNoPacket's player leave listener and is able to see whether the player is vanished
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (PlayerUtil.isVanished(player)) {
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Not sending a quit message for " + event.getPlayer().getName() + " because a vanish plugin reported them as vanished");
            return;
        }

        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerLeaveMessage");

        // make sure quit messages enabled
        if (messageFormat == null) return;

        final String name = player.getName();

        // no quit message, user shouldn't have one from permission
        if (GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.silentquit")) {
            DiscordSRV.info(LangUtil.InternalMessage.SILENT_QUIT.toString()
                    .replace("{player}", name)
            );
            return;
        }

        // player doesn't have silent quit, show quit message
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(),
                () -> DiscordSRV.getPlugin().sendLeaveMessage(event.getPlayer(), event.getQuitMessage()));
    }

}
