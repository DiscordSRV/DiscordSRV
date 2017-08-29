package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    public PlayerDeathListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void PlayerDeathEvent(PlayerDeathEvent event) {
        // return if death messages are disabled
        if (StringUtils.isBlank(LangUtil.Message.PLAYER_DEATH.toString())) return;

        if (event.getEntityType() != EntityType.PLAYER) return;

        if (event.getDeathMessage() == null) return;

        String discordMessage = LangUtil.Message.PLAYER_DEATH.toString()
                .replace("%username%", event.getEntity().getName())
                .replace("%displayname%", DiscordUtil.escapeMarkdown(event.getEntity().getDisplayName()))
                .replace("%world%", event.getEntity().getWorld().getName())
                .replace("%deathmessage%", DiscordUtil.escapeMarkdown(event.getDeathMessage()));
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(event.getEntity(), discordMessage);
        discordMessage = DiscordUtil.strip(discordMessage);

        if (discordMessage.replaceAll("[^A-z]", "").length() < 3) {
            DiscordSRV.debug("Not sending death message \"" + discordMessage + "\" because it's less than three characters long");
            return;
        }

        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), discordMessage);
    }

}
