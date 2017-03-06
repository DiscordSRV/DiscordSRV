package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 4:26 PM
 */
public class PlayerDeathListener implements Listener {

    @EventHandler
    public void PlayerDeathEvent(PlayerDeathEvent event) {
        // return if death messages are disabled
        if (StringUtils.isBlank(DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerDeathMessageFormat"))) return;

        if (event.getEntityType() != EntityType.PLAYER) return;

        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordUtil.stripColor(DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerDeathMessageFormat")
                .replace("%username%", event.getEntity().getName())
                .replace("%displayname%", DiscordUtil.escapeMarkdown(event.getEntity().getDisplayName()))
                .replace("%world%", event.getEntity().getWorld().getName())
                .replace("%deathmessage%", DiscordUtil.escapeMarkdown(event.getDeathMessage()))
        ));
    }

}
