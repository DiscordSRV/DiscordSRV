package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.event.EventBus;
import me.lucko.luckperms.api.event.user.track.UserPromoteEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/5/2017
 * @at 2:07 PM
 */
public class LuckPermsHook implements PermissionSystemHook, Listener {

    private final EventBus eventBus;
    private final EventHandler eventHandler;

    public LuckPermsHook() {
        eventBus = LuckPerms.getApi().getEventBus();
        eventHandler = (EventHandler) eventBus.subscribe(UserPromoteEvent.class, this::on);
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    private void on(UserPromoteEvent event) {
        DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(Bukkit.getPlayer(event.getUser().getUuid()), (String[]) event.getUser().getGroupNames().toArray());
    }

}
