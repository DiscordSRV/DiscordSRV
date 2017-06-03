package github.scarsz.discordsrv.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.entity.Player;

public class WebhookUtil {

    static {
        // get rid of all previous webhooks created by DiscordSRV if they don't match a good channel
        for (Guild guild : DiscordSRV.getPlugin().getJda().getGuilds()) {
            for (Webhook webhook : guild.getWebhooks().complete()) {
                if (webhook.getName().startsWith("DiscordSRV") && DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(webhook.getChannel()) == null) {
                    webhook.delete().reason("Purge").queue();
                }
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
            // no suitable webhook existed, create it
            targetWebhook = channel.getGuild().getController().createWebhook(channel, "DiscordSRV #" + channel.getName()).complete();
            DiscordSRV.debug("Created webhook " + targetWebhook + " to deliver messages to text channel #" + channel.getName());
        }

        try {
            HttpResponse<JsonNode> response = Unirest.post(targetWebhook.getUrl())
                    .field("content", message)
                    .field("username", player.getDisplayName())
                    .field("avatar_url", "https://minotar.net/helm/" + player.getName() + "/100.png")
                    .asJson();
            DiscordSRV.debug("Received API response for webhook message delivery: " + response.getStatus());
        } catch (Exception e) {
            DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
            DiscordSRV.debug(ExceptionUtils.getMessage(e));
            e.printStackTrace();
        }
    }

}
