package github.scarsz.discordsrv.objects.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import javax.annotation.Nonnull;
import java.util.*;

public class GroupSynchronizationManager extends ListenerAdapter implements Listener {

    private Map<Member, Map.Entry<Guild, Map<String, Set<Role>>>> justModified = new HashMap<>();

    public void reSyncGroups() {
        if (getPermissions() == null) return;

        Set<OfflinePlayer> players = new HashSet<>(PlayerUtil.getOnlinePlayers());

        // synchronize everyone in the connected discord servers
        DiscordUtil.getJda().getGuilds().stream()
                .flatMap(guild -> guild.getMembers().stream())
                .map(member -> DiscordSRV.getPlugin().getAccountLinkManager().getUuid(member.getId()))
                .filter(Objects::nonNull)
                .map(Bukkit::getOfflinePlayer)
                .forEach(players::add);

        players.forEach(this::reSyncGroups);
    }

    public void reSyncGroups(SyncDirection direction) {
        if (getPermissions() == null) return;

        Set<OfflinePlayer> players = new HashSet<>(PlayerUtil.getOnlinePlayers());

        // synchronize everyone in the connected discord servers
        DiscordUtil.getJda().getGuilds().stream()
                .flatMap(guild -> guild.getMembers().stream())
                .map(member -> DiscordSRV.getPlugin().getAccountLinkManager().getUuid(member.getId()))
                .filter(Objects::nonNull)
                .map(Bukkit::getOfflinePlayer)
                .forEach(players::add);

        players.forEach(player -> reSyncGroups(player, direction));
    }

    public void reSyncGroups(User user) {
        reSyncGroups(user, SyncDirection.AUTHORITATIVE);
    }

    public void reSyncGroups(User user, SyncDirection direction) {
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(user.getId());
        if (uuid == null) {
            DiscordSRV.debug("Tried to sync groups for " + user + " but their Discord account is not linked to a MC account");
            return;
        }

        reSyncGroups(Bukkit.getOfflinePlayer(uuid), direction);
    }

    public void reSyncGroups(OfflinePlayer player) {
        reSyncGroups(player, SyncDirection.AUTHORITATIVE);
    }

    public void reSyncGroups(OfflinePlayer player, SyncDirection direction) {
        if (player == null) return;
        if (getPermissions() == null) {
            DiscordSRV.error("Can't synchronize groups/roles for " + player.getName() + ", permissions provider is null");
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

        Map<Guild, Map<String, Set<Role>>> roleChanges = new HashMap<>();

        for (Map.Entry<String, String> entry : getSynchronizables().entrySet()) {
            String groupName = entry.getKey();
            String roleId = entry.getValue();

            if (StringUtils.isBlank(groupName)) continue;
            if (StringUtils.isBlank(roleId.replace("0", ""))) continue;

            Role role = DiscordUtil.getRole(roleId);
            if (role == null) {
                DiscordSRV.debug("Tried to sync role " + roleId + " but could not find role");
                continue;
            }

            Member member = role.getGuild().getMember(user);
            if (member == null) {
                // this is treated below as if they do not have the role
                DiscordSRV.debug("Tried to sync " + role + " but could not find " + user + " in the role's Discord server, treating it as if they don't have the role");

//                getPermissions().playerRemoveGroup(null, player, groupName);
//                continue;
            }

            boolean hasGroup = getPermissions().playerInGroup(null, player, groupName);
            boolean hasRole = member != null && member.getRoles().contains(role);
            boolean minecraftIsAuthoritative = direction == SyncDirection.AUTHORITATIVE
                    ? DiscordSRV.config().getBoolean("GroupRoleSynchronizationMinecraftIsAuthoritative")
                    : direction == SyncDirection.TO_DISCORD;

            if (hasGroup == hasRole) {
                // both sides agree, no changes necessary
                DiscordSRV.debug("Synchronization on " + player.getName() + " for {" + groupName + ":" + role + "} produces no change");
            } else if (!hasGroup && hasRole) {
                if (minecraftIsAuthoritative) {
                    roleChanges.computeIfAbsent(role.getGuild(), guild -> new HashMap<>())
                            .computeIfAbsent("remove", s -> new HashSet<>())
                            .add(role);
                    DiscordSRV.debug("Synchronization on " + player.getName() + " for {" + groupName + ":" + role + "} removes Discord role");
                } else {
                    Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () ->
                            getPermissions().playerAddGroup(null, player, groupName));
                    DiscordSRV.debug("Synchronization on " + player.getName() + " for {" + groupName + ":" + role + "} adds Minecraft group");
                }
            } else if (hasGroup && !hasRole) {
                if (minecraftIsAuthoritative) {
                    roleChanges.computeIfAbsent(role.getGuild(), guild -> new HashMap<>())
                            .computeIfAbsent("add", s -> new HashSet<>())
                            .add(role);
                    DiscordSRV.debug("Synchronization on " + player.getName() + " for {" + groupName + ":" + role + "} adds Discord role");
                } else {
                    Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () ->
                            getPermissions().playerRemoveGroup(null, player, groupName));
                    DiscordSRV.debug("Synchronization on " + player.getName() + " for {" + groupName + ":" + role + "} removes Minecraft group");
                }
            }
        }

