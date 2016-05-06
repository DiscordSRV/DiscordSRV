package com.scarsz.discordsrv.hooks;

import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
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
    public void onHerochatMessage(ChannelChatEvent event) {
        // make sure event is allowed
        if (event.getResult() != Chatter.Result.ALLOWED) return;
        // make sure chat channel is registered
        if (!DiscordSRV.channels.containsKey(event.getChannel().getName())) return;
        // make sure chat channel is linked to discord channel
        if (DiscordSRV.channels.get(event.getChannel().getName()) == null) return;
        // make sure message isn't blank
        if (event.getMessage().replace(" ", "").isEmpty()) return;

        DiscordSRV.processChatEvent(false, event.getSender().getPlayer(), event.getMessage(), event.getChannel().getName());
    }

    public static void broadcastMessageToChannel(String channel, String message) {
        Bukkit.getScheduler().runTask(DiscordSRV.plugin, () -> {
            List<Player> possiblyPlayers = new ArrayList<>();
            Herochat.getChannelManager().getChannel(channel).getMembers().forEach(chatter -> possiblyPlayers.add(chatter.getPlayer()));
            DiscordSRV.notifyPlayersOfMentions(possiblyPlayers, message);
        });

        Herochat.getChannelManager().getChannel(channel).sendRawMessage(message);
    }

}
