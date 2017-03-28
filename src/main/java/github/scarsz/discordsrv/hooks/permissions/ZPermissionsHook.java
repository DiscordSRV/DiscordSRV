package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collections;
import java.util.List;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/5/2017
 * @at 2:07 PM
 */
public class ZPermissionsHook implements PermissionSystemHook, Listener {

    public ZPermissionsHook() {
        if (!PluginUtil.checkIfPluginEnabled("vault")) {
            DiscordSRV.warning(LangUtil.InternalMessage.ZPERMISSIONS_VAULT_REQUIRED);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
        Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordSRV.getPlugin(), () -> sync(PlayerUtil.getOnlinePlayers()), 0, 20 * 60 * 5);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(PlayerJoinEvent event) {
        sync(Collections.singletonList(event.getPlayer()));
    }

    private void sync(List<Player> playersToSync) {
        for (Player player : playersToSync) {
            DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(player, VaultHook.getPlayersGroups(player));
        }
    }

}
