/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.*;
import java.util.stream.Collectors;

public class GroupSynchronizationUtil {

    static {
        int cycleTime = DiscordSRV.config().getInt("GroupRoleSynchronizationCycleTime") * 20 * 60;
        if (cycleTime < 20 * 60) cycleTime = 20 * 60;

        Bukkit.getScheduler().runTaskTimerAsynchronously(DiscordSRV.getPlugin(), () -> PlayerUtil.getOnlinePlayers(false).forEach(GroupSynchronizationUtil::reSyncGroups), 0, cycleTime);
    }

    public static void reSyncGroups(Player player) {
        reSyncGroups(player, false);
    }

    public static void reSyncGroups(Player player, boolean clearAssignedRoles) {
        if (player == null) return;

        DiscordSRV.debug("Synchronizing player " + player.getName());

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId == null) {
            DiscordSRV.debug("Tried to sync groups for player " + player.getName() + " but their MC account is not linked to a Discord account");
            return;
        }

        // get member
        Member member = DiscordUtil.getMemberById(discordId);

        if (member == null) {
            DiscordSRV.debug("Tried to sync groups for player " + player.getName() + " but their MC account is not linked to a Discord account");
            return;
        }

        if (member.isOwner()) {
            DiscordSRV.debug("Skipping member " + member.getEffectiveName() + "#" + member.getUser().getDiscriminator() + " because they're the owner and we can't touch them");
            return;
        }

        // get all the roles to synchronize from the config
        Map<Permission, Role> synchronizables = new HashMap<>();
        for (String roleId : DiscordSRV.config().getStringList("GroupRoleSynchronizationRoleIdsToSync")) {
            Role role = DiscordUtil.getRole(roleId);

            if (role == null && !roleId.equals("12345678901234567890") && !roleId.equals("DISCORDROLENAME")) {
                DiscordSRV.debug(LangUtil.InternalMessage.GROUP_SYNCHRONIZATION_COULD_NOT_FIND_ROLE.toString()
                        .replace("{rolename}", roleId)
                );
                continue;
            }

            Permission permission = new Permission("discordsrv.sync." + roleId, PermissionDefault.FALSE);

            synchronizables.put(permission, role);
        }
        if (synchronizables.size() == 0) {
            DiscordSRV.debug("Tried to sync groups but no synchronizables existed");
            return;
        }

        Set<Role> rolesToAdd = new HashSet<>();
        Set<Role> rolesToRemove = new HashSet<>();

        for (Map.Entry<Permission, Role> pair : synchronizables.entrySet()) {
            if (!clearAssignedRoles && player.hasPermission(pair.getKey())) {
                rolesToAdd.add(pair.getValue());
            } else {
                rolesToRemove.add(pair.getValue());
            }
        }

        // remove roles that the user already has from roles to add
        rolesToAdd.removeAll(member.getRoles());
        // remove roles that the user doesn't already have from roles to remove
        rolesToRemove.removeIf(role -> !member.getRoles().contains(role));

        List<String> changes = new ArrayList<>();

        DiscordUtil.modifyRolesOfMember(member, rolesToAdd, rolesToRemove);
        if (rolesToAdd.size() > 0) changes.add("+ " + String.join("", rolesToAdd.stream().map(Role::toString).collect(Collectors.toList())));
        if (rolesToRemove.size() > 0) changes.add("- " + String.join("", rolesToRemove.stream().map(Role::toString).collect(Collectors.toList())));
        DiscordSRV.debug("Synced player " + player.getName() + " (" + (changes.size() > 0 ? String.join(" | ", changes) : "no changes") + ")");
    }

}
