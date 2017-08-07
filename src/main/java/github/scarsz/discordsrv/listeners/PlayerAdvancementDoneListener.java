package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PlayerAdvancementDoneListener implements Listener {

    public PlayerAdvancementDoneListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        // return if achievement messages are disabled
        if (StringUtils.isBlank(LangUtil.Message.PLAYER_ACHIEVEMENT.toString())) return;

        // return if achievement or player objects are fucking knackered because this can apparently happen for some reason
        if (event == null || event.getAdvancement() == null || event.getPlayer() == null) return;

        // turn "SHITTY_ACHIEVEMENT_NAME" into "Shitty Achievement Name"
        String achievementName = Arrays.stream(event.getAdvancement().getClass().getSimpleName().toLowerCase().split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));

        String discordMessage = LangUtil.Message.PLAYER_ACHIEVEMENT.toString()
                .replace("%username%", event.getPlayer().getName())
                .replace("%displayname%", event.getPlayer().getDisplayName())
                .replace("%world%", event.getPlayer().getWorld().getName())
                .replace("%achievement%", achievementName);
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(event.getPlayer(), discordMessage);

        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), DiscordUtil.strip(discordMessage));
    }

}
