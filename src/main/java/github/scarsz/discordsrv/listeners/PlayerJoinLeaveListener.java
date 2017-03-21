package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.GamePermissionUtil;
import github.scarsz.discordsrv.util.LangUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 4:29 PM
 */
public class PlayerJoinLeaveListener implements Listener {

    @EventHandler
    public void PlayerJoinEvent(PlayerJoinEvent event) {
        // If player is OP & update is available tell them
        if (GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.updatenotification") && DiscordSRV.updateIsAvailable) {
            event.getPlayer().sendMessage(ChatColor.AQUA + "An update to DiscordSRV is available. Download it at http://dev.bukkit.org/bukkit-plugins/discordsrv/");
        }

        String joinMessageFormat = event.getPlayer().hasPlayedBefore()
                ? LangUtil.Message.PLAYER_JOIN.toString()
                : LangUtil.Message.PLAYER_JOIN_FIRST_TIME.toString()
        ;

        // Make sure join messages enabled
        if (StringUtils.isBlank(joinMessageFormat)) return;

        // Check if player has permission to not have join messages
        if (event.getPlayer().hasPermission("discordsrv.silentjoin")) {
            DiscordSRV.info(LangUtil.InternalMessage.SILENT_JOIN.toString()
                    .replace("{player}", event.getPlayer().getName())
            );
            return;
        }

        // schedule command to run in a second to be able to capture display name
        Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> {
            // Player doesn't have silent join permission, send join message
            DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), joinMessageFormat
                    .replace("%username%", DiscordUtil.escapeMarkdown(event.getPlayer().getName()))
                    .replace("%displayname%", DiscordUtil.escapeMarkdown(DiscordUtil.stripColor(event.getPlayer().getDisplayName())))
            );
        }, 20);
    }
    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        // Make sure quit messages enabled
        if (StringUtils.isBlank(LangUtil.Message.PLAYER_LEAVE.toString())) return;

        // No quit message, user shouldn't have one from permission
        if (event.getPlayer().hasPermission("discordsrv.silentquit")) {
            DiscordSRV.info(LangUtil.InternalMessage.SILENT_QUIT.toString()
                    .replace("{player}", event.getPlayer().getName())
            );
            return;
        }

        // Player doesn't have silent quit, show quit message
        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), LangUtil.Message.PLAYER_LEAVE.toString()
                .replace("%username%", DiscordUtil.escapeMarkdown(event.getPlayer().getName()))
                .replace("%displayname%", DiscordUtil.escapeMarkdown(DiscordUtil.stripColor(event.getPlayer().getDisplayName())))
        );
    }

}
