package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.track.UserTrackEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
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
        subscriptions.add(LuckPermsProvider.get().getEventBus().subscribe(NodeAddEvent.class, event -> handle(event, event.getNode())));
        subscriptions.add(LuckPermsProvider.get().getEventBus().subscribe(NodeRemoveEvent.class, event -> handle(event, event.getNode())));
    }

    private void handle(NodeMutateEvent event, Node node) {
        if (event.isUser() && node.getType() == NodeType.INHERITANCE) {
            handle(((User) event.getTarget()).getUniqueId());
        }
    }

    private void handle(UUID user) {
        if (DiscordSRV.config().getBoolean("GroupRoleSynchronizationEnabled")) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(user);
            Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordSRV.getPlugin(),
                    () -> DiscordSRV.getPlugin().getGroupSynchronizationManager().resync(player, GroupSynchronizationManager.SyncDirection.TO_DISCORD),
                    5
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() instanceof DiscordSRV) subscriptions.forEach(EventSubscription::close);
    }

}
