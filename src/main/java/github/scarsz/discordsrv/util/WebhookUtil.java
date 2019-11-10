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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (channel == null) return;

        Webhook targetWebhook = getWebhookToUseForChannel(channel, player.getUniqueId().toString());
        if (targetWebhook == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            try {
                String avatarUrl = DiscordSRV.config().getString("Experiment_WebhookChatMessageAvatarUrl");
                if (StringUtils.isBlank(avatarUrl)) avatarUrl = "https://crafatar.com/avatars/{uuid}?overlay";
                avatarUrl = avatarUrl
                        .replace("{username}", player.getName())
                        .replace("{uuid}", player.getUniqueId().toString());

                HttpResponse<String> response = Unirest.post(targetWebhook.getUrl())
                        .field("content", message)
                        .field("username", DiscordUtil.strip(player.getDisplayName()))
                        .field("avatar_url", avatarUrl)
                        .asString();
                DiscordSRV.debug("Received API response for webhook message delivery: " + response.getStatus());
            } catch (Exception e) {
                DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
                DiscordSRV.debug(ExceptionUtils.getMessage(e));
                e.printStackTrace();
            }
        });
    }

    static class LastWebhookInfo {

        final String webhook;
        final String targetName;

        public LastWebhookInfo(String webhook, String targetName) {
            this.webhook = webhook;
            this.targetName = targetName;
        }

    }

    public static final Map<TextChannel, LastWebhookInfo> lastUsedWebhooks = new HashMap<>();
    public static Webhook getWebhookToUseForChannel(TextChannel channel, String targetName) {
        synchronized (lastUsedWebhooks) {
            List<Webhook> webhooks = new ArrayList<>();
            channel.getGuild().retrieveWebhooks().complete().stream()
                    .filter(webhook -> webhook.getName().startsWith("DiscordSRV " + channel.getId() + " #"))
                    .forEach(webhooks::add);

            if (webhooks.size() != 2) {
                webhooks.forEach(webhook -> webhook.delete().reason("Purging orphaned webhook").queue());
                webhooks.clear();

                if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
                    DiscordSRV.error("Can't create a webhook to deliver chat message, bot is missing permission \"Manage Webhooks\"");
                    return null;
                }

                // create webhooks to use
                Webhook webhook1 = createWebhook(channel.getGuild(), channel, "DiscordSRV " + channel.getId() + " #1");
                Webhook webhook2 = createWebhook(channel.getGuild(), channel, "DiscordSRV " + channel.getId() + " #2");

                if (webhook1 == null || webhook2 == null) return null;

                webhooks.add(webhook1);
                webhooks.add(webhook2);
            }

            LastWebhookInfo info = lastUsedWebhooks.getOrDefault(channel, null);
            Webhook target;

            if (info == null) {
                target = webhooks.get(0);
                lastUsedWebhooks.put(channel, new LastWebhookInfo(target.getId(), targetName));
                return target;
            }

            target = info.targetName.equals(targetName)
                    ? webhooks.get(0).getId().equals(info.webhook)
                        ? webhooks.get(0)
                        : webhooks.get(1)
                    : webhooks.get(0).getId().equals(info.webhook)
                        ? webhooks.get(0)
                        : webhooks.get(1)
            ;

            lastUsedWebhooks.put(channel, new LastWebhookInfo(target.getId(), targetName));

            return target;
        }
    }

    public static Webhook createWebhook(Guild guild, TextChannel channel, String name) {
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
