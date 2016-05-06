package com.scarsz.discordsrv.listeners;

import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

public class AchievementListener implements Listener {

    @EventHandler
    public void PlayerAchievementAwardedEvent(PlayerAchievementAwardedEvent event) {
        // return if achievement messages are disabled
        if (!DiscordSRV.plugin.getConfig().getBoolean("MinecraftPlayerAchievementMessagesEnabled")) return;

        // return if achievement or player objects are fucking knackered
        if (event == null || event.getAchievement() == null || event.getPlayer() == null) return;

        DiscordSRV.sendMessage(DiscordSRV.chatChannel, ChatColor.stripColor(DiscordSRV.plugin.getConfig().getString("MinecraftPlayerAchievementMessagesFormat")
            .replace("%username%", event.getPlayer().getName())
            .replace("%displayname%", event.getPlayer().getDisplayName())
            .replace("%world%", event.getPlayer().getWorld().getName())
            .replace("%achievement%", event.getAchievement().toString())
        ));
    }
}
