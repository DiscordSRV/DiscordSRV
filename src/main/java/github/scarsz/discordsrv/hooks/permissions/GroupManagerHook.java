package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.PlayerUtil;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.events.GMGroupEvent;
import org.anjocaido.groupmanager.events.GMSystemEvent;
import org.anjocaido.groupmanager.events.GMUserEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    @EventHandler
    public void on(GMGroupEvent event) {
        for (Player player : PlayerUtil.getOnlinePlayers()) {
            String groupName = ((GroupManager) Bukkit.getPluginManager().getPlugin("GroupManager")).getWorldsHolder().getWorldData(player).getUser(player.getName()).getGroupName();
            DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(player, groupName);
        }
    }

    @EventHandler
    public void on(GMSystemEvent event) {
        for (Player player : PlayerUtil.getOnlinePlayers()) {
            String groupName = ((GroupManager) Bukkit.getPluginManager().getPlugin("GroupManager")).getWorldsHolder().getWorldData(player).getUser(player.getName()).getGroupName();
            DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(player, groupName);
        }
    }

}
