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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WebhookUtil {

    static {
        try {
            // get rid of all previous webhooks created by DiscordSRV if they don't match a good channel
            for (Guild guild : DiscordSRV.getPlugin().getJda().getGuilds()) {
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
        deliverMessage(channel, player.getUniqueId(), player.getName(), player.getDisplayName(), message, embed);
    }

    public static void deliverMessage(TextChannel channel, UUID uuid, String name, String displayName, String message, MessageEmbed embed) {
        if (channel == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            try {
                Webhook targetWebhook = getWebhookToUseForChannel(channel);
                if (targetWebhook == null) return;

                String avatarUrl = DiscordSRV.config().getString("Experiment_EmbedAvatarUrl");
                String username = DiscordUtil.strip(displayName);

                String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);
                if (userId != null) {
                    Member member = DiscordUtil.getMemberById(userId);
                    if (member != null) {
                        if (DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageAvatarFromDiscord"))
                            avatarUrl = member.getUser().getEffectiveAvatarUrl();
                        if (DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageUsernameFromDiscord"))
                            username = member.getEffectiveName();
                    }
                }

                if (StringUtils.isBlank(avatarUrl)) avatarUrl = "https://crafatar.com/avatars/{uuid}?overlay&size={size}";
                avatarUrl = avatarUrl
                        .replace("{username}", name)
                        .replace("{uuid}", uuid.toString())
                        .replace("{size}", "128");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", username);
                jsonObject.put("avatar_url", avatarUrl);

                if (StringUtils.isNotBlank(message)) jsonObject.put("content", message);
                if (embed != null) {
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(embed.toData().toMap());
                    jsonObject.put("embeds", jsonArray);
                }

                HttpResponse<String> response = Unirest.post(targetWebhook.getUrl())
                        .header("Content-Type", "application/json")
                        .body(jsonObject)
                        .asString();
                DiscordSRV.debug("Received API response for webhook message delivery: " + response.getStatus());
            } catch (Exception e) {
                DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
                DiscordSRV.debug(ExceptionUtils.getMessage(e));
                e.printStackTrace();
            }
        });
    }

    private static final Map<String, List<String>> channelWebhookIds = new ConcurrentHashMap<>();
    private static final Map<String, String> lastUsedWebhookIds = new ConcurrentHashMap<>();
    private static final int webhookPoolSize = 2;

    public static Webhook getWebhookToUseForChannel(TextChannel channel) {
        final String channelId = channel.getId();
        final List<String> webhookIds = channelWebhookIds.computeIfAbsent(channelId, cid -> {
            final List<Webhook> hooks = new ArrayList<>();
            final Guild guild = channel.getGuild();

            guild.retrieveWebhooks().complete().stream()
                .filter(webhook -> webhook.getName().startsWith("DiscordSRV " + cid + " #"))
                .forEach(hooks::add);

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
                    .map(ISnowflake::getId)
                    .collect(Collectors.toList());
        });
        if (webhookIds == null) return null;

        final String webhookId = lastUsedWebhookIds.compute(channelId, (cid, lastUsedWebhookId) -> {
            int index = webhookIds.indexOf(lastUsedWebhookId);
            index = (index + 1) % webhookPoolSize;
            return webhookIds.get(index);
        });

        return DiscordUtil.getJda().retrieveWebhookById(webhookId).complete();
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