        for (Map.Entry<Guild, Map<String, Set<Role>>> guildEntry : roleChanges.entrySet()) {
            Guild guild = guildEntry.getKey();
            Member member = guild.getMember(user);
            Set<Role> add = guildEntry.getValue().getOrDefault("add", Collections.emptySet());
            Set<Role> remove = guildEntry.getValue().getOrDefault("remove", Collections.emptySet());

            if (member == null) {
                DiscordSRV.debug("Synchronization on " + player.getName() + " failed for " + guild + ": user is not a member");
                continue;
            }

            if (!guild.getSelfMember().canInteract(member)) {
                DiscordSRV.debug("Synchronization on " + member + " failed: can't interact with member");
                continue;
            }

            if (!guild.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_ROLES)) {
                DiscordSRV.debug("Synchronization on " + member + " failed: bot doesn't have MANAGE_ROLES permission");
                continue;
            }

            guild.modifyMemberRoles(member, add, remove).reason("DiscordSRV synchronization").queue(v -> {
                DiscordSRV.debug("Synchronization on " + member + " successful: {add=" + add + ", remove=" + remove + "}");
            }, t -> {
                DiscordSRV.debug("Synchronization on " + member + " failed: " + t.getMessage());
                t.printStackTrace();
            });
            justModified.put(member, guildEntry);
        }
    }

    public void removeSynchronizedRoles(OfflinePlayer player) {
        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        User user = DiscordUtil.getUserById(userId);
        if (user != null) {
            Map<Guild, Set<Role>> roles = new HashMap<>();
            getSynchronizables().values().stream()
                    .map(DiscordUtil::getRole)
                    .filter(Objects::nonNull)
                    .forEach(role -> roles.computeIfAbsent(role.getGuild(), guild -> new HashSet<>()).add(role));
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
        if (justModified.containsKey(member)) {
            Map.Entry<Guild, Map<String, Set<Role>>> entry = justModified.remove(member);
            if (entry.getKey().equals(member.getGuild())) {
                Set<Role> recentlyChanged = entry.getValue().getOrDefault(type, Collections.emptySet());
                if (recentlyChanged.size() == roles.size() && recentlyChanged.containsAll(roles)) {
                    // we just made this change, ignore it
                    return;
                }
            }
        }

        reSyncGroups(member.getUser(), SyncDirection.TO_MINECRAFT);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String[] split = event.getMessage().split(" ");
        OfflinePlayer target = null;

        if (!StringUtils.containsIgnoreCase(event.getMessage(), "promote")
                && !StringUtils.containsIgnoreCase(event.getMessage(), "demote")
                && !StringUtils.containsIgnoreCase(event.getMessage(), "parent")
                && !StringUtils.containsIgnoreCase(event.getMessage(), "group")) {
            return;
        }

        if (StringUtils.startsWithIgnoreCase(event.getMessage(), "/lp user ")
                && (StringUtils.containsIgnoreCase(event.getMessage(), " parent ")
                    || StringUtils.containsIgnoreCase(event.getMessage(), " promote ")
                    || StringUtils.containsIgnoreCase(event.getMessage(), " demote "))
                && split.length >= 3) {

            target = Bukkit.getOfflinePlayer(split[2]);
        }

        //TODO add more permission plugin commands to detect

        if (target == null) {
            target = Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(player -> ArrayUtils.contains(split, player.getName()))
                    .findFirst().orElse(null);
        }

        // run task later so that this command has time to execute & change the group state
        OfflinePlayer finalTarget = target;
        Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordSRV.getPlugin(),
                () -> reSyncGroups(finalTarget, SyncDirection.TO_DISCORD),
                5
        );
    }

    private Map<String, String> getSynchronizables() {
        HashMap<String, String> map = new HashMap<>();
        DiscordSRV.config().dget("GroupRoleSynchronizationGroupsAndRolesToSync").children().forEach(dynamic ->
                map.put(dynamic.key().convert().intoString(), dynamic.convert().intoString()));
        return map;
    }

    private Permission permission = null;
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
                DiscordSRV.error("Group synchronization failed: Vault isn't installed. It is required for synchronization to work.");
                return null;
            }
        }
    }

    public enum SyncDirection {

        TO_MINECRAFT,
        TO_DISCORD,
        AUTHORITATIVE

    }

}
