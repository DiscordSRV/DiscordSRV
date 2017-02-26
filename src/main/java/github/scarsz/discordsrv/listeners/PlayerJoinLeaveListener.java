package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 4:29 PM
 */
public class PlayerJoinLeaveListener implements Listener {

    private Map<Player, Boolean> playerStatusIsOnline = new HashMap<>();

    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent event) {
        // If player is OP & update is available tell them
        if ((event.getPlayer().isOp() || event.getPlayer().hasPermission("discordsrv.admin")) && DiscordSRV.updateIsAvailable) {
            event.getPlayer().sendMessage(ChatColor.AQUA + "An update to DiscordSRV is available. Download it at http://dev.bukkit.org/bukkit-plugins/discordsrv/");
        }

        String joinMessageFormat = event.getPlayer().hasPlayedBefore()
                ? DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerJoinMessageFormat")
                : DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerFirstJoinMessageFormat")
        ;

        // Make sure join messages enabled
        if (StringUtils.isBlank(joinMessageFormat)) return;

        // Check if player has permission to not have join messages
        if (event.getPlayer().hasPermission("discordsrv.silentjoin")) {
            DiscordSRV.info("Player " + event.getPlayer().getName() + " joined with silent joining permission, not sending a join message");
            return;
        }

        // Assign player's status to online since they don't have silent join permissions
        playerStatusIsOnline.put(event.getPlayer(), true);

        // Player doesn't have silent join permission, send join message
        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), joinMessageFormat
                .replace("%username%", DiscordUtil.escapeMarkdown(event.getPlayer().getName()))
                .replace("%displayname%", DiscordUtil.stripColor(DiscordUtil.escapeMarkdown(event.getPlayer().getDisplayName())))
        );
    }
    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        // Make sure quit messages enabled
        if (StringUtils.isBlank(DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerLeaveMessageFormat"))) return;

        // No quit message, user shouldn't have one from permission
        if (event.getPlayer().hasPermission("discordsrv.silentquit")) {
            DiscordSRV.info("Player " + event.getPlayer().getName() + " quit with silent quiting permission, not sending a quit message");
            return;
        }

        // Remove player from status map to help with memory management
        playerStatusIsOnline.remove(event.getPlayer());

        // Player doesn't have silent quit, show quit message
        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerLeaveMessageFormat")
                .replace("%username%", DiscordUtil.escapeMarkdown(event.getPlayer().getName()))
                .replace("%displayname%", DiscordUtil.stripColor(DiscordUtil.escapeMarkdown(event.getPlayer().getDisplayName())))
        );
    }
    @EventHandler
    public void PlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        if (isFakeJoin(event.getMessage()) && event.getPlayer().hasPermission("vanish.fakeannounce") && StringUtils.isNotBlank(DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerJoinMessageFormat"))) {
            // Player has permission to fake join messages

            // Set player's status if they don't already have one
            if (!playerStatusIsOnline.containsKey(event.getPlayer())) playerStatusIsOnline.put(event.getPlayer(), false);

            // Make sure player's status isn't already "online" and isn't a forced command
            // (player is already online AND command is not forced)
            if (playerStatusIsOnline.get(event.getPlayer()) && !isForceFakeJoin(event.getMessage())) return;

            // Set status as online
            playerStatusIsOnline.put(event.getPlayer(), true);

            // Send fake join message
            DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerJoinMessageFormat")
                    .replace("%username%", DiscordUtil.escapeMarkdown(event.getPlayer().getName()))
                    .replace("%displayname%", DiscordUtil.stripColor(DiscordUtil.escapeMarkdown(event.getPlayer().getDisplayName())))
            );
        } else if (isFakeQuit(event.getMessage()) && event.getPlayer().hasPermission("vanish.fakeannounce") && StringUtils.isNotBlank(DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerLeaveMessageFormat"))) {
            // Player has permission to fake quit messages

            // Set player's status if they don't already have one
            if (!playerStatusIsOnline.containsKey(event.getPlayer())) playerStatusIsOnline.put(event.getPlayer(), true);

            // Make sure player's status isn't already "offline" and isn't a forced command
            // (player is already offline AND command is not forced)
            if (!playerStatusIsOnline.get(event.getPlayer()) && !isForceFakeQuit(event.getMessage())) return;

            // Set status as online
            playerStatusIsOnline.put(event.getPlayer(), false);

            // Send fake quit message
            DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerLeaveMessageFormat")
                    .replace("%username%", DiscordUtil.escapeMarkdown(event.getPlayer().getName()))
                    .replace("%displayname%", DiscordUtil.stripColor(DiscordUtil.escapeMarkdown(event.getPlayer().getDisplayName())))
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
