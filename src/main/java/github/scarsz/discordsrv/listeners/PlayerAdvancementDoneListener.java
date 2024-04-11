/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.AchievementMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.AchievementMessagePreProcessEvent;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import lombok.SneakyThrows;
import java.lang.reflect.Field;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.Optional;

public class PlayerAdvancementDoneListener implements Listener {

    private static final boolean GAMERULE_CLASS_AVAILABLE;
    private static final Object GAMERULE;

    static {
        String gamerule = "announceAdvancements";
        Object gameruleValue = null;
        try {
            Class<?> gameRuleClass = Class.forName("org.bukkit.GameRule");
            gameruleValue = gameRuleClass.getMethod("getByName", String.class).invoke(null, gamerule);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {}

        GAMERULE_CLASS_AVAILABLE = gameruleValue != null;
        GAMERULE = GAMERULE_CLASS_AVAILABLE ? gameruleValue : gamerule;
    }

    public PlayerAdvancementDoneListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        // return if advancement or player objects are knackered because this can apparently happen for some reason
        if (event.getAdvancement() == null || player == null) return;

        // respect invisibility plugins
        if (PlayerUtil.isVanished(player)) return;

        SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () -> runAsync(event));
    }

    private void runAsync(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        if (advancementIsHiddenInChat(advancement, player.getWorld())) return;

        String channelName = DiscordSRV.getPlugin().getOptionalChannel("awards");

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

    private static Method ADVANCEMENT_GET_DISPLAY_METHOD = null;
    private static Method ADVANCEMENT_DISPLAY_ANNOUNCE_CHAT_METHOD = null;
    @SneakyThrows
    @SuppressWarnings("removal")
    private boolean advancementIsHiddenInChat(Advancement advancement, World world) {

        // don't send messages for advancements related to recipes
        String key = advancement.getKey().getKey();
        if (key.contains("recipe/") || key.contains("recipes/")) return true;

        // ensure advancements should be announced in the world
        Boolean isGamerule = GAMERULE_CLASS_AVAILABLE // This class was added in 1.13
                ? world.getGameRuleValue((GameRule<Boolean>) GAMERULE) // 1.13+
                : Boolean.parseBoolean(world.getGameRuleValue((String) GAMERULE)); // <= 1.12
        if (Boolean.FALSE.equals(isGamerule)) return true;

        // paper advancement API has its own AdvancementDisplay type from Advancement#getDisplay
        Object advancementDisplay = null;
        if (ADVANCEMENT_DISPLAY_ANNOUNCE_CHAT_METHOD == null) {
            try {
                if (ADVANCEMENT_GET_DISPLAY_METHOD == null)
                    ADVANCEMENT_GET_DISPLAY_METHOD = Arrays.stream(advancement.getClass().getMethods())
                            .filter(method -> method.getName().equals("getDisplay"))
                            .findFirst().orElse(null);
                advancementDisplay = ADVANCEMENT_GET_DISPLAY_METHOD.invoke(advancement);
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Successfully invoked bukkit AdvancementDisplay method");
            } catch (Exception e) {
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Failed to find PlayerAdvancementDoneEvent#getDisplay method");
            }
        }

        // dive into nms if paper and spigot don't have an AdvancementDisplay class
        if (ADVANCEMENT_DISPLAY_ANNOUNCE_CHAT_METHOD != null || advancementDisplay == null) {
            try {
                Object craftAdvancement = NMSUtil.getHandle(advancement);
                Optional<Object> craftAdvancementDisplayOptional = getAdvancementDisplayObject(craftAdvancement);

                Object craftAdvancementDisplay = craftAdvancementDisplayOptional.get();
                if (ADVANCEMENT_DISPLAY_ANNOUNCE_CHAT_METHOD == null) {
                    ADVANCEMENT_DISPLAY_ANNOUNCE_CHAT_METHOD = Arrays.stream(craftAdvancementDisplay.getClass().getMethods())
                            .filter(method -> method.getReturnType().equals(boolean.class))
                            .filter(method -> method.getName().equals("i"))
                            .findFirst().orElse(null);
                }
                boolean doesAnnounceToChat = (boolean) ADVANCEMENT_DISPLAY_ANNOUNCE_CHAT_METHOD.invoke(craftAdvancementDisplay);
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Successfully invoked NMS announce in chat");
                return !doesAnnounceToChat;
            } catch (Exception e) {
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Failed to get NMS announceChat value: " + e);
            }
        }

        // in some versions between 1.17.x and 1.18.x, bukkit api doesn't include AdvancementDisplay but paper api does
        if (!advancementDisplay.getClass().getSimpleName().equals("PaperAdvancementDisplay") && advancementDisplay instanceof org.bukkit.advancement.AdvancementDisplay) {
            return !((org.bukkit.advancement.AdvancementDisplay) advancementDisplay).shouldAnnounceChat();
        } else if (advancementDisplay instanceof io.papermc.paper.advancement.AdvancementDisplay) {
            return !((io.papermc.paper.advancement.AdvancementDisplay) advancementDisplay).doesAnnounceToChat();
        } else {
            try {
                Object craftAdvancement = NMSUtil.getHandle(advancement);
                assert craftAdvancement != null;
                Optional<Object> craftAdvancementDisplayOptional = getAdvancementDisplayObject(craftAdvancement);
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Successfully invoked nms AdvancementDisplay method");
                return !craftAdvancementDisplayOptional.isPresent();
            } catch (Exception e) {
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Failed to check if advancement should be displayed: " + e);
            }
        }
        return false;
    }

    private static final Map<NamespacedKey, String> ADVANCEMENT_TITLE_CACHE = new ConcurrentHashMap<>();
    public static String getTitle(Advancement advancement) {
        return ADVANCEMENT_TITLE_CACHE.computeIfAbsent(advancement.getKey(), v -> {
            try {
                Object handle = NMSUtil.getHandle(advancement);
                assert handle != null;
                Optional<Object> advancementDisplayOptional = getAdvancementDisplayObject(handle);
                if (!advancementDisplayOptional.isPresent()) throw new RuntimeException("Advancement doesn't have display properties");

                Object advancementDisplay = advancementDisplayOptional.get();
                try {
                    Field advancementMessageField = advancementDisplay.getClass().getDeclaredField("a");
                    advancementMessageField.setAccessible(true);
                    Object advancementMessage = advancementMessageField.get(advancementDisplay);
                    Object advancementTitle = advancementMessage.getClass().getMethod("getString").invoke(advancementMessage);
                    DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Successfully retrieved advancement title from getString");
                    return (String) advancementTitle;
                } catch (Exception e) {
                    DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Failed to get title of advancement using getString, trying JSON method");
                }

                Field titleComponentField = Arrays.stream(advancementDisplay.getClass().getDeclaredFields())
                        .filter(field -> field.getType().getSimpleName().equals("IChatBaseComponent") || field.getType().getSimpleName().equals("IChatMutableComponent"))
                        .findFirst().orElseThrow(() -> new RuntimeException("Failed to find advancement display properties field"));
                titleComponentField.setAccessible(true);
                Object titleChatBaseComponent = titleComponentField.get(advancementDisplay);
                Method method_getText = null;
                try {
                    method_getText = titleChatBaseComponent.getClass().getMethod("getText");
                } catch (Exception ignored) {}
                if (method_getText == null) {
                    try {
                        method_getText = titleChatBaseComponent.getClass().getMethod("getString");
                    } catch (Exception ignored) {}
                }

                if (method_getText != null) {
                    String title = (String) method_getText.invoke(titleChatBaseComponent);
                    DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Successfully retrieved advancement title from component");
                    if (StringUtils.isNotBlank(title)) return title;
                }

                Class<?> chatSerializerClass = Arrays.stream(titleChatBaseComponent.getClass().getDeclaredClasses())
                        .filter(clazz -> clazz.getSimpleName().equals("ChatSerializer"))
                        .findFirst().orElseThrow(() -> new RuntimeException("Couldn't get component ChatSerializer class"));
                String componentJson = (String) chatSerializerClass.getMethod("a", titleChatBaseComponent.getClass()).invoke(null, titleChatBaseComponent);
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Successfully retrieved advancement title from json");
                return MessageUtil.toLegacy(GsonComponentSerializer.gson().deserialize(componentJson));
            } catch (Exception e) {
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, e, "Failed to get title of advancement " + advancement.getKey().getKey() + ": " + e.getMessage());

                String rawAdvancementName = advancement.getKey().getKey();
                return Arrays.stream(rawAdvancementName.substring(rawAdvancementName.lastIndexOf("/") + 1).toLowerCase().split("_"))
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                        .collect(Collectors.joining(" "));
            }
        });
    }

    private static Method method_getAdvancementFromHolder = null;
    private static Method method_getAdvancementDisplay = null;
    private static Optional<Object> getAdvancementDisplayObject(Object handle) throws IllegalAccessException, InvocationTargetException {
        if (handle.getClass().getSimpleName().equals("AdvancementHolder")) {
            if (method_getAdvancementFromHolder == null) {
                method_getAdvancementFromHolder = Arrays.stream(handle.getClass().getMethods())
                        .filter(method -> method.getReturnType().getName().equals("net.minecraft.advancements.Advancement"))
                        .filter(method -> method.getParameterCount() == 0)
                        .findFirst().orElseThrow(() -> new RuntimeException("Failed to find Advancement from AdvancementHolder"));
            }

            if (method_getAdvancementFromHolder != null) {
                Object holder = method_getAdvancementFromHolder.invoke(handle);

                if (method_getAdvancementDisplay == null) {
                    method_getAdvancementDisplay = Arrays.stream(holder.getClass().getMethods())
                            .filter(method -> method.getReturnType().getSimpleName().equals("Optional"))
                            .filter(method -> method.getGenericReturnType().getTypeName().contains("AdvancementDisplay"))
                            .findFirst().orElseThrow(() -> new RuntimeException("Failed to find AdvancementDisplay getter for advancement handle"));
                }

                @SuppressWarnings("unchecked")
                Optional<Object> optionalAdvancementDisplay = (Optional<Object>) method_getAdvancementDisplay.invoke(holder);
                return optionalAdvancementDisplay;
            }
        } else {
            if (method_getAdvancementDisplay == null) {
                method_getAdvancementDisplay = Arrays.stream(handle.getClass().getMethods())
                        .filter(method -> method.getReturnType().getSimpleName().equals("AdvancementDisplay"))
                        .filter(method -> method.getParameterCount() == 0)
                        .findFirst().orElseThrow(() -> new RuntimeException("Failed to find AdvancementDisplay getter for advancement handle"));
            }

            return Optional.of(method_getAdvancementDisplay.invoke(handle));
        }
        return Optional.empty();
    }

}
