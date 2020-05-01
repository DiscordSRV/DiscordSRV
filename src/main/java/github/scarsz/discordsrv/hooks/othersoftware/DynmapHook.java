package github.scarsz.discordsrv.hooks.othersoftware;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapWebChatEvent;

public class DynmapHook implements OtherSoftwareHook {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void dynmapChatEvent(DynmapWebChatEvent event) {
        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), "**[Dynmap]** " + event.getName() + "» " + event.getMessage());
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("dynmap");
    }

    @Override
    public void broadcast(String message) {
        DynmapCommonAPI api = (DynmapCommonAPI) getPlugin();

        api.sendBroadcastToWeb("", message);
    }
}
