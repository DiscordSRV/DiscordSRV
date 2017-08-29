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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    public void PlayerJoinEvent(PlayerJoinEvent event) {
        // if player is OP & update is available tell them
        if (GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.updatenotification") && DiscordSRV.updateIsAvailable) {
            event.getPlayer().sendMessage(ChatColor.AQUA + "An update to DiscordSRV is available. Download it at https://www.spigotmc.org/resources/discordsrv.18494/");
        }

        // trigger a synchronization for the player
        GroupSynchronizationUtil.reSyncGroups(event.getPlayer());

        String joinMessageFormat = event.getPlayer().hasPlayedBefore()
                ? LangUtil.Message.PLAYER_JOIN.toString()
                : LangUtil.Message.PLAYER_JOIN_FIRST_TIME.toString()
        ;

        // make sure join messages enabled
        if (StringUtils.isBlank(joinMessageFormat)) return;

        // check if player has permission to not have join messages
        if (GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.silentjoin")) {
            DiscordSRV.info(LangUtil.InternalMessage.SILENT_JOIN.toString()
                    .replace("{player}", event.getPlayer().getName())
            );
            return;
        }

        // schedule command to run in a second to be able to capture display name
        Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> {
            String discordMessage = joinMessageFormat
                    .replace("%username%", DiscordUtil.escapeMarkdown(event.getPlayer().getName()))
                    .replace("%displayname%", DiscordUtil.escapeMarkdown(DiscordUtil.strip(event.getPlayer().getDisplayName())));
            if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(event.getPlayer(), discordMessage);

            // player doesn't have silent join permission, send join message
            DiscordUtil.queueMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordUtil.strip(discordMessage));
        }, 20);

        // if enabled, set the player's discord nickname as their ign
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(event.getPlayer().getUniqueId());
        if (discordId != null && DiscordSRV.config().getBoolean("MinecraftDiscordAccountLinkedSetDiscordNicknameAsInGameName")) {
            User discordUser = DiscordUtil.getUserById(discordId);
            if (!DiscordSRV.getPlugin().getMainGuild().getMember(discordUser).getEffectiveName().equals(event.getPlayer().getName()))
                DiscordUtil.setNickname(DiscordSRV.getPlugin().getMainGuild().getMember(discordUser), event.getPlayer().getName());
        }
    }

    @EventHandler //priority needs to be different to MONITOR to avoid problems with permissions check when PEX is used
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        // make sure quit messages enabled
        if (StringUtils.isBlank(LangUtil.Message.PLAYER_LEAVE.toString())) return;

        // no quit message, user shouldn't have one from permission
        if (GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.silentquit")) {
            DiscordSRV.info(LangUtil.InternalMessage.SILENT_QUIT.toString()
                    .replace("{player}", event.getPlayer().getName())
            );
            return;
        }

        String discordMessage = LangUtil.Message.PLAYER_LEAVE.toString()
                .replace("%username%", DiscordUtil.escapeMarkdown(event.getPlayer().getName()))
                .replace("%displayname%", DiscordUtil.escapeMarkdown(DiscordUtil.strip(event.getPlayer().getDisplayName())));
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(event.getPlayer(), discordMessage);

        // player doesn't have silent quit, show quit message
        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordUtil.strip(discordMessage));
    }

}
