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
import github.scarsz.discordsrv.api.events.AchievementMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.AchievementMessagePreProcessEvent;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;

public class PlayerAchievementsListener {

    @SuppressWarnings("Convert2Lambda")
    private final RegisteredListener registeredListener = new RegisteredListener(
            new Listener() {},
            new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) {
                    onPlayerAchievementAwarded(event);
                }
            },
            EventPriority.MONITOR,
            DiscordSRV.getPlugin(),
            false
    );
    private HandlerList handlerList;

    public PlayerAchievementsListener() {
        try {
            Class<?> achievementAwardedEventClass = Class.forName("org.bukkit.event.player.PlayerAchievementAwardedEvent");

            // in MC <1.16 servers, the achievement class is present but has a deprecated annotation
            if (achievementAwardedEventClass.isAnnotationPresent(Deprecated.class)) return;

            handlerList = (HandlerList) achievementAwardedEventClass.getMethod("getHandlerList").invoke(null);
            handlerList.register(registeredListener);
        } catch (ClassNotFoundException ignored) {
            // achievement class not found, MC >= 1.16
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            DiscordSRV.error("Failed to get the handler list for PlayerAchievementAwardedEvent, achievement events will not function");
        }
    }

    public void onPlayerAchievementAwarded(Event event) {
        // return if achievement or player objects are knackered because this can apparently happen for some reason
        if (event == null) return;

        Player player = ((PlayerEvent) event).getPlayer();

        // respect invisibility plugins
        if (PlayerUtil.isVanished(player)) return;

        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> runAsync(event, player));
    }

    private void runAsync(Event event, Player player) {
        Enum<?> achievement;
        try {
            achievement = (Enum<?>) event.getClass().getMethod("getAchievement").invoke(event);
            if (achievement == null) return;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            DiscordSRV.error("Failed to get achievement name from " + event.getEventName() + ": " + e.getMessage());
            handlerList.unregister(registeredListener);
            return;
        }

        // turn "ACHIEVEMENT_NAME" into "Achievement Name"
        String channelName = DiscordSRV.getPlugin().getOptionalChannel("awards");
        String achievementName = PrettyUtil.beautify(achievement);

        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerAchievementMessage");
        if (messageFormat == null) return;

        AchievementMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new AchievementMessagePreProcessEvent(channelName, messageFormat, player, achievementName, event));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "AchievementMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }
        // Update from event in case any listeners modified parameters
        achievementName = preEvent.getAchievementName();
        channelName = preEvent.getChannel();
        messageFormat = preEvent.getMessageFormat();

        if (messageFormat == null) return;

        String finalAchievementName = StringUtils.isNotBlank(achievementName) ? achievementName : "";
        String avatarUrl = DiscordSRV.getAvatarUrl(player);
        String botAvatarUrl = DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
        String botName = DiscordSRV.getPlugin().getMainGuild() != null ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();
        String displayName = StringUtils.isNotBlank(player.getDisplayName()) ? MessageUtil.strip(player.getDisplayName()) : "";

        TextChannel destinationChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
            if (content == null) return null;
            content = content
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%username%", needsEscape ? DiscordUtil.escapeMarkdown(player.getName()) : player.getName())
                    .replace("%displayname%", needsEscape ? DiscordUtil.escapeMarkdown(displayName) : displayName)
                    .replace("%usernamenoescapes%", player.getName())
                    .replace("%displaynamenoescapes%", displayName)
                    .replace("%world%", player.getWorld().getName())
                    .replace("%achievement%", MessageUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(finalAchievementName) : finalAchievementName))
                    .replace("%embedavatarurl%", avatarUrl)
                    .replace("%botavatarurl%", botAvatarUrl)
                    .replace("%botname%", botName);
            if (destinationChannel != null) content = DiscordUtil.translateEmotes(content, destinationChannel.getGuild());
            content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
            return content;
        };
        Message discordMessage = DiscordSRV.translateMessage(messageFormat, translator);
        if (discordMessage == null) return;

        String webhookName = translator.apply(messageFormat.getWebhookName(), false);
        String webhookAvatarUrl = translator.apply(messageFormat.getWebhookAvatarUrl(), false);

        AchievementMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new AchievementMessagePostProcessEvent(channelName, discordMessage, player, achievementName, event, messageFormat.isUseWebhooks(), webhookName, webhookAvatarUrl, preEvent.isCancelled()));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "AchievementMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }

        // Update from event in case any listeners modified parameters
        channelName = postEvent.getChannel();
        discordMessage = postEvent.getDiscordMessage();

        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        if (postEvent.isUsingWebhooks()) {
            WebhookUtil.deliverMessage(textChannel, postEvent.getWebhookName(), postEvent.getWebhookAvatarUrl(),
                    discordMessage.getContentRaw(), discordMessage.getEmbeds().stream().findFirst().orElse(null));
        } else {
            DiscordUtil.queueMessage(textChannel, discordMessage, true);
        }
    }

}
