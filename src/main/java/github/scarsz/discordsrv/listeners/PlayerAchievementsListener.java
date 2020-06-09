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
import github.scarsz.discordsrv.api.events.AchievementMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.AchievementMessagePreProcessEvent;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

import java.util.function.BiFunction;

@SuppressWarnings("deprecation")
public class PlayerAchievementsListener implements Listener {

    public PlayerAchievementsListener() {
        if (PlayerAchievementAwardedEvent.class.isAnnotationPresent(Deprecated.class)) return;

        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAchievementAwarded(PlayerAchievementAwardedEvent event) {
        // return if achievement or player objects are knackered because this can apparently happen for some reason
        if (event == null || event.getAchievement() == null || event.getPlayer() == null) return;

        // respect invisibility plugins
        if (PlayerUtil.isVanished(event.getPlayer())) return;

        // turn "ACHIEVEMENT_NAME" into "Achievement Name"
        String channelName = DiscordSRV.getPlugin().getMainChatChannel();
        String achievementName = PrettyUtil.beautify(event.getAchievement());

        Player player = event.getPlayer();

        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerAchievementMessage");
        if (messageFormat == null) return;

        AchievementMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new AchievementMessagePreProcessEvent(channelName, messageFormat, player, achievementName));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug("AchievementMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }
        // Update from event in case any listeners modified parameters
        achievementName = preEvent.getAchievementName();
        channelName = preEvent.getChannel();
        messageFormat = preEvent.getMessageFormat();

        if (messageFormat == null) return;

        String finalAchievementName = StringUtils.isNotBlank(achievementName) ? achievementName : "";
        String avatarUrl = DiscordSRV.getPlugin().getEmbedAvatarUrl(player);
        String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        String botName = DiscordSRV.getPlugin().getMainGuild() != null ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();
        String webhookName = messageFormat.getWebhookName();
        String webhookAvatarUrl = messageFormat.getWebhookAvatarUrl();
        String displayName = StringUtils.isNotBlank(player.getDisplayName()) ? player.getDisplayName() : "";

        TextChannel destinationChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
            if (content == null) return null;
            content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%username%", player.getName())
                    .replace("%displayname%", DiscordUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName))
                    .replace("%world%", player.getWorld().getName())
                    .replace("%achievement%", DiscordUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(finalAchievementName) : finalAchievementName))
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl)
                    .replace("%botname%", botName);
            if (destinationChannel != null) content = DiscordUtil.translateEmotes(content, destinationChannel.getGuild());
            content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
            return content;
        };
        Message discordMessage = DiscordSRV.getPlugin().translateMessage(messageFormat, translator);
        if (discordMessage == null) return;

        webhookName = translator.apply(webhookName, true);
        webhookAvatarUrl = translator.apply(webhookAvatarUrl, true);

        AchievementMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new AchievementMessagePostProcessEvent(channelName, discordMessage, player, achievementName, messageFormat.isUseWebhooks(), webhookName, webhookAvatarUrl, preEvent.isCancelled()));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("AchievementMessagePostProcessEvent was cancelled, message send aborted");
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
