package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.ArrayList;
import java.util.List;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/5/2017
 * @at 2:05 PM
 */
@SuppressWarnings("deprecation")
public class PermissionsExHook implements PermissionSystemHook, Listener {

    public PermissionsExHook() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());

        Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordSRV.getPlugin(), () -> sync(PlayerUtil.getOnlinePlayers()), 0, 20 * 60 * 5);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        handleCommand(event.getMessage());
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        handleCommand(event.getCommand());
    }
    private void handleCommand(String command) {
        if (command.replace("/", "").contains("pex user") && command.replace("/", "").contains("group")) {
            List<Player> playersToReSync = new ArrayList<>();
            for (String part : command.split(" ")) {
                Player player = Bukkit.getPlayerExact(part);
                if (player != null) playersToReSync.add(player);
            }
            sync(playersToReSync);
        }
    }

    private void sync(List<Player> playersToReSync) {
        playersToReSync.forEach(player -> {
            List<String> userGroups = new ArrayList<>();
            PermissionsEx.getUser(player).getParents().forEach(permissionGroup -> userGroups.add(permissionGroup.getName()));

            DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(player, (String[]) userGroups.toArray());
        });
    }

}
