package com.scarsz.discordsrv.hooks.chat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.github.ucchyocean.lc.LunaChat;
import com.github.ucchyocean.lc.channel.Channel;
import com.github.ucchyocean.lc.event.LunaChatChannelChatEvent;
import com.scarsz.discordsrv.DiscordSRV;

public class LunaChatHook implements Listener {

    public LunaChatHook() {
        DiscordSRV.usingLunaChat = true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(LunaChatChannelChatEvent event) {

        // make sure chat channel is registered
        if (!DiscordSRV.chatChannelIsLinked(event.getChannel().getName())) return;

        // make sure chat channel is linked to discord channel
        if (DiscordSRV.getTextChannelFromChannelName(event.getChannel().getName()) == null) return;

        // make sure message isn't blank
        if (event.getNgMaskedMessage().trim().isEmpty()) return;

        // get sender player
        Player player = (event.getPlayer() != null) ? event.getPlayer().getPlayer() : null;

        DiscordSRV.processChatEvent(false, player, event.getNgMaskedMessage(), event.getChannel().getName());
    }

    public static void broadcastMessageToChannel(String channelName, String message, String rawMessage) {

        Channel chatChannel = LunaChat.getInstance().getLunaChatAPI().getChannel(channelName);
        if (chatChannel == null) return; // no suitable channel found
        chatChannel.sendMessage(null, "", ChatColor.translateAlternateColorCodes('&', DiscordSRV.plugin.getConfig().getString("ChatChannelHookMessageFormat")
                .replace("%channelcolor%", chatChannel.getColorCode())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", (chatChannel.getAlias().equals("")) ? chatChannel.getName() : chatChannel.getAlias() )
                .replace("%message%", message)), true, "Discord");

        // notify players
        List<Player> playersToNotify = new ArrayList<>();
        chatChannel.getMembers().forEach(chatter -> playersToNotify.add(chatter.getPlayer()));
        DiscordSRV.notifyPlayersOfMentions(playersToNotify, rawMessage);
    }

}
