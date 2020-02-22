package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.track.UserTrackEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LuckPermsHook implements Listener {

    private final Set<EventSubscription<?>> subscriptions = new HashSet<>();

    public LuckPermsHook() {
        subscriptions.add(LuckPermsProvider.get().getEventBus().subscribe(UserTrackEvent.class, event -> handle(event.getUser().getUniqueId())));
    }

    private void handle(UUID user) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(user);
        DiscordSRV.getPlugin().getGroupSynchronizationManager().reSyncGroups(player, GroupSynchronizationManager.SyncDirection.TO_DISCORD);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() instanceof DiscordSRV) subscriptions.forEach(EventSubscription::close);
    }

}
