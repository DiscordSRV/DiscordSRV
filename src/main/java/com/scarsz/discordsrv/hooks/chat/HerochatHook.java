package com.scarsz.discordsrv.hooks.chat;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class HerochatHook implements Listener {

    public HerochatHook() {
        DiscordSRV.usingHerochat = true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(ChannelChatEvent event) {
        // make sure event is allowed
        if (event.getResult() != Chatter.Result.ALLOWED) return;

        // make sure chat channel is registered
        if (!DiscordSRV.chatChannelIsLinked(event.getChannel().getName())) return;

        // make sure chat channel is linked to discord channel
        if (DiscordSRV.getTextChannelFromChannelName(event.getChannel().getName()) == null) return;

        // make sure message isn't blank
        if (event.getMessage().replace(" ", "").isEmpty()) return;

        DiscordSRV.processChatEvent(false, event.getSender().getPlayer(), event.getMessage(), event.getChannel().getName());
    }

    public static void broadcastMessageToChannel(String channelName, String message, String rawMessage) {
        Channel chatChannel = Herochat.getChannelManager().getChannel(channelName);
        if (chatChannel == null) return; // no suitable channel found
        chatChannel.sendRawMessage(DiscordSRV.plugin.getConfig().getString("ChatChannelHookMessageFormat")
                .replace("%channelcolor%", chatChannel.getColor().toString())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", chatChannel.getNick())
                .replace("%message%", message));

        // notify players
        List<Player> playersToNotify = new ArrayList<>();
        chatChannel.getMembers().forEach(chatter -> playersToNotify.add(chatter.getPlayer()));
        DiscordSRV.notifyPlayersOfMentions(playersToNotify, rawMessage);
    }
    
}
