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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class LuckPermsHook implements PluginHook {

    private final net.luckperms.api.LuckPerms luckPerms;
    private final Set<net.luckperms.api.event.EventSubscription<?>> subscriptions = new HashSet<>();
    private net.luckperms.api.context.ContextCalculator<?> contextCalculator;

    public LuckPermsHook() {
        luckPerms = Bukkit.getServicesManager().load(net.luckperms.api.LuckPerms.class);

        if (luckPerms == null) {
            DiscordSRV.error("Failed to get LuckPerms service. Is LuckPerms enabled?");
            return;
        }

        // update events
        if (!DiscordSRV.config().getStringList("DisabledPluginHooks").contains("LuckPerms-GroupUpdates")) {
            subscriptions.add(luckPerms.getEventBus().subscribe(net.luckperms.api.event.user.track.UserTrackEvent.class, event -> handle(event.getUser().getUniqueId())));
            subscriptions.add(luckPerms.getEventBus().subscribe(net.luckperms.api.event.node.NodeAddEvent.class, event -> handle(event, event.getNode(), true)));
            subscriptions.add(luckPerms.getEventBus().subscribe(net.luckperms.api.event.node.NodeRemoveEvent.class, event -> handle(event, event.getNode(), false)));
        }

        // contexts
        if (!DiscordSRV.config().getStringList("DisabledPluginHooks").contains("LuckPerms-Contexts")) {
            contextCalculator = new ContextCalculator();
            luckPerms.getContextManager().registerCalculator(contextCalculator);
        }
    }

    private void handle(net.luckperms.api.event.node.NodeMutateEvent event, net.luckperms.api.node.Node node, boolean add) {
        if (event.isUser() && node.getType() == net.luckperms.api.node.NodeType.INHERITANCE) {
            String groupName = net.luckperms.api.node.NodeType.INHERITANCE.cast(node).getGroupName();
            UUID uuid = ((net.luckperms.api.model.user.User) event.getTarget()).getUniqueId();
            Map<String, List<String>> justModified = DiscordSRV.getPlugin()
                    .getGroupSynchronizationManager().getJustModifiedGroups().getOrDefault(uuid, null);
            if (justModified != null && justModified.getOrDefault(add ? "add" : "remove", Collections.emptyList()).remove(groupName)) {
                return;
            }
            handle(((net.luckperms.api.model.user.User) event.getTarget()).getUniqueId());
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
        if (event.getPlugin() instanceof DiscordSRV) {
            subscriptions.forEach(net.luckperms.api.event.EventSubscription::close);
            if (contextCalculator != null) luckPerms.getContextManager().unregisterCalculator(contextCalculator);
        }
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("LuckPerms");
    }

}
