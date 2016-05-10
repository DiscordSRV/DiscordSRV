package com.scarsz.discordsrv.listeners;

import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;
import com.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.JDA;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Date;

/**
 * Created by TimeTheCat on 5/9/2016.
 */
public class LegendChatListener implements Listener {
    JDA api;
    public LegendChatListener(JDA api) {
        this.api = api;
    }
    @EventHandler
    public void onLegendChat(ChatMessageEvent event) {
        //check if channel is restricted
        if (DiscordSRV.plugin.getConfig().getList("blacklisted-legendchat-channels").contains(event.getChannel().getName())) return;

        // ReportCanceledChatEvents debug message
        if (DiscordSRV.plugin.getConfig().getBoolean("ReportCanceledChatEvents")) DiscordSRV.plugin.getLogger().info("Chat message received, canceled: " + event.isCancelled());

        // return if event canceled
        if (DiscordSRV.plugin.getConfig().getBoolean("DontSendCanceledChatEvents") && event.isCancelled()) return;

        // return if should not send in-game chat
        if (!DiscordSRV.plugin.getConfig().getBoolean("DiscordChatChannelMinecraftToDiscord")) return;

        // return if user is unsubscribed from Discord and config says don't send those peoples' messages
        if (!DiscordSRV.getSubscribed(event.getSender().getUniqueId()) && !DiscordSRV.plugin.getConfig().getBoolean("MinecraftUnsubscribedMessageForwarding")) return;

        // return if doesn't match prefix filter
        if (!event.getMessage().startsWith(DiscordSRV.plugin.getConfig().getString("DiscordChatChannelPrefix"))) return;

        String message = DiscordSRV.plugin.getConfig().getString("MinecraftChatToDiscordMessageFormat")
                .replaceAll("&([0-9a-qs-z])", "")
                .replace("%message%", ChatColor.stripColor(event.getMessage()))
                .replace("%primarygroup%", DiscordSRV.getPrimaryGroup(event.getSender()))
                .replace("%displayname%", ChatColor.stripColor(event.getSender().getDisplayName()))
                .replace("%username%", ChatColor.stripColor(event.getSender().getName()))
                .replace("%time%", new Date().toString());

        message = DiscordSRV.convertMentionsFromNames(message);

        DiscordSRV.sendMessage(DiscordSRV.chatChannel, message);
    }
}
