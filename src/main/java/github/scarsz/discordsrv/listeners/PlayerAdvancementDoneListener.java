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
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class PlayerAdvancementDoneListener implements Listener {

    public PlayerAdvancementDoneListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        // return if advancement or player objects are knackered because this can apparently happen for some reason
        if (event.getAdvancement() == null || event.getAdvancement().getKey().getKey().contains("recipe/") || event.getPlayer() == null) return;

        // respect invisibility plugins
        if (PlayerUtil.isVanished(event.getPlayer())) return;

        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> runAsync(event));
    }

    private void runAsync(PlayerAdvancementDoneEvent event) {
        try {
            Object craftAdvancement = ((Object) event.getAdvancement()).getClass().getMethod("getHandle").invoke(event.getAdvancement());
            Object advancementDisplay = craftAdvancement.getClass().getMethod("c").invoke(craftAdvancement);
            boolean display = (boolean) advancementDisplay.getClass().getMethod("i").invoke(advancementDisplay);
            if (!display) return;
        } catch (NullPointerException e) {
            return;
        } catch (Exception e) {
            DiscordSRV.error(e);
            return;
        }

        String channelName = DiscordSRV.getPlugin().getOptionalChannel("awards");
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("MinecraftPlayerAchievementMessage");
        if (messageFormat == null) return;

        // turn "story/advancement_name" into "Advancement Name"
        String advancementTitle = getTitle(advancement);

        AchievementMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new AchievementMessagePreProcessEvent(channelName, messageFormat, player, advancementTitle, event));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "AchievementMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }
        // Update from event in case any listeners modified parameters
        advancementTitle = preEvent.getAchievementName();
        channelName = preEvent.getChannel();
        messageFormat = preEvent.getMessageFormat();

        if (messageFormat == null) return;

        String finalAchievementName = StringUtils.isNotBlank(advancementTitle) ? advancementTitle : "";
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

        AchievementMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new AchievementMessagePostProcessEvent(channelName, discordMessage, player, advancementTitle, event, messageFormat.isUseWebhooks(), webhookName, webhookAvatarUrl, preEvent.isCancelled()));
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

    private static final Map<Advancement, String> ADVANCEMENT_TITLE_CACHE = new ConcurrentHashMap<>();
    public static String getTitle(Advancement advancement) {
        return ADVANCEMENT_TITLE_CACHE.computeIfAbsent(advancement, v -> {
            try {
                Object handle = advancement.getClass().getMethod("getHandle").invoke(advancement);
                Object advancementDisplay = Arrays.stream(handle.getClass().getMethods())
                        .filter(method -> method.getReturnType().getSimpleName().equals("AdvancementDisplay"))
                        .filter(method -> method.getParameterCount() == 0)
                        .findFirst().orElseThrow(() -> new RuntimeException("Failed to find AdvancementDisplay getter for advancement handle"))
                        .invoke(handle);
                if (advancementDisplay == null) throw new RuntimeException("Advancement doesn't have display properties");

                try {
                    Field advancementMessageField = advancementDisplay.getClass().getDeclaredField("a");
                    advancementMessageField.setAccessible(true);
                    Object advancementMessage = advancementMessageField.get(advancementDisplay);
                    Object advancementTitle = advancementMessage.getClass().getMethod("getString").invoke(advancementMessage);
                    return (String) advancementTitle;
                } catch (Exception e) {
                    DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Failed to get title of advancement using getString, trying JSON method");
                }

                Field titleComponentField = Arrays.stream(advancementDisplay.getClass().getDeclaredFields())
                        .filter(field -> field.getType().getSimpleName().equals("IChatBaseComponent"))
                        .findFirst().orElseThrow(() -> new RuntimeException("Failed to find advancement display properties field"));
                titleComponentField.setAccessible(true);
                Object titleChatBaseComponent = titleComponentField.get(advancementDisplay);
                String title = (String) titleChatBaseComponent.getClass().getMethod("getText").invoke(titleChatBaseComponent);
                if (StringUtils.isNotBlank(title)) return title;
                Class<?> chatSerializerClass = Arrays.stream(titleChatBaseComponent.getClass().getDeclaredClasses())
                        .filter(clazz -> clazz.getSimpleName().equals("ChatSerializer"))
                        .findFirst().orElseThrow(() -> new RuntimeException("Couldn't get component ChatSerializer class"));
                String componentJson = (String) chatSerializerClass.getMethod("a", titleChatBaseComponent.getClass()).invoke(null, titleChatBaseComponent);
                return MessageUtil.toLegacy(GsonComponentSerializer.gson().deserialize(componentJson));
            } catch (Exception e) {
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Failed to get title of advancement " + advancement.getKey().getKey() + ": " + e.getMessage());

                String rawAdvancementName = advancement.getKey().getKey();
                return Arrays.stream(rawAdvancementName.substring(rawAdvancementName.lastIndexOf("/") + 1).toLowerCase().split("_"))
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                        .collect(Collectors.joining(" "));
            }
        });
    }

}
