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

package github.scarsz.discordsrv.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            if (DiscordSRV.config().getInt("DebugLevel") > 0) {
                e.printStackTrace();
            }
        }
    }

    public static void deliverMessage(TextChannel channel, Player player, String message) {
        deliverMessage(channel, player, message, null);
    }

    public static void deliverMessage(TextChannel channel, Player player, String message, MessageEmbed embed) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            String avatarUrl = DiscordSRV.config().getString("Experiment_EmbedAvatarUrl");
            avatarUrl = PlaceholderUtil.replacePlaceholders(avatarUrl, player);

            String username = DiscordSRV.config().getString("Experiment_WebhookChatMessageUsernameFormat")
                    .replace("%displayname%", DiscordUtil.strip(player.getDisplayName()))
                    .replace("%username%", player.getName());
            username = PlaceholderUtil.replacePlaceholders(username, player);
            username = DiscordUtil.strip(username);

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

            if (StringUtils.isBlank(avatarUrl)) avatarUrl = "https://minotar.net/helm/{uuid-nodashes}/{size}";
            avatarUrl = avatarUrl
                    .replace("{timestamp}", String.valueOf(System.currentTimeMillis() / 1000))
                    .replace("{username}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString())
                    .replace("{uuid-nodashes}", player.getUniqueId().toString().replace("-", ""))
                    .replace("{size}", "128");

            deliverMessage(channel, username, avatarUrl, message, embed);
        });
    }

    public static void deliverMessage(TextChannel channel, String webhookName, String webhookAvatarUrl, String message, MessageEmbed embed) {
        deliverMessage(channel, webhookName, webhookAvatarUrl, message, embed, true);
    }

    public static void deliverMessage(TextChannel channel, String webhookName, String webhookAvatarUrl, String message, MessageEmbed embed, boolean allowSecondAttempt) {
        if (channel == null) return;

        String webhookUrl = getWebhookUrlToUseForChannel(channel, webhookName);
        if (webhookUrl == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", webhookName);
                jsonObject.put("avatar_url", webhookAvatarUrl);

                if (StringUtils.isNotBlank(message)) jsonObject.put("content", message);
                if (embed != null) {
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(embed.toData().toMap());
                    jsonObject.put("embeds", jsonArray);
                }

                HttpResponse<String> response = Unirest.post(webhookUrl)
                        .header("Content-Type", "application/json")
                        .body(jsonObject)
                        .asString();
                int status = response.getStatus();
                if (status == 404) {
                    // 404 = Invalid Webhook (most likely to have been deleted)
                    DiscordSRV.debug("Webhook delivery returned 404, marking webhooks url's as invalid to let them regenerate" + (allowSecondAttempt ? " & trying again" : ""));
                    invalidWebhookUrlForChannel(channel); // tell it to get rid of the urls & get new ones
                    if (allowSecondAttempt) deliverMessage(channel, webhookName, webhookAvatarUrl, message, embed, false);
                    return;
                }
                DiscordSRV.debug("Received API response for webhook message delivery: " + response.getStatus());
            } catch (Exception e) {
                DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
                DiscordSRV.debug(ExceptionUtils.getMessage(e));
                e.printStackTrace();
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
                if (!guild.getSelfMember().hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
                    DiscordSRV.error("Can't manage webhook(s) to deliver chat message, bot is missing permission \"Manage Webhooks\"");
                    return null;
                }

                hooks.forEach(webhook -> webhook.delete().reason("Purging orphaned webhook").queue());
                hooks.clear();

                // create webhooks to use
                for (int i = 1; i <= webhookPoolSize; i++) {
                    final Webhook webhook = createWebhook(channel, "DiscordSRV " + cid + " #" + i);
                    if (webhook == null) return null;
                    hooks.add(webhook);
                }
            }

            return hooks
                    .stream()
                    .map(Webhook::getUrl)
                    .collect(Collectors.toList());
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
