package com.scarsz.discordsrv.listeners;

import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void PlayerDeathEvent(PlayerDeathEvent event) {
        // return if death messages are disabled
        if (!DiscordSRV.plugin.getConfig().getBoolean("MinecraftPlayerDeathMessageEnabled")) return;
        
        DiscordSRV.sendMessage(DiscordSRV.chatChannel, ChatColor.stripColor(event.getDeathMessage()
                .replace(event.getEntity().getName(), DiscordSRV.escapeMarkdown(event.getEntity().getName())))
        );
    }

}
