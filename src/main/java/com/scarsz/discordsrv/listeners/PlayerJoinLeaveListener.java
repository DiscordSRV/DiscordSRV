package com.scarsz.discordsrv.listeners;

import com.scarsz.discordsrv.DiscordSRV;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

public class PlayerJoinLeaveListener implements Listener {

    private Map<Player, Boolean> playerStatusIsOnline = new HashMap<>();

    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent event) {
        // If player is OP & update is available tell them
        if (event.getPlayer().isOp() && DiscordSRV.updateIsAvailable) event.getPlayer().sendMessage(ChatColor.AQUA + "An update to DiscordSRV is available. Download it at http://dev.bukkit.org/bukkit-plugins/discordsrv/");

        // Make sure join messages enabled
        if (!DiscordSRV.plugin.getConfig().getBoolean("MinecraftPlayerJoinMessageEnabled")) return;

        // Check if player has permission to not have join messages
        if (event.getPlayer().hasPermission("discordsrv.silentjoin")) {
            DiscordSRV.plugin.getLogger().info("Player " + event.getPlayer().getName() + " joined with silent joining permission, not sending a join message");
            return;
        }

        // Assign player's status to online since they don't have silent join permissions
        playerStatusIsOnline.put(event.getPlayer(), true);

        // Player doesn't have silent join permission, send join message
        DiscordSRV.sendMessage(DiscordSRV.chatChannel, DiscordSRV.plugin.getConfig().getString("MinecraftPlayerJoinMessageFormat")
                .replace("%username%", DiscordSRV.escapeMarkdown(event.getPlayer().getName()))
                .replace("%displayname%", ChatColor.stripColor(DiscordSRV.escapeMarkdown(event.getPlayer().getDisplayName())))
        );
    }
    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        // Make sure quit messages enabled
        if (!DiscordSRV.plugin.getConfig().getBoolean("MinecraftPlayerLeaveMessageEnabled")) return;

        // No quit message, user shouldn't have one from permission
        if (event.getPlayer().hasPermission("discordsrv.silentquit")) {
            DiscordSRV.plugin.getLogger().info("Player " + event.getPlayer().getName() + " quit with silent quiting permission, not sending a quit message");
            return;
        }

        // Remove player from status map to help with memory management
        playerStatusIsOnline.remove(event.getPlayer());

        // Player doesn't have silent quit, show quit message
        DiscordSRV.sendMessage(DiscordSRV.chatChannel, DiscordSRV.plugin.getConfig().getString("MinecraftPlayerLeaveMessageFormat")
                .replace("%username%", DiscordSRV.escapeMarkdown(event.getPlayer().getName()))
                .replace("%displayname%", ChatColor.stripColor(DiscordSRV.escapeMarkdown(event.getPlayer().getDisplayName())))
        );
    }
    @EventHandler
    public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        if (isFakeJoin(event.getMessage()) && event.getPlayer().hasPermission("vanish.fakeannounce") && DiscordSRV.plugin.getConfig().getBoolean("MinecraftPlayerJoinMessageEnabled")) {
            // Player has permission to fake join messages

            // Set player's status if they don't already have one
            if (!playerStatusIsOnline.containsKey(event.getPlayer())) playerStatusIsOnline.put(event.getPlayer(), false);

            // Make sure player's status isn't already "online" and isn't a forced command
            // (player is already online AND command is not forced)
            if (playerStatusIsOnline.get(event.getPlayer()) && !isForceFakeJoin(event.getMessage())) return;

            // Set status as online
            playerStatusIsOnline.put(event.getPlayer(), true);

            // Send fake join message
            DiscordSRV.sendMessage(DiscordSRV.chatChannel, DiscordSRV.plugin.getConfig().getString("MinecraftPlayerJoinMessageFormat")
                    .replace("%username%", DiscordSRV.escapeMarkdown(event.getPlayer().getName()))
                    .replace("%displayname%", ChatColor.stripColor(DiscordSRV.escapeMarkdown(event.getPlayer().getDisplayName())))
            );
        } else if (isFakeQuit(event.getMessage()) && event.getPlayer().hasPermission("vanish.fakeannounce") && DiscordSRV.plugin.getConfig().getBoolean("MinecraftPlayerLeaveMessageEnabled")) {
            // Player has permission to fake quit messages

            // Set player's status if they don't already have one
            if (!playerStatusIsOnline.containsKey(event.getPlayer())) playerStatusIsOnline.put(event.getPlayer(), true);

            // Make sure player's status isn't already "offline" and isn't a forced command
            // (player is already offline AND command is not forced)
            if (!playerStatusIsOnline.get(event.getPlayer()) && !isForceFakeQuit(event.getMessage())) return;

            // Set status as online
            playerStatusIsOnline.put(event.getPlayer(), false);

            // Send fake quit message
            DiscordSRV.sendMessage(DiscordSRV.chatChannel, DiscordSRV.plugin.getConfig().getString("MinecraftPlayerLeaveMessageFormat")
                    .replace("%username%", DiscordSRV.escapeMarkdown(event.getPlayer().getName()))
                    .replace("%displayname%", ChatColor.stripColor(DiscordSRV.escapeMarkdown(event.getPlayer().getDisplayName())))
            );
        }
    }

    private boolean isFakeJoin(String message) {
        return message.startsWith("/v fj") || message.startsWith("/vanish fj") || message.startsWith("/v fakejoin") || message.startsWith("/vanish fakejoin");
    }
    private boolean isFakeQuit(String message) {
        return message.startsWith("/v fq") || message.startsWith("/vanish fq") || message.startsWith("/v fakequit") || message.startsWith("/vanish fakequit");
    }
    private boolean isForceFakeJoin(String message) {
        return isFakeJoin(message) && (message.endsWith(" f") || message.endsWith(" force"));
    }
    private boolean isForceFakeQuit(String message) {
        return isFakeQuit(message) && (message.endsWith(" f") || message.endsWith(" force"));
    }
}
