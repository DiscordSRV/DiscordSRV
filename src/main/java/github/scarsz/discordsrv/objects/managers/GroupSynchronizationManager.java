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

package github.scarsz.discordsrv.objects.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.GamePermissionUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GroupSynchronizationManager extends ListenerAdapter implements Listener {

    private final AtomicInteger synchronizationCount = new AtomicInteger(0);
    private final Map<Member, Map.Entry<Guild, Map<String, Set<Role>>>> justModifiedRoles = new HashMap<>();
    private final Map<UUID, Map<String, List<String>>> justModifiedGroups = new HashMap<>();

    public void resync() {
        resync(SyncDirection.AUTHORITATIVE);
    }

    public void resync(SyncDirection direction) {
        if (getPermissions() == null) return;

        Set<OfflinePlayer> players = new HashSet<>(PlayerUtil.getOnlinePlayers());

        // synchronize everyone in the connected discord servers
        DiscordUtil.getJda().getGuilds().stream()
                .flatMap(guild -> guild.getMembers().stream())
                .map(member -> DiscordSRV.getPlugin().getAccountLinkManager().getUuid(member.getId()))
                .filter(Objects::nonNull)
                .map(Bukkit::getOfflinePlayer)
                .forEach(players::add);

        players.forEach(player -> resync(player, direction));
    }

    public void resync(User user) {
        resync(user, SyncDirection.AUTHORITATIVE);
    }

    public void resync(User user, SyncDirection direction) {
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(user.getId());
        if (uuid == null) {
            DiscordSRV.debug("Tried to sync groups for " + user + " but their Discord account is not linked to a MC account");
            return;
        }

        resync(Bukkit.getOfflinePlayer(uuid), direction);
    }

    public void resync(OfflinePlayer player) {
        resync(player, SyncDirection.AUTHORITATIVE);
    }

    public void resync(OfflinePlayer player, SyncDirection direction) {
        resync(player, direction, false);
    }

    public void resync(OfflinePlayer player, SyncDirection direction, boolean addLinkedRole) {
        if (player == null) return;
        if (getPermissions() == null) {
            DiscordSRV.debug("Can't synchronize groups/roles for " + player.getName() + ", permissions provider is null");
            return;
        }

        if (DiscordSRV.getPlugin().getAccountLinkManager() == null) {
            DiscordSRV.debug("Tried to sync groups for player " + player.getName() + " but the AccountLinkManager wasn't initialized yet");
            return;
        }

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) {
            DiscordSRV.debug("Tried to sync groups for player " + player.getName() + " but their MC account is not linked to a Discord account");
            return;
        }
        User user = DiscordUtil.getUserById(discordId);
        if (user == null) {
            DiscordSRV.debug("Tried to sync groups for player " + player.getName() + " but Discord user is not available");
            return;
        }

        int id = synchronizationCount.incrementAndGet();
        List<String> synchronizationSummary = new ArrayList<>();
        synchronizationSummary.add("Group synchronization (#" + id + ") " + direction + " for " + "{" + player.getName() + ":" + user + "}");
        List<String> bothSidesTrue = new ArrayList<>();
        List<String> bothSidesFalse = new ArrayList<>();

        Map<Guild, Map<String, Set<Role>>> roleChanges = new HashMap<>();

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

            Member member = role.getGuild().getMember(user);
            if (member == null) {
                // this is treated below as if they do not have the role
                synchronizationSummary.add("Tried to sync " + role + " but could not find " + user + " in the role's Discord server, treating it as if they don't have the role");

//                getPermissions().playerRemoveGroup(null, player, groupName);
//                continue;
            }

            try {
                String[] groups = getPermissions().getPlayerGroups(null, player);
                if (groups == null) {
                    synchronizationSummary.add("Tried to sync {" + role + ":" + groupName + "} but Vault returned null as the player's groups (Player is " + (player.isOnline() ? "online" : "offline") + ")");
                    continue;
                }
            } catch (Throwable t) {
                synchronizationSummary.add("Tried to sync {" + role + ":" + groupName + "} but the player's groups couldn't be retrieved from Vault due to exception: " + ExceptionUtils.getMessage(t));
                continue;
            }

            boolean hasGroup = DiscordSRV.config().getBoolean("GroupRoleSynchronizationPrimaryGroupOnly")
                    ? groupName.equalsIgnoreCase(getPermissions().getPrimaryGroup(null, player))
                    : getPermissions().playerInGroup(null, player, groupName);
            if (getPermissions().playerHas(null, player, "discordsrv.sync." + groupName)) hasGroup = true;
            if (DiscordSRV.config().getBoolean("GroupRoleSynchronizationEnableDenyPermission") &&
                    getPermissions().playerHas(null, player, "discordsrv.sync.deny." + groupName)) {
                hasGroup = false;
                synchronizationSummary.add(player.getName() + " doesn't have group " + groupName + " due to having the deny permission for it");
            }

            boolean hasRole = member != null && member.getRoles().contains(role);
            boolean roleIsManaged = role.isManaged();
            // Managed roles cannot be given or taken, so it will be Discord -> Minecraft only
            boolean minecraftIsAuthoritative = !roleIsManaged && direction == SyncDirection.AUTHORITATIVE
                    ? DiscordSRV.config().getBoolean("GroupRoleSynchronizationMinecraftIsAuthoritative")
                    : direction == SyncDirection.TO_DISCORD;

            if (hasGroup == hasRole) {
                // both sides agree, no changes necessary
                (hasGroup ? bothSidesTrue : bothSidesFalse).add("{" + groupName + ":" + role + "}" + (roleIsManaged ? " (Managed Role)" : ""));
            } else if (!hasGroup) { // !hasGroup && hasRole
                if (minecraftIsAuthoritative) {
                    roleChanges.computeIfAbsent(role.getGuild(), guild -> new HashMap<>())
                            .computeIfAbsent("remove", s -> new HashSet<>())
                            .add(role);
                    synchronizationSummary.add("Player " + player.getName() + "'s Vault groups: " + Arrays.toString(getPermissions().getPlayerGroups(null, player))
                            + " (Player is " + (player.isOnline() ? "online" : "offline") + ")");
                    synchronizationSummary.add("{" + groupName + ":" + role + "} removes Discord role");
                } else {
                    boolean luckPerms = PluginUtil.pluginHookIsEnabled("LuckPerms");
                    List<String> additions = justModifiedGroups.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).computeIfAbsent("add", key -> new ArrayList<>());
                    Runnable runnable = () -> {
                        String[] groups = getPermissions().getGroups();
                        if (ArrayUtils.contains(groups, groupName)) {
                            if (!getPermissions().playerAddGroup(null, player, groupName)) {
                                DiscordSRV.debug("Synchronization #" + id + " for {" + player.getName() + ":" + user + "} failed: adding group " + groupName + ", returned a failure");
                                additions.remove(groupName);
                            }
                        } else {
                            DiscordSRV.debug("Synchronization #" + id + " for {" + player.getName() + ":" + user + "} failed: group " + groupName + " doesn't exist (Server's Groups: " + Arrays.toString(groups) + ")");
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
                    roleChanges.computeIfAbsent(role.getGuild(), guild -> new HashMap<>())
                            .computeIfAbsent("add", s -> new HashSet<>())
                            .add(role);
                    synchronizationSummary.add("{" + groupName + ":" + role + "} adds Discord role");
                } else {
                    boolean luckPerms = PluginUtil.pluginHookIsEnabled("LuckPerms");
                    List<String> removals = justModifiedGroups.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).computeIfAbsent("remove", key -> new ArrayList<>());
                    Runnable runnable = () -> {
                        if (!getPermissions().playerRemoveGroup(null, player, groupName)) {
                            DiscordSRV.debug("Synchronization #" + id + " for {" + player.getName() + ":" + user + "} failed: removing group " + groupName + " returned a failure");
                            removals.add(groupName);
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

        if (!bothSidesTrue.isEmpty()) synchronizationSummary.add("No changes for (Both sides true): " + String.join(" | ", bothSidesTrue));
        if (!bothSidesFalse.isEmpty()) synchronizationSummary.add("No changes for (Both sides false): " + String.join(" | ", bothSidesFalse));

        if (addLinkedRole) {
            try {
                Role role = DiscordUtil.getJda().getRolesByName(DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"), true).stream().findFirst().orElse(null);
                if (role != null) {
                    roleChanges.computeIfAbsent(role.getGuild(), guild -> new HashMap<>())
                            .computeIfAbsent("add", s -> new HashSet<>())
                            .add(role);
                } else {
                    DiscordSRV.debug("Couldn't add user to null (\"linked\") role to " + player.getName());
                }
            } catch (Throwable t) {
                DiscordSRV.debug("Couldn't add \"linked\" role to " + player.getName() + " due to exception: " + ExceptionUtils.getMessage(t));
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

            if (!guild.getSelfMember().canInteract(member)) {
                synchronizationSummary.add("Synchronization failed for " + member + ": can't interact with member");
                continue;
            }

            if (!guild.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_ROLES)) {
                synchronizationSummary.add("Synchronization failed for " + member + ": bot doesn't have MANAGE_ROLES permission");
                continue;
            }

            Iterator<Role> addIterator = add.iterator();
            while (addIterator.hasNext()) {
                Role role = addIterator.next();
                if (!guild.getSelfMember().canInteract(role)) {
                    synchronizationSummary.add("Synchronization for role " + role + " (add) in " + guild + " failed: can't interact with role");
                    addIterator.remove();
                }
            }

            Iterator<Role> removeIterator = add.iterator();
            while (removeIterator.hasNext()) {
                Role role = removeIterator.next();
                if (!guild.getSelfMember().canInteract(role)) {
                    synchronizationSummary.add("Synchronization for role " + role + " (remove) in " + guild + " failed: can't interact with role");
                    removeIterator.remove();
                }
            }

            guild.modifyMemberRoles(member, add, remove).reason("DiscordSRV synchronization").queue(
                    v -> DiscordSRV.debug("Synchronization #" + id + " for {" + player.getName() + ":" + member + "} successful in " + guild + ": {add=" + add + ", remove=" + remove + "}"),
                    t -> DiscordSRV.debug("Synchronization #" + id + " for {" + player.getName() + ":" + member + "} failed in " + guild + ": " + ExceptionUtils.getStackTrace(t)));
            justModifiedRoles.put(member, guildEntry);
        }

        DiscordSRV.debug(synchronizationSummary);
    }

    public void resyncEveryone() {
        resyncEveryone(SyncDirection.AUTHORITATIVE);
    }
    public void resyncEveryone(SyncDirection direction) {
        Set<OfflinePlayer> players = new HashSet<>();

        // synchronize everyone with a linked account that's played on the server
        DiscordSRV.getPlugin().getAccountLinkManager().getManyDiscordIds(Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getUniqueId)
                .collect(Collectors.toSet())
        ).keySet().stream()
                .map(Bukkit.getServer()::getOfflinePlayer)
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
                .forEach(players::add);

        players.forEach(player -> resync(player, direction));
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
                Role role = StringUtils.isNotBlank(linkRole) ? DiscordUtil.getJda().getRolesByName(linkRole, true).stream().findFirst().orElse(null) : null;
                if (role != null) {
                    roles.computeIfAbsent(role.getGuild(), guild -> new HashSet<>()).add(role);
                } else {
                    DiscordSRV.debug("Couldn't remove user from null \"linked\" role");
                }
            } catch (Throwable t) {
                DiscordSRV.debug("Failed to remove \"linked\" role from " + player.getName() + " during unlink: " + ExceptionUtils.getMessage(t));
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

        resync(member.getUser(), SyncDirection.TO_MINECRAFT);
    }

    // Capturing group for username or uuid
    private final String userRegex = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|[a-zA-Z0-9_]{1,16})";
    private final List<Pattern> patterns = Arrays.asList(
            // GroupManager
            Pattern.compile("/?manu(?:add(?:sub)?|del(?:sub)?|promote|demote) " + userRegex + ".*", Pattern.CASE_INSENSITIVE),
            // PermissionsEx
            Pattern.compile("/?pex user " + userRegex + " group(?: timed)? (?:add)|(?:set)|(?:remove) .*", Pattern.CASE_INSENSITIVE),
            // zPermissions
            Pattern.compile("/?permissions player " + userRegex + " (?:(?:setgroup)|(?:addgroup)|(?:removegroup)).*"),
            Pattern.compile("/?(?:un)?setrank " + userRegex + ".*"),
            // PermissionsEx + zPermissions
            Pattern.compile("/?(?:pex )?(?:promote|demote) " + userRegex + ".*", Pattern.CASE_INSENSITIVE)
    );

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        checkCommand(event.getCommand());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRemoteServerCommand(RemoteServerCommandEvent event) {
        checkCommand(event.getCommand());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!GamePermissionUtil.hasPermission(event.getPlayer(), "discordsrv.groupsyncwithcommands")) {
            return;
        }
        checkCommand(event.getMessage());
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
                () -> resync(target, SyncDirection.TO_DISCORD),
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
                    DiscordSRV.debug("Can't access permissions: registration provider was null");
                    return null;
                }
                return permission = provider.getProvider();
            } catch (ClassNotFoundException e) {
                if (!warnedAboutMissingVault) {
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

}
