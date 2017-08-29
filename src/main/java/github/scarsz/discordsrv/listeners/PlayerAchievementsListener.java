package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import github.scarsz.discordsrv.util.PrettyUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

@SuppressWarnings("deprecation")
public class PlayerAchievementsListener implements Listener {

    public PlayerAchievementsListener() {
        if (PlayerAchievementAwardedEvent.class.isAnnotationPresent(Deprecated.class)) return;

        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void PlayerAchievementAwardedEvent(PlayerAchievementAwardedEvent event) {
        // return if achievement messages are disabled
        if (StringUtils.isBlank(LangUtil.Message.PLAYER_ACHIEVEMENT.toString())) return;

        // return if achievement or player objects are fucking knackered because this can apparently happen for some reason
        if (event == null || event.getAchievement() == null || event.getPlayer() == null) return;

        // turn "SHITTY_ACHIEVEMENT_NAME" into "Shitty Achievement Name"
        String achievementName = PrettyUtil.beautify(event.getAchievement());

        String discordMessage = LangUtil.Message.PLAYER_ACHIEVEMENT.toString()
                .replace("%username%", event.getPlayer().getName())
                .replace("%displayname%", event.getPlayer().getDisplayName())
                .replace("%world%", event.getPlayer().getWorld().getName())
                .replace("%achievement%", achievementName);
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(event.getPlayer(), discordMessage);

        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordUtil.strip(discordMessage));
    }

}
