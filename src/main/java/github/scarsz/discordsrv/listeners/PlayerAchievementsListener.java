package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

import java.util.LinkedList;
import java.util.List;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 4:30 PM
 */
public class PlayerAchievementsListener implements Listener {

    @EventHandler
    public void PlayerAchievementAwardedEvent(PlayerAchievementAwardedEvent event) {
        // return if achievement messages are disabled
        if (!DiscordSRV.getPlugin().getConfig().getBoolean("MinecraftPlayerAchievementMessagesEnabled")) return;

        // return if achievement or player objects are fucking knackered
        if (event == null || event.getAchievement() == null || event.getPlayer() == null) return;

        // turn "SHITTY_ACHIEVEMENT_NAME" into "Shitty Achievement Name"
        List<String> achievementNameParts = new LinkedList<>();
        for (String s : event.getAchievement().toString().toLowerCase().split("_"))
            achievementNameParts.add(s.substring(0, 1).toUpperCase() + s.substring(1));
        String achievementName = String.join(" ", achievementNameParts);

        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordUtil.stripColor(DiscordSRV.getPlugin().getConfig().getString("MinecraftPlayerAchievementMessagesFormat")
                .replace("%username%", event.getPlayer().getName())
                .replace("%displayname%", event.getPlayer().getDisplayName())
                .replace("%world%", event.getPlayer().getWorld().getName())
                .replace("%achievement%", achievementName)
        ));
    }

}
