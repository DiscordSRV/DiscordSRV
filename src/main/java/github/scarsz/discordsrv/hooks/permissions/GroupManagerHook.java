package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import org.anjocaido.groupmanager.events.GMUserEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/5/2017
 * @at 2:05 PM
 */
public class GroupManagerHook implements Listener, PermissionSystemHook {

    public GroupManagerHook() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler
    public void on(GMUserEvent event) {
        DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(event.getUser().getBukkitPlayer(), event.getUser().getGroup().getName());
    }

}
