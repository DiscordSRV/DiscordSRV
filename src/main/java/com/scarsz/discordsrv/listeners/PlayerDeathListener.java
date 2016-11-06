package com.scarsz.discordsrv.listeners;

import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void PlayerDeathEvent(PlayerDeathEvent event) {
        // return if death messages are disabled
        if (!DiscordSRV.plugin.getConfig().getBoolean("MinecraftPlayerDeathMessageEnabled")) return;

        if (event.getEntityType() != EntityType.PLAYER) return;

        DiscordSRV.sendMessage(DiscordSRV.chatChannel, ChatColor.stripColor(DiscordSRV.plugin.getConfig().getString("MinecraftPlayerDeathMessageFormat")
            .replace("%username%", event.getEntity().getName())
            .replace("%displayname%", DiscordSRV.escapeMarkdown(event.getEntity().getDisplayName()))
            .replace("%world%", event.getEntity().getWorld().getName())
            .replace("%deathmessage%", DiscordSRV.escapeMarkdown(event.getDeathMessage()))
        ));
    }

}
