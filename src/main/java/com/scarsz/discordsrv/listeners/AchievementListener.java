package com.scarsz.discordsrv.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

import com.scarsz.discordsrv.DiscordSRV;

import net.dv8tion.jda.JDA;

public class AchievementListener implements Listener {

    JDA api;

    public AchievementListener(JDA api) {
        this.api = api;
    }

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
