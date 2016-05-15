package com.scarsz.discordsrv.hooks;

import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;
import com.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.JDA;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LegendChatHook implements Listener {

    public LegendChatHook(){
        DiscordSRV.usingLegendChat = true;
    }
    @EventHandler
    public void onchat(ChatMessageEvent event) {

        if (DiscordSRV.channels.containsKey(event.getChannel().getName())) return;

        // make sure chat channel is registered
        if (!DiscordSRV.channels.containsKey(event.getChannel().getName())) return;

        // make sure chat channel is linked to discord channel
        if (DiscordSRV.channels.get(event.getChannel().getName()) == null) return;

        // make sure message isn't blank
        if (event.getMessage().replace(" ", "").isEmpty()) return;

        DiscordSRV.processChatEvent(false, event.getSender().getPlayer(), event.getMessage(), event.getChannel().getName());
    }
}
