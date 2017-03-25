package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.PluginUtil;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.events.GMUserEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(PlayerJoinEvent event) {
        DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(event.getPlayer(),
               ((GroupManager) PluginUtil.getPlugin("GroupManager")).getWorldsHolder().getWorldPermissions(event.getPlayer()).getGroup(event.getPlayer().getName())
        );
    }

    @EventHandler
    public void on(GMUserEvent event) {
        DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(event.getUser().getBukkitPlayer(), event.getUser().getGroup().getName());
    }

}
