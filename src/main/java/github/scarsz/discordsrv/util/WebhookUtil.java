package github.scarsz.discordsrv.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
            if (DiscordSRV.getPlugin().getConfig().getInt("DebugLevel") > 0) {
                e.printStackTrace();
            }
        }
    }

    public static void deliverMessage(TextChannel channel, Player player, String message) {
        if (channel == null) return;

        Webhook targetWebhook = null;
        for (Webhook webhook : channel.getGuild().getWebhooks().complete()) {
            if (webhook.getName().equals("DiscordSRV #" + channel.getName())) {
                targetWebhook = webhook;
            }
        }

        if (targetWebhook == null) {
            // no suitable webhook existed

            if (!channel.getGuild().getMember(channel.getJDA().getSelfUser()).hasPermission(Permission.MANAGE_WEBHOOKS)) {
                DiscordSRV.error("Can't create a webhook to deliver chat message, bot is missing permission \"Manage Webhooks\"");
                return;
            }

            // create a webhook to use
            targetWebhook = channel.getGuild().getController().createWebhook(channel, "DiscordSRV #" + channel.getName()).complete();
            DiscordSRV.debug("Created webhook " + targetWebhook + " to deliver messages to text channel #" + channel.getName());
        }

        try {
            HttpResponse<String> response = Unirest.post(targetWebhook.getUrl())
                    .field("content", message)
                    .field("username", ChatColor.stripColor(player.getDisplayName()))
                    .field("avatar_url", "https://minotar.net/helm/" + player.getName() + "/100.png")
                    .asString();
            DiscordSRV.debug("Received API response for webhook message delivery: " + response.getStatus());
        } catch (Exception e) {
            DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
            DiscordSRV.debug(ExceptionUtils.getMessage(e));
            e.printStackTrace();
        }
    }

}
