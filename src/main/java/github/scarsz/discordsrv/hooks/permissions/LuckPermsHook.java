/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import github.scarsz.discordsrv.util.PluginUtil;
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
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LuckPermsHook implements PluginHook {

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
        if (!DiscordSRV.getPlugin().isGroupRoleSynchronizationEnabled()) return;
        OfflinePlayer player = Bukkit.getOfflinePlayer(user);
        Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordSRV.getPlugin(),
                () -> DiscordSRV.getPlugin().getGroupSynchronizationManager().resync(player, GroupSynchronizationManager.SyncDirection.TO_DISCORD),
                5
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() instanceof DiscordSRV) subscriptions.forEach(EventSubscription::close);
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("LuckPerms");
    }

}
