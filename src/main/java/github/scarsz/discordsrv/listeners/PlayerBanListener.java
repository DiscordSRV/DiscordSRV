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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class PlayerBanListener implements Listener {

    public PlayerBanListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> {
            if (Bukkit.getBannedPlayers().contains(Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId()))) {
                if (event.getPlayer() instanceof OfflinePlayer) {
                    if (!DiscordSRV.config().getBoolean("BanSynchronizationMinecraftToDiscord")) {
                        DiscordSRV.debug("Not handling ban for player " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ") because doing so is disabled in the config");
                        return;
                    }

                    DiscordSRV.debug("Handling ban for player " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ")");
                    DiscordUtil.banMember(DiscordUtil.getMemberById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(event.getPlayer().getUniqueId())));
                }
            }
        }, 20);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!DiscordSRV.config().getBoolean("BanSynchronizationMinecraftToDiscord")) {
            DiscordSRV.debug("Not handling possible unban for player " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ") because doing so is disabled in the config");
            return;
        }

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(event.getPlayer().getUniqueId());
        if (discordId == null) return;

        DiscordSRV.getPlugin().getMainGuild().retrieveBanById(discordId).queue(ban -> {
            DiscordSRV.info("Unbanning player " + event.getPlayer().getName() + " from Discord (ID " + discordId + ") because they aren't banned on the server");
            DiscordSRV.getPlugin().getMainGuild().unban(discordId).queue();
        }, failure ->
            DiscordSRV.debug("Failed to unban player " + event.getPlayer().getName() + ": " + failure.getMessage())
        );
    }

}
