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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerJoinLeaveListener implements Listener {

    public PlayerJoinLeaveListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // if player is OP & update is available tell them
        if (GamePermissionUtil.hasPermission(player, "discordsrv.updatenotification") && DiscordSRV.updateIsAvailable) {
            event.getPlayer().sendMessage(ChatColor.AQUA + "An update to DiscordSRV is available. Download it at https://www.spigotmc.org/resources/discordsrv.18494/");
        }

        if (DiscordSRV.isGroupRoleSynchronizationEnabled()) {
            // trigger a synchronization for the player
            DiscordSRV.getPlugin().getGroupSynchronizationManager().resync(player);
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
        final UUID uuid = player.getUniqueId();
        final String message = event.getJoinMessage();
        final boolean webhookDelivery = DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageDelivery");
        final String avatarUrl = DiscordSRV.getPlugin().getEmbedAvatarUrl(player);
        final String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();

        // schedule command to run in a second to be able to capture display name
        Bukkit.getScheduler().runTaskLater(DiscordSRV.getPlugin(), () -> {
            final String displayName = player.getDisplayName();
            final Message discordMessage = DiscordSRV.getPlugin().translateMessage(messageFormat, content -> {
                content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%message%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(message)))
                    .replace("%username%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(name)))
                    .replace("%displayname%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(displayName)))
                    .replace("%effectiveavatarurl%", webhookDelivery ? botAvatarUrl : avatarUrl)
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl);
                content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
                return content;
            });

            if (webhookDelivery) {
                WebhookUtil.deliverMessage(DiscordSRV.getPlugin().getMainTextChannel(), uuid, name, displayName,
                        discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
            } else {
                DiscordUtil.queueMessage(DiscordSRV.getPlugin().getMainTextChannel(), discordMessage);
            }
        }, 20);

        // if enabled, set the player's discord nickname as their ign
        if (DiscordSRV.config().getBoolean("NicknameSynchronizationEnabled")) {
            final String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);
            DiscordSRV.getPlugin().getNicknameUpdater().setNickname(DiscordUtil.getMemberById(discordId), player);
        }
    }

    @EventHandler //priority needs to be different to MONITOR to avoid problems with permissions check when PEX is used
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerLeaveMessage");

        // make sure quit messages enabled
        if (messageFormat == null) return;

        final Player player = event.getPlayer();
        final String name = player.getName();

        // no quit message, user shouldn't have one from permission
        if (GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.silentquit")) {
            DiscordSRV.info(LangUtil.InternalMessage.SILENT_QUIT.toString()
                    .replace("{player}", name)
            );
            return;
        }

        final UUID uuid = player.getUniqueId();
        final String displayName = player.getDisplayName();
        final String message = event.getQuitMessage();

        final boolean webhookDelivery = DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageDelivery");
        final String avatarUrl = DiscordSRV.getPlugin().getEmbedAvatarUrl(event.getPlayer());
        final String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        Message discordMessage = DiscordSRV.getPlugin().translateMessage(messageFormat, content -> {
            content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%message%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(message)))
                    .replace("%username%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(name)))
                    .replace("%displayname%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(DiscordUtil.strip(displayName))))
                    .replace("%effectiveavatarurl%", webhookDelivery ? botAvatarUrl : avatarUrl)
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl);
            content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
            return content;
        });

        // player doesn't have silent quit, show quit message
        if (webhookDelivery) {
            WebhookUtil.deliverMessage(DiscordSRV.getPlugin().getMainTextChannel(), uuid, name, displayName,
                    discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
        } else {
            DiscordUtil.queueMessage(DiscordSRV.getPlugin().getMainTextChannel(), discordMessage);
        }
    }

}
