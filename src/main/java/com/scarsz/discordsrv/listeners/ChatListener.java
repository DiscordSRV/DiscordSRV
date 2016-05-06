package com.scarsz.discordsrv.listeners;

import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        DiscordSRV.processChatEvent(event.isCancelled(), event.getPlayer(), event.getMessage(), null);
    }

}