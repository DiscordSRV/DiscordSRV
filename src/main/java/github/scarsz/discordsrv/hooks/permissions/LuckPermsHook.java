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
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

public class LuckPermsHook implements PluginHook, ContextCalculator<Player> {
    private static final String CONTEXT_LINKED = "discordsrv:linked";
    private static final String CONTEXT_BOOSTING = "discordsrv:boosting";
    private static final String CONTEXT_ROLE = "discordsrv:role";

    private final LuckPerms luckPerms;
    private final Set<EventSubscription<?>> subscriptions = new HashSet<>();

    public LuckPermsHook() {
        luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);

        // update events
        if (!DiscordSRV.config().getStringList("DisabledPluginHooks").contains("LuckPerms-GroupUpdates")) {
            subscriptions.add(luckPerms.getEventBus().subscribe(UserTrackEvent.class, event -> handle(event.getUser().getUniqueId())));
            subscriptions.add(luckPerms.getEventBus().subscribe(NodeAddEvent.class, event -> handle(event, event.getNode(), true)));
            subscriptions.add(luckPerms.getEventBus().subscribe(NodeRemoveEvent.class, event -> handle(event, event.getNode(), false)));
        }

        // contexts
        if (!DiscordSRV.config().getStringList("DisabledPluginHooks").contains("LuckPerms-Contexts")) {
            luckPerms.getContextManager().registerCalculator(this);
        }
    }

    private void handle(NodeMutateEvent event, Node node, boolean add) {
        if (event.isUser() && node.getType() == NodeType.INHERITANCE) {
            String groupName = NodeType.INHERITANCE.cast(node).getGroupName();
            UUID uuid = ((User) event.getTarget()).getUniqueId();
            Map<String, List<String>> justModified = DiscordSRV.getPlugin()
                    .getGroupSynchronizationManager().getJustModifiedGroups().getOrDefault(uuid, null);
            if (justModified != null && justModified.getOrDefault(add ? "add" : "remove", Collections.emptyList()).remove(groupName)) {
                return;
            }
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

    @Override
    public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
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
    public ContextSet estimatePotentialContexts() {
        ImmutableContextSet.Builder builder = ImmutableContextSet.builder();

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() instanceof DiscordSRV) {
            subscriptions.forEach(EventSubscription::close);
            luckPerms.getContextManager().unregisterCalculator(this);
        }
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("LuckPerms");
    }

}
