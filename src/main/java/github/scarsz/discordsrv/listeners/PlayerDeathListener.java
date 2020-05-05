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
import github.scarsz.discordsrv.api.events.DeathMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DeathMessagePreProcessEvent;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.function.BiFunction;

public class PlayerDeathListener implements Listener {

    public PlayerDeathListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;

        String deathMessage = event.getDeathMessage();
        if (StringUtils.isBlank(deathMessage)) return;

        Player player = event.getEntity();

        // respect invisibility plugins
        if (PlayerUtil.isVanished(player)) return;

        String channelName = DiscordSRV.getPlugin().getMainChatChannel();
        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerDeathMessage");
        if (messageFormat == null) return;

        DeathMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new DeathMessagePreProcessEvent(channelName, messageFormat, player, deathMessage));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug("DeathMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }

        // Update from event in case any listeners modified parameters
        channelName = preEvent.getChannel();
        messageFormat = preEvent.getMessageFormat();
        deathMessage = preEvent.getDeathMessage();

        if (messageFormat == null) return;

        String finalDeathMessage = StringUtils.isNotBlank(deathMessage) ? deathMessage : "";
        String avatarUrl = DiscordSRV.getPlugin().getEmbedAvatarUrl(event.getEntity());
        String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        String botName = DiscordSRV.getPlugin().getMainGuild() != null ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();
        String webhookName = messageFormat.getWebhookName();
        String webhookAvatarUrl = messageFormat.getWebhookAvatarUrl();
        String displayName = StringUtils.isNotBlank(player.getDisplayName()) ? player.getDisplayName() : "";

        BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
            if (content == null) return null;
            content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%username%", player.getName())
                    .replace("%displayname%", DiscordUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName))
                    .replace("%world%", player.getWorld().getName())
                    .replace("%deathmessage%", DiscordUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(finalDeathMessage) : finalDeathMessage))
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl)
                    .replace("%botname%", botName);
            content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
            return content;
        };
        Message discordMessage = DiscordSRV.getPlugin().translateMessage(messageFormat, translator);
        if (discordMessage == null) return;

        webhookName = translator.apply(webhookName, true);
        webhookAvatarUrl = translator.apply(webhookAvatarUrl, true);

        if (DiscordSRV.getPlugin().getLength(discordMessage) < 3) {
            DiscordSRV.debug("Not sending death message, because it's less than three characters long. Message: " + messageFormat);
            return;
        }

        DeathMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new DeathMessagePostProcessEvent(channelName, discordMessage, player, deathMessage, messageFormat.isUseWebhooks(), webhookName, webhookAvatarUrl, preEvent.isCancelled()));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("DeathMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }

        // Update from event in case any listeners modified parameters
        channelName = postEvent.getChannel();
        discordMessage = postEvent.getDiscordMessage();

        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        if (messageFormat.isUseWebhooks()) {
            WebhookUtil.deliverMessage(textChannel, postEvent.getWebhookName(), postEvent.getWebhookAvatarUrl(),
                    discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
        } else {
            DiscordUtil.queueMessage(textChannel, discordMessage);
        }
    }

}
