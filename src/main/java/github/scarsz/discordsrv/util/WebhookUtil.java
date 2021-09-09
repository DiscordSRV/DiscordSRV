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

package github.scarsz.discordsrv.util;

import com.github.kevinsawicki.http.HttpRequest;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WebhookUtil {

    private static final Predicate<Webhook> LEGACY = hook -> hook.getName().endsWith("#1") || hook.getName().endsWith("#2");

    static {
        try {
            // get rid of all previous webhooks created by DiscordSRV if they don't match a good channel
            for (Guild guild : DiscordSRV.getPlugin().getJda().getGuilds()) {
                Member selfMember = guild.getSelfMember();
                if (!selfMember.hasPermission(Permission.MANAGE_WEBHOOKS)) {
                    DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Unable to manage webhooks guild-wide in " + guild);
                    continue;
                }

                guild.retrieveWebhooks().queue(webhooks -> {
                    for (Webhook webhook : webhooks) {
                        Member owner = webhook.getOwner();
                        if (owner == null || !owner.getId().equals(selfMember.getId()) || !webhook.getName().startsWith("DiscordSRV")) {
                            continue;
                        }

                        if (DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(webhook.getChannel()) == null) {
                            webhook.delete().reason("DiscordSRV: Purging webhook for unlinked channel").queue();
                        } else if (LEGACY.test(webhook)) {
                            webhook.delete().reason("DiscordSRV: Purging legacy formatted webhook").queue();
                        }
                    }
                });
            }
        } catch (Exception e) {
            DiscordSRV.warning("Failed to purge already existing webhooks: " + e.getMessage());
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, e);
        }
    }

    public static void deliverMessage(TextChannel channel, Player player, String message) {
        deliverMessage(channel, player, message, null);
    }

    @SuppressWarnings("deprecation")
    public static void deliverMessage(TextChannel channel, Player player, String message, MessageEmbed embed) {
        deliverMessage(channel, player, player.getDisplayName(), message, embed);
    }

    public static void deliverMessage(TextChannel channel, OfflinePlayer player, String displayName, String message, MessageEmbed embed) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            String avatarUrl;
            if (player instanceof Player) {
                avatarUrl = DiscordSRV.getAvatarUrl((Player) player);
            } else {
                avatarUrl = DiscordSRV.getAvatarUrl(player.getName(), player.getUniqueId());
            }

            String username = DiscordSRV.config().getString("Experiment_WebhookChatMessageUsernameFormat")
                    .replace("%displayname%", displayName)
                    .replace("%username%", String.valueOf(player.getName()));
            String chatMessage = DiscordSRV.config().getString("Experiment_WebhookChatMessageFormat")
                    .replace("%displayname%", displayName)
                    .replace("%username%", player.getName())
                    .replace("%message%", message.replace("[", "\\["));
            chatMessage = PlaceholderUtil.replacePlaceholdersToDiscord(chatMessage, player);
            username = PlaceholderUtil.replacePlaceholdersToDiscord(username, player);
            username = MessageUtil.strip(username);

            String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
            if (userId != null) {
                Member member = DiscordUtil.getMemberById(userId);
                if (member != null) {
                    if (DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageAvatarFromDiscord"))
                        avatarUrl = member.getUser().getEffectiveAvatarUrl();
                    if (DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageUsernameFromDiscord"))
                        username = member.getEffectiveName();
                }
            }

            deliverMessage(channel, username, avatarUrl, chatMessage, embed);
        });
    }

    public static void deliverMessage(TextChannel channel, String webhookName, String webhookAvatarUrl, String message, MessageEmbed embed) {
        deliverMessage(channel, webhookName, webhookAvatarUrl, message, embed, true);
    }

    private static void deliverMessage(TextChannel channel, String webhookName, String webhookAvatarUrl, String message, MessageEmbed embed, boolean allowSecondAttempt) {
        if (channel == null) return;

        String webhookUrl = getWebhookUrlToUseForChannel(channel);
        if (webhookUrl == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            try {
                JSONObject jsonObject = new JSONObject();
                // workaround for a Discord block for using 'Clyde' in usernames
                jsonObject.put("username", webhookName.replaceAll("(?:(?i)c)l(?:(?i)yde)", "$1I$2").replaceAll("(?i)(clyd)e", "$13"));
                jsonObject.put("avatar_url", webhookAvatarUrl);

                if (StringUtils.isNotBlank(message)) jsonObject.put("content", message);
                if (embed != null) {
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(embed.toData().toMap());
                    jsonObject.put("embeds", jsonArray);
                }

                JSONObject allowedMentions = new JSONObject();
                Set<String> parse = MessageAction.getDefaultMentions().stream()
                        .filter(Objects::nonNull)
                        .map(Message.MentionType::getParseKey)
                        .collect(Collectors.toSet());
                allowedMentions.put("parse", parse);
                jsonObject.put("allowed_mentions", allowedMentions);

                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Sending webhook payload: " + jsonObject);

                HttpRequest request = HttpRequest.post(webhookUrl)
                        .header("Content-Type", "application/json")
                        .userAgent("DiscordSRV/" + DiscordSRV.getPlugin().getDescription().getVersion())
                        .send(jsonObject.toString());

                int status = request.code();
                if (status == 404) {
                    // 404 = Invalid Webhook (most likely to have been deleted)
                    DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Webhook delivery returned 404, marking webhooks URLs as invalid to let them regenerate" + (allowSecondAttempt ? " & trying again" : ""));
                    invalidWebhookUrlForChannel(channel); // tell it to get rid of the urls & get new ones
                    if (allowSecondAttempt) deliverMessage(channel, webhookName, webhookAvatarUrl, message, embed, false);
                    return;
                }
                String body = request.body();
                try {
                    JSONObject jsonObj = new JSONObject(body);
                    if (jsonObj.has("code")) {
                        // 10015 = unknown webhook, https://discord.com/developers/docs/topics/opcodes-and-status-codes#json-json-error-codes
                        if (jsonObj.getInt("code") == 10015) {
                            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Webhook delivery returned 10015 (Unknown Webhook), marking webhooks url's as invalid to let them regenerate" + (allowSecondAttempt ? " & trying again" : ""));
                            invalidWebhookUrlForChannel(channel); // tell it to get rid of the urls & get new ones
                            if (allowSecondAttempt) deliverMessage(channel, webhookName, webhookAvatarUrl, message, embed, false);
                            return;
                        }
                    }
                } catch (Throwable ignored) {}
                if (status == 204) {
                    DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received API response for webhook message delivery: " + status);
                } else {
                    DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received unexpected API response for webhook message delivery: " + status + " for request: " + jsonObject.toString() + ", response: " + body);
                }
            } catch (Exception e) {
                DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
                DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, e);
            }
        });
    }

    private static final Map<String, String> channelWebhookUrls = new ConcurrentHashMap<>();

    public static void invalidWebhookUrlForChannel(TextChannel textChannel) {
        String channelId = textChannel.getId();
        channelWebhookUrls.remove(channelId);
    }

    public static String getWebhookUrlToUseForChannel(TextChannel channel) {
        final String channelId = channel.getId();
        return channelWebhookUrls.computeIfAbsent(channelId, cid -> {
            List<Webhook> hooks = new ArrayList<>();
            final Guild guild = channel.getGuild();
            final Member selfMember = guild.getSelfMember();

            String webhookFormat = "DiscordSRV " + cid;

            // Check if we have permission guild-wide
            List<Webhook> result;
            if (guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
                result = guild.retrieveWebhooks().complete();
            } else {
                result = channel.retrieveWebhooks().complete();
            }

            result.stream()
                    .filter(webhook -> webhook.getName().startsWith(webhookFormat))
                    .filter(webhook -> {
                        // Filter to what we can modify
                        Member owner = webhook.getOwner();
                        return owner != null && selfMember.getId().equals(owner.getId());
                    })
                    .filter(webhook -> {
                        if (!webhook.getChannel().equals(channel)) {
                            webhook.delete().reason("DiscordSRV: Purging lost webhook").queue();
                            return false;
                        }
                        return true;
                    })
                    .filter(webhook -> {
                        if (LEGACY.test(webhook)) {
                            webhook.delete().reason("DiscordSRV: Purging legacy formatted webhook").queue();
                            return false;
                        }
                        return true;
                    })
                    .forEach(hooks::add);

            if (hooks.isEmpty()) {
                hooks.add(createWebhook(channel, webhookFormat));
            } else if (hooks.size() > 1) {
                for (int index = 1; index < hooks.size(); index++) {
                    hooks.get(index).delete().reason("DiscordSRV: Purging duplicate webhook").queue();
                }
            }

            return hooks.stream().map(Webhook::getUrl).findAny().orElse(null);
        });
    }

    public static Webhook createWebhook(TextChannel channel, String name) {
        try {
            Webhook webhook = channel.createWebhook(name).reason("DiscordSRV: Creating webhook").complete();
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Created webhook " + webhook.getName() + " to deliver messages to text channel #" + channel.getName());
            return webhook;
        } catch (Exception e) {
            DiscordSRV.error("Failed to create webhook " + name + " for message delivery: " + e.getMessage());
            return null;
        }
    }

}
