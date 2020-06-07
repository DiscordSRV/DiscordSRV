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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

public class LuckPermsHook implements PluginHook, net.luckperms.api.context.ContextCalculator<Player> {

    private static final String CONTEXT_LINKED = "discordsrv:linked";
    private static final String CONTEXT_BOOSTING = "discordsrv:boosting";
    private static final String CONTEXT_ROLE = "discordsrv:role";

    private final net.luckperms.api.LuckPerms luckPerms;
    private final Set<net.luckperms.api.event.EventSubscription<?>> subscriptions = new HashSet<>();

    public LuckPermsHook() {
        luckPerms = Bukkit.getServicesManager().load(net.luckperms.api.LuckPerms.class);

        if (luckPerms == null) {
            DiscordSRV.error("Failed to get LuckPerms service. Is LuckPerms enabled?");
            return;
        }

        // update events
        if (!DiscordSRV.config().getStringList("DisabledPluginHooks").contains("LuckPerms-GroupUpdates")) {
            DiscordSRV.debug("Enabling LuckPerms' instant group updates");
            subscriptions.add(luckPerms.getEventBus().subscribe(net.luckperms.api.event.user.track.UserTrackEvent.class, event -> handle(event.getUser().getUniqueId())));
            subscriptions.add(luckPerms.getEventBus().subscribe(net.luckperms.api.event.node.NodeAddEvent.class, event -> handle(event, event.getNode(), true)));
            subscriptions.add(luckPerms.getEventBus().subscribe(net.luckperms.api.event.node.NodeRemoveEvent.class, event -> handle(event, event.getNode(), false)));
        } else {
            DiscordSRV.debug("Not using LuckPerms' instant group updates because they are disabled in the config");
        }

        // contexts
        if (!DiscordSRV.config().getStringList("DisabledPluginHooks").contains("LuckPerms-Contexts")) {
            DiscordSRV.debug("Enabling LuckPerms' contexts");
            luckPerms.getContextManager().registerCalculator(this);
        } else {
            DiscordSRV.debug("Not using LuckPerms' contexts because they are disabled in the config");
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
            luckPerms.getContextManager().unregisterCalculator(this);
        }
    }

    @Override
    public void calculate(@NonNull Player target, net.luckperms.api.context.ContextConsumer consumer) {
        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(target.getUniqueId());
        consumer.accept(CONTEXT_LINKED, Boolean.toString(userId != null));

        if (userId == null) {
            return;
        }

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
        if (mainGuild == null) {
            return;
        }

        Member member = mainGuild.getMemberById(userId);
        if (member == null) {
            return;
        }

        consumer.accept(CONTEXT_BOOSTING, Boolean.toString(member.getTimeBoosted() != null));

        for (Role role : member.getRoles()) {
            consumer.accept(CONTEXT_ROLE, role.getName());
        }
    }

    @Override
    public net.luckperms.api.context.ContextSet estimatePotentialContexts() {
        net.luckperms.api.context.ImmutableContextSet.Builder builder = net.luckperms.api.context.ImmutableContextSet.builder();

        builder.add(CONTEXT_LINKED, "true");
        builder.add(CONTEXT_LINKED, "false");

        builder.add(CONTEXT_BOOSTING, "true");
        builder.add(CONTEXT_BOOSTING, "false");

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
        if (mainGuild != null) {
            for (Role role : mainGuild.getRoles()) {
                builder.add(CONTEXT_ROLE, role.getName());
            }
        }

        return builder.build();
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("LuckPerms");
    }

}
