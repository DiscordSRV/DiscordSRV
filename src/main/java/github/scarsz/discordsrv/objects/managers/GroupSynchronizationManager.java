/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.objects.managers;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.GamePermissionUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GroupSynchronizationManager extends ListenerAdapter implements Listener {

    private final AtomicInteger synchronizationCount = new AtomicInteger(0);
    private final Map<Member, Map.Entry<Guild, Map<String, Set<Role>>>> justModifiedRoles = new HashMap<>();
    // expiring just incase, so it doesn't stick around (avoiding memory leaks)
    private final Map<UUID, Map<String, List<String>>> justModifiedGroups =
            new ExpiringDualHashBidiMap<>(TimeUnit.MINUTES.toMillis(1));
    private final Map<String, Set<String>> membersNotInGuilds = new ConcurrentHashMap<>();

    @Deprecated
    public void resync() {
        resync(SyncCause.LEGACY);
    }
    @Deprecated
    public void resync(SyncDirection direction) {
        resync(direction, SyncCause.LEGACY);
    }
    @Deprecated
    public void resync(User user) {
        resync(user, SyncCause.LEGACY);
    }
    @Deprecated
    public void resync(User user, SyncDirection direction) {
        resync(user, direction, SyncCause.LEGACY);
    }
    @Deprecated
    public void resync(OfflinePlayer player) {
        resync(player, SyncCause.LEGACY);
    }
    @Deprecated
    public void resync(OfflinePlayer player, SyncDirection direction) {
        resync(player, direction, SyncCause.LEGACY);
    }
    @Deprecated
    public void resync(OfflinePlayer player, SyncDirection direction, boolean addLinkedRole) {
        resync(player, direction, addLinkedRole, SyncCause.LEGACY);
    }
    @Deprecated
    public void resyncEveryone() {
        resyncEveryone(SyncCause.LEGACY);
    }
    @Deprecated
    public void resyncEveryone(SyncDirection direction) {
        resyncEveryone(direction, SyncCause.LEGACY);
    }

    public void resync(SyncCause cause) {
        resync(SyncDirection.AUTHORITATIVE, cause);
    }

    public void resync(SyncDirection direction, SyncCause cause) {
        if (getPermissions() == null) return;

        Set<OfflinePlayer> players = new HashSet<>(PlayerUtil.getOnlinePlayers());

        if (DiscordSRV.config().getBoolean("GroupRoleSynchronizationCycleCompletely")) {
            // synchronize everyone in the connected discord servers
            // otherwise, only online players are synchronized
            DiscordUtil.getJda().getGuilds().stream()
                    .flatMap(guild -> guild.getMembers().stream())
                    .map(member -> DiscordSRV.getPlugin().getAccountLinkManager().getUuid(member.getId()))
                    .filter(Objects::nonNull)
                    .map(Bukkit::getOfflinePlayer)
                    .forEach(players::add);
        }

        players.forEach(player -> resync(player, direction, cause));
    }

    public void resync(User user, SyncCause cause) {
        resync(user, SyncDirection.AUTHORITATIVE, cause);
    }

    public void resync(User user, SyncDirection direction, SyncCause cause) {
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(user.getId());
        if (uuid == null) {
            DiscordSRV.debug(Debug.GROUP_SYNC, "Tried to sync groups for " + user + " but their Discord account is not linked to a MC account");
            return;
        }

        resync(Bukkit.getOfflinePlayer(uuid), direction, cause);
    }

    public void resync(OfflinePlayer player, SyncCause cause) {
        resync(player, SyncDirection.AUTHORITATIVE, cause);
    }

    public void resync(OfflinePlayer player, SyncDirection direction, SyncCause cause) {
        resync(player, direction, false, cause);
    }

    public void resync(OfflinePlayer player, SyncDirection direction, boolean addLinkedRole, SyncCause cause) {
        if (player == null) return;
        if (getPermissions() == null) {
            DiscordSRV.debug(Debug.GROUP_SYNC, "Can't synchronize groups/roles for " + player.getName() + ", permissions provider is null");
            return;
        }

        if (Bukkit.isPrimaryThread()) throw new IllegalStateException("Resync cannot be run on the server main thread");

        if (DiscordSRV.getPlugin().getAccountLinkManager() == null) {
            DiscordSRV.debug(Debug.GROUP_SYNC, "Tried to sync groups for player " + player.getName() + " but the AccountLinkManager wasn't initialized yet");
            return;
        }

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) {
            DiscordSRV.debug(Debug.GROUP_SYNC, "Tried to sync groups for player " + player.getName() + " but their MC account is not linked to a Discord account. Synchronization cause: " + cause);
            return;
        }
        User user = DiscordUtil.getUserById(discordId);
        if (user == null) {
            DiscordSRV.debug(Debug.GROUP_SYNC, "Tried to sync groups for player " + player.getName() + " but Discord user is not available");
            return;
        }

        int id = synchronizationCount.incrementAndGet();
        boolean vaultGroupsLogged = false;
        List<String> synchronizationSummary = new ArrayList<>();
        synchronizationSummary.add("Group synchronization (#" + id + ") " + direction + " for " + "{" + player.getName() + ":" + user + "}. Synchronization cause: " + cause);
        List<String> bothSidesTrue = new ArrayList<>();
        List<String> bothSidesFalse = new ArrayList<>();
        List<String> groupsGrantedByPermission = new ArrayList<>();
        List<String> groupsDeniedByPermission = new ArrayList<>();

        Map<Guild, Map<String, Set<Role>>> roleChanges = new HashMap<>();

        // Check if Minecraft or Discord is strictly authoritative.
        boolean oneWaySynchronisation = DiscordSRV.config().getBoolean("GroupRoleSynchronizationOneWay");
        boolean minecraftIsStrictlyAuthoritative = oneWaySynchronisation && DiscordSRV.config().getBoolean("GroupRoleSynchronizationMinecraftIsAuthoritative");
        boolean discordIsStrictlyAuthoritative = oneWaySynchronisation && !DiscordSRV.config().getBoolean("GroupRoleSynchronizationMinecraftIsAuthoritative");
        if (oneWaySynchronisation) synchronizationSummary.add("Synchronisation is one way (" + (minecraftIsStrictlyAuthoritative ? "Minecraft -> Discord" : "Discord -> Minecraft") + ")");

        if ((minecraftIsStrictlyAuthoritative && direction == SyncDirection.TO_MINECRAFT) || (discordIsStrictlyAuthoritative && direction == SyncDirection.TO_DISCORD)) {
            DiscordSRV.debug(Debug.GROUP_SYNC, "Group synchronization (#" + id + ") " + direction + " cancelled because " + (minecraftIsStrictlyAuthoritative ? "Minecraft" : "Discord") + " is strictly authoritative");
            return;
        }

        for (Map.Entry<String, String> entry : DiscordSRV.getPlugin().getGroupSynchronizables().entrySet()) {
            String groupName = entry.getKey();
            String roleId = entry.getValue();

            if (StringUtils.isBlank(groupName)) continue;
            if (StringUtils.isBlank(roleId.replace("0", ""))) continue;

            Role role = DiscordUtil.getRole(roleId);
            if (role == null) {
                synchronizationSummary.add("Tried to sync role " + roleId + " but could not find role");
                continue;
            }

            if (role.isPublicRole()) {
                synchronizationSummary.add("Skipping role " + roleId + " because it's a Guild's public role");
                continue;
            }

            Guild guild = role.getGuild();

            // get the member, from cache if it's there otherwise from Discord
            Set<String> membersNotInGuild = membersNotInGuilds.computeIfAbsent(guild.getId(), key -> new HashSet<>());
            if (guild.getMember(user) != null) membersNotInGuild.remove(user.getId()); // is in cache, so is in the server too
            if (membersNotInGuild.contains(user.getId())) {
                synchronizationSummary.add("Tried to sync role " + role + " but the user wasn't a member in the guild the role is in (cached)");
                continue;
            }

            Member member;
            try {
                member = guild.retrieveMember(user, false).complete();
            } catch (ErrorResponseException e) {
                if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) {
                    membersNotInGuild.add(user.getId());
                    synchronizationSummary.add("Tried to sync role " + role + " but the user wasn't a member in the guild the role is in (Discord response)");
                    continue;
                }
                DiscordSRV.error(e);
                continue;
            }
            if (member == null) {
                // this is treated below as if they do not have the role
                synchronizationSummary.add("Tried to sync " + role + " but could not find " + user + " in the role's Discord server, treating it as if they don't have the role");
            }

            String[] playerGroups;
            try {
                playerGroups = getPermissions().getPlayerGroups(null, player);
                if (playerGroups == null) {
                    synchronizationSummary.add("Tried to sync {" + role + ":" + groupName + "} but Vault returned null as the player's groups (Player is " + (player.isOnline() ? "online" : "offline") + ")");
                    continue;
                }
            } catch (Throwable t) {
                vaultError("Could not get player's groups", t);
                continue;
            }

            boolean primaryGroupOnly = DiscordSRV.config().getBoolean("GroupRoleSynchronizationPrimaryGroupOnly");
            if (!vaultGroupsLogged) {
                synchronizationSummary.add("Player " + player.getName() + "'s " +
                        (primaryGroupOnly ? "Primary group: " + getPermissions().getPrimaryGroup(null, player) + ", " : "")
                        + "Vault groups: " + Arrays.toString(playerGroups) +
                        " (Player is " + (player.isOnline() ? "online" : "offline") + ")");
                vaultGroupsLogged = true;
            }

            boolean hasGroup;
            try {
                hasGroup = primaryGroupOnly
                        ? groupName.equalsIgnoreCase(getPermissions().getPrimaryGroup(null, player))
                        : getPermissions().playerInGroup(null, player, groupName);

                if (getPermissions().playerHas(null, player, "discordsrv.sync." + groupName)) {
                    hasGroup = true;
                    groupsGrantedByPermission.add(groupName);
                }
                if (DiscordSRV.config().getBoolean("GroupRoleSynchronizationEnableDenyPermission") &&
                        getPermissions().playerHas(null, player, "discordsrv.sync.deny." + groupName)) {
                    hasGroup = false;
                    groupsDeniedByPermission.add(groupName);
                }
            } catch (Throwable t) {
                vaultError("Could not check the player's groups/permissions", t);
                continue;
            }

            boolean hasRole = member != null && member.getRoles().contains(role);
            boolean roleIsManaged = role.isManaged();
            // Managed roles cannot be given or taken, so it will be Discord -> Minecraft only
            if (roleIsManaged && minecraftIsStrictlyAuthoritative) {
                synchronizationSummary.add("Tried to sync {" + role + ":" + groupName + "} to Discord but the role is managed and Minecraft is strictly authoritative");
                continue;
            }
            // Determine if Minecraft is authoritative in the synchronization.
            boolean minecraftIsAuthoritative = minecraftIsStrictlyAuthoritative
                || (!roleIsManaged
                    && !discordIsStrictlyAuthoritative
                    && (direction == SyncDirection.AUTHORITATIVE ? DiscordSRV.config().getBoolean("GroupRoleSynchronizationMinecraftIsAuthoritative") : direction == SyncDirection.TO_DISCORD));

            if (hasGroup == hasRole) {
                // both sides agree, no changes necessary
                (hasGroup ? bothSidesTrue : bothSidesFalse).add("{" + groupName + ":" + role + "}" + (roleIsManaged ? " (Managed Role)" : ""));
            } else if (!hasGroup) { // !hasGroup && hasRole
                if (minecraftIsAuthoritative) {
                    roleChanges.computeIfAbsent(role.getGuild(), g -> new HashMap<>())
                            .computeIfAbsent("remove", s -> new HashSet<>())
                            .add(role);
                    synchronizationSummary.add("{" + groupName + ":" + role + "} removes Discord role");
                } else {
                    boolean luckPerms = PluginUtil.pluginHookIsEnabled("LuckPerms");
                    List<String> additions = justModifiedGroups.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).computeIfAbsent("add", key -> new ArrayList<>());
                    Runnable runnable = () -> {
                        try {
                            String[] serverGroups = getPermissions().getGroups();
                            if (ArrayUtils.contains(serverGroups, groupName)) {
                                if (!getPermissions().playerAddGroup(null, player, groupName)) {
                                    DiscordSRV.debug(Debug.GROUP_SYNC, "Synchronization #" + id + " for {" + player.getName() + ":" + user + "} failed: adding group " + groupName + ", returned a failure");
                                    additions.remove(groupName);
                                }
                            } else {
                                DiscordSRV.debug(Debug.GROUP_SYNC, "Synchronization #" + id + " for {" + player.getName() + ":" + user + "} failed: group " + groupName + " doesn't exist (Server's Groups: " + Arrays.toString(serverGroups) + ")");
                            }
                        } catch (Throwable t) {
                            vaultError("Could not add a player to a group", t);
                        }
                    };
                    if (luckPerms) {
                        additions.add(groupName);
                        runnable.run();
                    } else {
                        Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), runnable);
                    }
                    synchronizationSummary.add("{" + groupName + ":" + role + "} adds Minecraft group" + (roleIsManaged ? " (Managed Role)" : ""));
                }
            } else { // hasGroup && !hasRole
                if (minecraftIsAuthoritative) {
                    roleChanges.computeIfAbsent(role.getGuild(), g -> new HashMap<>())
                            .computeIfAbsent("add", s -> new HashSet<>())
                            .add(role);
                    synchronizationSummary.add("{" + groupName + ":" + role + "} adds Discord role");
                } else {
                    boolean luckPerms = PluginUtil.pluginHookIsEnabled("LuckPerms");
                    List<String> removals = justModifiedGroups.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).computeIfAbsent("remove", key -> new ArrayList<>());
                    Runnable runnable = () -> {
                        try {
                            if (getPermissions().playerInGroup(null, player, groupName)) {
                                if (!getPermissions().playerRemoveGroup(null, player, groupName)) {
                                    DiscordSRV.debug(Debug.GROUP_SYNC, "Synchronization #" + id + " for {" + player.getName() + ":" + user + "} failed: removing group " + groupName + " returned a failure");
                                    removals.add(groupName);
                                }
                            } else {
                                DiscordSRV.debug(Debug.GROUP_SYNC, "Synchronization #" + id + " for {" + player.getName() + ":" + user + "} failed: player is not in group \"" + groupName + "\"");
                                removals.add(groupName);
                            }
                        } catch (Throwable t) {
                            vaultError("Could not remove a player from a group", t);
                        }
                    };
                    if (luckPerms) {
                        removals.add(groupName);
                        runnable.run();
                    } else {
                        Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), runnable);
                    }
                    synchronizationSummary.add("{" + groupName + ":" + role + "} removes Minecraft group" + (roleIsManaged ? " (Managed Role)" : ""));
                }
            }
        }

        if (!groupsGrantedByPermission.isEmpty()) {
            synchronizationSummary.add("The following groups were granted due to having the " +
                    "discordsrv.sync.<group name> permission(s): " + String.join(" | ", groupsGrantedByPermission));
        }
        if (!groupsDeniedByPermission.isEmpty()) {
            synchronizationSummary.add("The player does not have the following groups due to having the " +
                    "discordsrv.sync.deny.<group name> permission(s): " + String.join(" | ", groupsDeniedByPermission));
        }
        if (!bothSidesTrue.isEmpty()) synchronizationSummary.add("No changes for (Both sides true): " + String.join(" | ", bothSidesTrue));
        if (!bothSidesFalse.isEmpty()) synchronizationSummary.add("No changes for (Both sides false): " + String.join(" | ", bothSidesFalse));

        if (addLinkedRole) {
            try {
                Role role = DiscordUtil.resolveRole(DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
                if (role != null) {
                    roleChanges.computeIfAbsent(role.getGuild(), guild -> new HashMap<>())
                            .computeIfAbsent("add", s -> new HashSet<>())
                            .add(role);
                } else {
                    DiscordSRV.debug(Debug.GROUP_SYNC, "Couldn't add user to null (\"linked\") role to " + player.getName());
                }
            } catch (Throwable t) {
                DiscordSRV.debug(Debug.GROUP_SYNC, "Couldn't add \"linked\" role to " + player.getName() + " due to exception: " + ExceptionUtils.getMessage(t));
            }
        }

        for (Map.Entry<Guild, Map<String, Set<Role>>> guildEntry : roleChanges.entrySet()) {
            Guild guild = guildEntry.getKey();
            Member member = guild.getMember(user);
            Set<Role> add = guildEntry.getValue().getOrDefault("add", Collections.emptySet());
            Set<Role> remove = guildEntry.getValue().getOrDefault("remove", Collections.emptySet());

            if (member == null) {
                synchronizationSummary.add("Synchronization failed for " + user + " in " + guild + ": user is not a member");
                continue;
            }

            Member selfMember = guild.getSelfMember();
            if (!selfMember.canInteract(member)) {
                synchronizationSummary.add("Synchronization failed for " + member + ": can't interact with member" +
                        (member.isOwner()
                                ? " (server owner)"
                                : !member.getRoles().isEmpty()
                                    ? !selfMember.getRoles().isEmpty()
                                        ? selfMember.getRoles().get(0).getPosition() <= member.getRoles().get(0).getPosition()
                                            ? " (member has a higher or equal role: " + member.getRoles().get(0) + " (" + member.getRoles().get(0).getPosition() + "))"
                                            : " (bot has a higher role????? bot: " + selfMember.getRoles().get(0) + ", member: " + member.getRoles().get(0) + ")"
                                        : " (bot has 0 roles)"
                                    : " (bot & member both have 0 roles)"
                        )
                );
                synchronizationSummary.add("Bot's top role in " + guild + ": " +
                        (selfMember.getRoles().isEmpty()
                                ? "bot has no roles"
                                : selfMember.getRoles().get(0) + " (" + selfMember.getRoles().get(0).getPosition() + ")"
                        )
                );
                continue;
            }

            if (!selfMember.hasPermission(net.dv8tion.jda.api.Permission.MANAGE_ROLES)) {
                synchronizationSummary.add("Synchronization failed for " + member + ": bot doesn't have MANAGE_ROLES permission");
                continue;
            }

            boolean anyInteractFail = false;

            Iterator<Role> addIterator = add.iterator();
            while (addIterator.hasNext()) {
                Role role = addIterator.next();
                if (!selfMember.canInteract(role)) {
                    synchronizationSummary.add("Synchronization for role " + role + " (add) in " + guild + " failed: can't interact with role (" + role.getPosition() + ")");
                    addIterator.remove();
                    anyInteractFail = true;
                }
            }

            Iterator<Role> removeIterator = add.iterator();
            while (removeIterator.hasNext()) {
                Role role = removeIterator.next();
                if (!selfMember.canInteract(role)) {
                    synchronizationSummary.add("Synchronization for role " + role + " (remove) in " + guild + " failed: can't interact with role (" + role.getPosition() + ")");
                    removeIterator.remove();
                    anyInteractFail = true;
                }
            }

            if (anyInteractFail) {
                synchronizationSummary.add("Bot's top role in " + guild + ": " +
                        (selfMember.getRoles().isEmpty()
                                ? "bot has no roles"
                                : selfMember.getRoles().get(0) + " (" + selfMember.getRoles().get(0).getPosition() + ")"
                        )
                );
            }

            guild.modifyMemberRoles(member, add, remove).reason("DiscordSRV synchronization").queue(
                    v -> DiscordSRV.debug(Debug.GROUP_SYNC, "Synchronization #" + id + " for {" + player.getName() + ":" + member + "} successful in " + guild + ": {add=" + add + ", remove=" + remove + "}"),
                    t -> DiscordSRV.debug(Debug.GROUP_SYNC, "Synchronization #" + id + " for {" + player.getName() + ":" + member + "} failed in " + guild + ": " + ExceptionUtils.getStackTrace(t)));
            justModifiedRoles.put(member, guildEntry);
        }

        DiscordSRV.debug(Debug.GROUP_SYNC, synchronizationSummary);
    }

    private void vaultError(String problem, Throwable throwable) {
        String name;
        Permission permission = getPermissions();
        try {
            name = permission.getName();
        } catch (Throwable t) {
            name = permission.getClass().getName();
        }
        DiscordSRV.error(problem + ". Caused by a error in Vault or it's permissions provider: " + name, throwable);
    }

    public void resyncEveryone(SyncCause cause) {
        resyncEveryone(SyncDirection.AUTHORITATIVE, cause);
    }
    @SuppressWarnings("ConstantConditions") // I'm tired of hearing this
    public void resyncEveryone(SyncDirection direction, SyncCause cause) {
        Set<OfflinePlayer> players = new HashSet<>();

        // synchronize everyone with a linked account that's played on the server
        DiscordSRV.getPlugin().getAccountLinkManager().getManyDiscordIds(Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getUniqueId)
                .collect(Collectors.toSet())
        ).keySet().stream()
                .map(Bukkit::getOfflinePlayer)
                .filter(Objects::nonNull)
                .forEach(players::add);

        // synchronize everyone with a linked account in the connected discord servers
        Map<String, UUID> linkedDiscords = DiscordSRV.getPlugin().getAccountLinkManager().getManyUuids(
                DiscordUtil.getJda().getGuilds().stream()
                        .flatMap(guild -> guild.getMembers().stream())
                        .map(ISnowflake::getId)
                        .collect(Collectors.toSet())
        );
        DiscordUtil.getJda().getGuilds().stream()
                .flatMap(guild -> guild.getMembers().stream())
                .filter(member -> linkedDiscords.containsKey(member.getId()))
                .map(member -> linkedDiscords.get(member.getId()))
                .filter(Objects::nonNull)
                .map(Bukkit::getOfflinePlayer)
                .filter(Objects::nonNull)
                .forEach(players::add);

        players.forEach(player -> resync(player, direction, cause));
    }

    public void removeSynchronizables(OfflinePlayer player) {
        if (!DiscordSRV.config().getBoolean("GroupRoleSynchronizationMinecraftIsAuthoritative")) {
            // Discord is authoritative, remove minecraft groups
            Permission permission = getPermissions();

            List<String> fail = new ArrayList<>();
            List<String> success = new ArrayList<>();
            for (String group : DiscordSRV.getPlugin().getGroupSynchronizables().keySet()) {
                if (permission.playerInGroup(null, player, group)) {
                    if (permission.playerRemoveGroup(null, player, group)) {
                        success.add(group);
                    } else {
                        fail.add(group);
                    }
                }
            }
            DiscordSRV.debug(Debug.GROUP_SYNC, player.getName() + " removed from their groups (Discord is authoritative). succeeded: " + success + ", failed: " + fail);
        } else {
            removeSynchronizedRoles(player);
        }
    }

    public void removeSynchronizedRoles(OfflinePlayer player) {
        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        User user = DiscordUtil.getUserById(userId);
        if (user != null) {
            Map<Guild, Set<Role>> roles = new HashMap<>();
            DiscordSRV.getPlugin().getGroupSynchronizables().values().stream()
                    .map(DiscordUtil::getRole)
                    .filter(Objects::nonNull)
                    .forEach(role -> roles.computeIfAbsent(role.getGuild(), guild -> new HashSet<>()).add(role));

            try {
                // remove user from linked role
                String linkRole = DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo");
                Role role = StringUtils.isNotBlank(linkRole) ? DiscordUtil.resolveRole(linkRole) : null;
                if (role != null) {
                    roles.computeIfAbsent(role.getGuild(), guild -> new HashSet<>()).add(role);
                } else {
                    DiscordSRV.debug(Debug.GROUP_SYNC, "Couldn't remove user from null \"linked\" role");
                }
            } catch (Throwable t) {
                DiscordSRV.debug(Debug.GROUP_SYNC, "Failed to remove \"linked\" role from " + player.getName() + " during unlink: " + ExceptionUtils.getMessage(t));
            }

            for (Map.Entry<Guild, Set<Role>> entry : roles.entrySet()) {
                Guild guild = entry.getKey();
                Member member = guild.getMember(user);
                if (member != null) {
                    if (guild.getSelfMember().canInteract(member)) {
                        DiscordUtil.removeRolesFromMember(member, entry.getValue());
                    }
                }
            }
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        membersNotInGuilds.computeIfAbsent(event.getGuild().getId(), key -> new HashSet<>()).remove(event.getMember().getId());
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        membersNotInGuilds.computeIfAbsent(event.getGuild().getId(), key -> new HashSet<>()).add(event.getUser().getId());
    }

    @Override
    public void onGuildMemberRoleAdd(@Nonnull GuildMemberRoleAddEvent event) {
        onGuildMemberRolesChanged("add", event.getMember(), event.getRoles());
    }
    @Override
    public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent event) {
        onGuildMemberRolesChanged("remove", event.getMember(), event.getRoles());
    }
    private void onGuildMemberRolesChanged(String type, Member member, List<Role> roles) {
        if (!DiscordSRV.getPlugin().isGroupRoleSynchronizationEnabled()) return;
        List<Role> checkRoles = new ArrayList<>(roles);
        Collection<String> validRoleIds = DiscordSRV.getPlugin().getGroupSynchronizables().values();
        checkRoles.removeIf(role -> !validRoleIds.contains(role.getId()));
        if (checkRoles.isEmpty()) return; // none of the changed roles were ones that would be synchronized
        if (justModifiedRoles.containsKey(member)) {
            Map.Entry<Guild, Map<String, Set<Role>>> entry = justModifiedRoles.remove(member);
            if (entry.getKey().equals(member.getGuild())) {
                Set<Role> recentlyChanged = entry.getValue().getOrDefault(type, Collections.emptySet());
                if (recentlyChanged.size() == roles.size() && recentlyChanged.containsAll(roles)) {
                    // we just made this change, ignore it
                    return;
                }
            }
        }

        resync(member.getUser(), SyncDirection.TO_MINECRAFT, SyncCause.DISCORD_ROLE_EDIT);
    }

    // Capturing group for username or uuid
    private final String userRegex = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|[a-zA-Z0-9_]{1,16})";
    private final List<Pattern> patterns = Arrays.asList(
            // GroupManager
            Pattern.compile("/?manu(?:add(?:sub)?|del(?:sub)?|promote|demote) " + userRegex + ".*", Pattern.CASE_INSENSITIVE),
            // PermissionsEx
            Pattern.compile("/?pex user " + userRegex + " group(?: timed)? (?:add)|(?:set)|(?:remove) .*", Pattern.CASE_INSENSITIVE),
            // zPermissions
            Pattern.compile("/?permissions player " + userRegex + " (?:(?:setgroup)|(?:addgroup)|(?:removegroup)).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/?(?:un)?setrank " + userRegex + ".*", Pattern.CASE_INSENSITIVE),
            // PermissionsEx + zPermissions
            Pattern.compile("/?(?:pex )?(?:promote|demote) " + userRegex + ".*", Pattern.CASE_INSENSITIVE)
    );

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> checkCommand(event.getCommand()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRemoteServerCommand(RemoteServerCommandEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> checkCommand(event.getCommand()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.groupsyncwithcommands")) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> checkCommand(event.getMessage()));
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"}) // 2013 Bukkit
    private void checkCommand(String message) {
        if (!DiscordSRV.getPlugin().isGroupRoleSynchronizationEnabled()) return;

        OfflinePlayer target = patterns.stream()
                .map(pattern -> pattern.matcher(message))
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .filter(Objects::nonNull)
                .map(input -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(input);
                    if (offlinePlayer != null) return offlinePlayer;

                    try {
                        return Bukkit.getOfflinePlayer(UUID.fromString(input));
                    } catch (IllegalArgumentException ignored) {}
                    return null;
                })
                .findAny().orElse(null);

        // run task later so that this command has time to execute & change the group state
        Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordSRV.getPlugin(),
                () -> resync(target, SyncDirection.TO_DISCORD, SyncCause.MINECRAFT_GROUP_EDIT_COMMAND),
                5
        );
    }

    public Map<UUID, Map<String, List<String>>> getJustModifiedGroups() {
        return justModifiedGroups;
    }

    private Permission permission = null;
    private boolean warnedAboutMissingVault = false;
    public Permission getPermissions() {
        if (permission != null) {
            return permission;
        } else {
            try {
                Class.forName("net.milkbowl.vault.permission.Permission");
                RegisteredServiceProvider<Permission> provider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
                if (provider == null) {
                    DiscordSRV.debug(Debug.GROUP_SYNC, "Can't access permissions: registration provider was null");
                    return null;
                }
                return permission = provider.getProvider();
            } catch (ClassNotFoundException e) {
                if (!warnedAboutMissingVault && DiscordSRV.getPlugin().isGroupRoleSynchronizationEnabled(false)) {
                    DiscordSRV.error("Group synchronization failed: Vault classes couldn't be found (did it enable properly?). Vault is required for synchronization to work.");
                    warnedAboutMissingVault = true;
                }
                return null;
            }
        }
    }

    public enum SyncDirection {

        TO_MINECRAFT("to Minecraft"),
        TO_DISCORD("to Discord"),
        AUTHORITATIVE("on authority");

        private final String description;

        SyncDirection(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }

    }

    public enum SyncCause {

        DISCORD_ROLE_EDIT("Discord roles changed"),
        MINECRAFT_GROUP_EDIT_API("Minecraft group change (via api)"),
        MINECRAFT_GROUP_EDIT_COMMAND("Minecraft group change (via command)"),
        PLAYER_LINK("Player linked"),
        PLAYER_JOIN("Player joined"),
        TIMER("Timer"),
        MANUAL("Manually triggered via resync command"),
        LEGACY("Legacy (deprecated method)");

        private final String description;

        SyncCause(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

}
