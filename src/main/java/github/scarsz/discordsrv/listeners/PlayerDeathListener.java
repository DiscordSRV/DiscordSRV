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
import github.scarsz.discordsrv.api.events.DeathMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DeathMessagePreProcessEvent;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    public PlayerDeathListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        String deathMessage = event.getDeathMessage();
        if (StringUtils.isBlank(deathMessage)) return;
        String message = LangUtil.Message.PLAYER_DEATH.toString();
        if (StringUtils.isBlank(message)) return;

        // respect invisibility plugins
        Player player = event.getEntity();
        if (PlayerUtil.isVanished(player)) return;

        String channelName = DiscordSRV.getPlugin().getMainChatChannel();

        DeathMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new DeathMessagePreProcessEvent(channelName, message, player, deathMessage));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug("DeathMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }
        // Update from event in case any listeners modified parameters
        deathMessage = preEvent.getDeathMessage();
        channelName = preEvent.getChannel();
        message = preEvent.getMessage();

        String discordMessage = message
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%username%", player.getName())
                .replace("%displayname%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(player.getDisplayName())))
                .replace("%world%", player.getWorld().getName())
                .replace("%deathmessage%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(deathMessage)));
        discordMessage = PlaceholderUtil.replacePlaceholdersToDiscord(discordMessage, player);

        discordMessage = DiscordUtil.strip(discordMessage);
        if (StringUtils.isBlank(discordMessage)) return;
        String lengthCheckMessage = discordMessage.replaceAll("[^A-z]", "");
        if (StringUtils.isBlank(lengthCheckMessage)) return;
        if (lengthCheckMessage.length() < 3) {
            DiscordSRV.debug("Not sending death message \"" + discordMessage + "\" because it's less than three characters long");
            return;
        }

        DeathMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new DeathMessagePostProcessEvent(channelName, discordMessage, player, deathMessage, preEvent.isCancelled()));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("DeathMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }
        // Update from event in case any listeners modified parameters
        channelName = postEvent.getChannel();
        discordMessage = postEvent.getProcessedMessage();

        TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);

        DiscordUtil.sendMessage(channel, discordMessage);
    }

}
