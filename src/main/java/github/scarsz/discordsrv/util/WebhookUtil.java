package github.scarsz.discordsrv.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
                for (Webhook webhook : guild.getWebhooks().complete()) {
                    if (webhook.getName().startsWith("DiscordSRV") && DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(webhook.getChannel()) == null) {
                        webhook.delete().reason("Purge").queue();
                    }
                }
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

        try {
            HttpResponse<String> response = Unirest.post(targetWebhook.getUrl())
                    .field("content", message)
                    .field("username", DiscordUtil.strip(player.getDisplayName()))
                    .field("avatar_url", "https://minotar.net/helm/" + player.getName() + "/100.png")
                    .asString();
            DiscordSRV.debug("Received API response for webhook message delivery: " + response.getStatus());
        } catch (Exception e) {
            DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
            DiscordSRV.debug(ExceptionUtils.getMessage(e));
            e.printStackTrace();
        }
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
            channel.getGuild().getWebhooks().complete().stream()
                    .filter(webhook -> webhook.getName().startsWith("DiscordSRV #" + channel.getName() + " #"))
                    .forEach(webhooks::add);

            if (webhooks.size() != 2) {
                webhooks.forEach(webhook -> webhook.delete().reason("Purging orphaned webhook").queue());
                webhooks.clear();

                if (!channel.getGuild().getMember(channel.getJDA().getSelfUser()).hasPermission(Permission.MANAGE_WEBHOOKS)) {
                    DiscordSRV.error("Can't create a webhook to deliver chat message, bot is missing permission \"Manage Webhooks\"");
                    return null;
                }

                // create webhooks to use
                Webhook webhook1 = createWebhook(channel.getGuild(), channel, "DiscordSRV #" + channel.getName() + " #1");
                Webhook webhook2 = createWebhook(channel.getGuild(), channel, "DiscordSRV #" + channel.getName() + " #2");

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
            Webhook createdWebhook = guild.getController().createWebhook(channel, name).complete();
            DiscordSRV.debug("Created webhook " + createdWebhook.getName() + " to deliver messages to text channel #" + channel.getName());
            return createdWebhook;
        } catch (Exception e) {
            DiscordSRV.error("Failed to create webhook " + name + " for message delivery: " + e.getMessage());
            return null;
        }
    }

}
