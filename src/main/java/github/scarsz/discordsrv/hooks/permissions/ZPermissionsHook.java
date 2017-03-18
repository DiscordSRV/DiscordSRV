package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.hooks.VaultHook;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/5/2017
 * @at 2:07 PM
 */
public class ZPermissionsHook implements PermissionSystemHook {

    public ZPermissionsHook() {
        if (!PluginUtil.checkIfPluginEnabled("vault")) {
            DiscordSRV.warning(LangUtil.InternalMessage.ZPERMISSIONS_VAULT_REQUIRED);
            return;
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordSRV.getPlugin(), () -> sync(PlayerUtil.getOnlinePlayers()), 0, 20 * 60 * 5);
    }

    private void sync(List<Player> playersToSync) {
        for (Player player : playersToSync) {
            DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(player, VaultHook.getPlayersGroups(player));
        }
    }


}
