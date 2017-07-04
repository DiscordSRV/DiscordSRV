package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/5/2017
 * @at 2:38 PM
 */
public class GroupSynchronizationUtil {

    static {
        int cycleTime = DiscordSRV.getPlugin().getConfig().getInt("GroupRoleSynchronizationCycleTime") * 20 * 60;
        if (cycleTime < 20 * 60) cycleTime = 20 * 60;

        Bukkit.getScheduler().runTaskTimerAsynchronously(DiscordSRV.getPlugin(), () -> PlayerUtil.getOnlinePlayers(false).forEach(GroupSynchronizationUtil::reSyncGroups), 0, cycleTime);
    }

    public static void reSyncGroups(Player player) {
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

        // get all the roles to synchronize from the config
        Map<String, Role> synchronizables = new HashMap<>();
        for (String roleId : DiscordSRV.getPlugin().getConfig().getStringList("GroupRoleSynchronizationRoleIdsToSync")) {
            Role role = DiscordUtil.getRole(roleId);

            if (role == null && !roleId.equals("12345678901234567890") && !roleId.equals("DISCORDROLENAME")) {
                DiscordSRV.debug(LangUtil.InternalMessage.GROUP_SYNCHRONIZATION_COULD_NOT_FIND_ROLE.toString()
                    .replace("{rolename}", roleId)
                );
                continue;
            }

            synchronizables.put("discordsrv.sync." + roleId, role);
        }
        if (synchronizables.size() == 0) {
            DiscordSRV.debug("Tried to sync groups but no synchronizables existed");
            return;
        }

        List<Role> rolesToAdd = new ArrayList<>();
        List<Role> rolesToRemove = new ArrayList<>();

        for (Map.Entry<String, Role> pair : synchronizables.entrySet()) {
            if (player.hasPermission(pair.getKey())) {
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

        if (rolesToAdd.size() > 0) {
            DiscordUtil.addRolesToMember(member, rolesToAdd);
            changes.add("+ " + String.join("", rolesToAdd.stream().map(Role::toString).collect(Collectors.toList())));
        }
        if (rolesToRemove.size() > 0) {
            DiscordUtil.removeRolesFromMember(member, rolesToRemove);
            changes.add("- " + String.join("", rolesToRemove.stream().map(Role::toString).collect(Collectors.toList())));
        }

        DiscordSRV.debug("Synced player " + player.getName() + " (" + (changes.size() > 0 ? String.join(" | ", changes) : "no changes") + ")");
    }

}
