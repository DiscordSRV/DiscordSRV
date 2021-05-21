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
import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WebhookUtil {

    static {
        try {
            // get rid of all previous webhooks created by DiscordSRV if they don't match a good channel
            for (Guild guild : DiscordSRV.getPlugin().getJda().getGuilds()) {
                if (!guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
                    DiscordSRV.debug("Unable to manage webhooks guild-wide in " + guild);
                    continue;
                }

                guild.retrieveWebhooks().queue(webhooks -> {
                    for (Webhook webhook : webhooks) {
                        if (webhook.getName().startsWith("DiscordSRV") && DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(webhook.getChannel()) == null) {
                            webhook.delete().reason("DiscordSRV-Created Webhook Purge").queue();
                        }
                    }
                });
            }
        } catch (Exception e) {
            DiscordSRV.warning("Failed to purge already existing webhooks: " + e.getMessage());
            if (DiscordSRV.config().getInt("DebugLevel") > 0) DiscordSRV.error(e);
        }
    }

    public static void deliverMessage(TextChannel channel, Player player, String message) {
        deliverMessage(channel, player, message, null);
    }

    public static void deliverMessage(TextChannel channel, Player player, String message, MessageEmbed embed) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            String avatarUrl = DiscordSRV.getAvatarUrl(player);
            String username = DiscordSRV.config().getString("Experiment_WebhookChatMessageUsernameFormat")
                    .replace("%displayname%", MessageUtil.strip(player.getDisplayName()))
                    .replace("%username%", player.getName());
            String chatMessage = DiscordSRV.config().getString("Experiment_WebhookChatMessageFormat")
                    .replace("%displayname%", MessageUtil.strip(player.getDisplayName()))
                    .replace("%username%", player.getName())
                    .replace("%message%", message);
            chatMessage = PlaceholderUtil.replacePlaceholders(chatMessage, player);
            username = PlaceholderUtil.replacePlaceholders(username, player);
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

        String webhookUrl = getWebhookUrlToUseForChannel(channel, webhookName);
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

                HttpRequest request = HttpRequest.post(webhookUrl)
                        .header("Content-Type", "application/json")
                        .userAgent("DiscordSRV/" + DiscordSRV.getPlugin().getDescription().getVersion())
                        .send(jsonObject.toString());

                int status = request.code();
                if (status == 404) {
                    // 404 = Invalid Webhook (most likely to have been deleted)
                    DiscordSRV.debug("Webhook delivery returned 404, marking webhooks URLs as invalid to let them regenerate" + (allowSecondAttempt ? " & trying again" : ""));
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
                            DiscordSRV.debug("Webhook delivery returned 10015 (Unknown Webhook), marking webhooks url's as invalid to let them regenerate" + (allowSecondAttempt ? " & trying again" : ""));
                            invalidWebhookUrlForChannel(channel); // tell it to get rid of the urls & get new ones
                            if (allowSecondAttempt) deliverMessage(channel, webhookName, webhookAvatarUrl, message, embed, false);
                            return;
                        }
                    }
                } catch (Throwable ignored) {}
                if (status == 204) {
                    DiscordSRV.debug("Received API response for webhook message delivery: " + status);
                } else {
                    DiscordSRV.debug("Received unexpected API response for webhook message delivery: " + status + " for request: " + jsonObject.toString() + ", response: " + body);
                }
            } catch (Exception e) {
                DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
                DiscordSRV.debug(e);
            }
        });
    }

    private static final Map<String, List<String>> channelWebhookUrls = new ConcurrentHashMap<>();
    private static final Map<String, String> lastUsedWebhookUrls = new ConcurrentHashMap<>();
    private static final Map<String, String> lastWebhookUsers = new ConcurrentHashMap<>();
    private static final int webhookPoolSize = 2;

    public static void invalidWebhookUrlForChannel(TextChannel textChannel) {
        String channelId = textChannel.getId();
        channelWebhookUrls.remove(channelId);
        lastUsedWebhookUrls.remove(channelId);
        lastWebhookUsers.remove(channelId);
    }

    public static String getWebhookUrlToUseForChannel(TextChannel channel, String username) {
        final String channelId = channel.getId();
        final List<String> webhookUrls = channelWebhookUrls.computeIfAbsent(channelId, cid -> {
            final List<Webhook> hooks = new ArrayList<>();
            final Guild guild = channel.getGuild();

            // Check if we have permission guild-wide
            if (guild.getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
                guild.retrieveWebhooks().complete().stream()
                        .filter(webhook -> webhook.getName().startsWith("DiscordSRV " + cid + " #"))
                        .filter(webhook -> {
                            if (!webhook.getChannel().equals(channel)) {
                                webhook.delete().reason("Purging lost webhook").queue();
                                return false;
                            } else {
                                return true;
                            }
                        }).forEach(hooks::add);
            } else {
                channel.retrieveWebhooks().complete().stream()
                        .filter(webhook -> webhook.getName().startsWith("DiscordSRV " + cid + " #"))
                        .forEach(hooks::add);
            }

            if (hooks.size() != webhookPoolSize) {
                for (Webhook hook : hooks) {
                    hook.delete("Purging orphaned webhook").queue(null, error -> {
                        if (error instanceof InsufficientPermissionException) {
                            Permission permission = ((InsufficientPermissionException) error).getPermission();
                            DiscordSRV.error("Can't delete orphaned webhook \"" + hook.getName() + "\" because bot is missing permission \"" + permission.getName() + "\"");
                        } else {
                            DiscordSRV.error("Can't delete orphaned webhook \"" + hook.getName() + "\": " + error.getMessage());
                        }
                    });
                }
                hooks.clear();

                // create webhooks to use
                for (int i = 1; i <= webhookPoolSize; i++) {
                    final Webhook webhook = createWebhook(channel, "DiscordSRV " + cid + " #" + i);
                    if (webhook == null) return null;
                    hooks.add(webhook);
                }
            }

            return hooks.stream().map(Webhook::getUrl).collect(Collectors.toList());
        });
        if (webhookUrls == null) return null;

        String lastUser = lastWebhookUsers.getOrDefault(channelId, null);
        if (lastUser != null && lastUser.equals(username)) {
            String lastWebhookUrl = lastUsedWebhookUrls.getOrDefault(channelId, null);
            if (lastWebhookUrl != null) {
                return lastWebhookUrl;
            }
        }

        final String webhookUrl = lastUsedWebhookUrls.compute(channelId, (cid, lastUsedWebhookId) -> {
            int index = webhookUrls.indexOf(lastUsedWebhookId);
            index = (index + 1) % webhookPoolSize;
            return webhookUrls.get(index);
        });

        lastWebhookUsers.put(channelId, username);
        return webhookUrl;
    }

    public static Webhook createWebhook(TextChannel channel, String name) {
        try {
            Webhook webhook = channel.createWebhook(name).complete();
            DiscordSRV.debug("Created webhook " + webhook.getName() + " to deliver messages to text channel #" + channel.getName());
            return webhook;
        } catch (Exception e) {
            DiscordSRV.error("Failed to create webhook " + name + " for message delivery: " + e.getMessage());
            return null;
        }
    }

}
