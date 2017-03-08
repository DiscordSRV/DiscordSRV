package github.scarsz.discordsrv.objects;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.hooks.permissions.*;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.Getter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/5/2017
 * @at 2:38 PM
 */
public class GroupSynchronizationManager {

    @Getter private PermissionSystemHook permissionSystemHook = null;

    public void init() {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.getName().toLowerCase().startsWith("groupmanager")) permissionSystemHook = new GroupManagerHook();
            if (plugin.getName().toLowerCase().startsWith("luckperms")) permissionSystemHook = new LuckPermsHook();
            if (plugin.getName().toLowerCase().startsWith("permissionsex")) permissionSystemHook = new PermissionsExHook();
            if (plugin.getName().toLowerCase().startsWith("zpermissions")) permissionSystemHook = new ZPermissionsHook();
        }

        if (permissionSystemHook == null)
            DiscordSRV.warning("No supported permissions management plugin detected. Group synchronization will not work.");
    }

    public void reSyncGroups(Player player, String... newGroups) {
        DiscordSRV.debug("Synchronizing groups for player " + player.getName() + " " + Arrays.toString(newGroups));

        // get member
        Member member = DiscordSRV.getPlugin().getMainTextChannel().getGuild().getMemberById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId()));

        if (member == null) {
            DiscordSRV.debug("Tried to sync groups for player " + player.getName() + " but their MC account is not linked to a Discord account");
            return;
        }

        // get all the roles to synchronize from the config
        Map<String, Role> synchronizables = new HashMap<>();
        for (Map.Entry<String, Object> pairToSynchronize : ((MemorySection) DiscordSRV.getPlugin().getConfig().get("GroupRoleSynchronizationRolesToSynchronize")).getValues(true).entrySet()) {
            Role role = DiscordUtil.getRole(DiscordSRV.getPlugin().getMainTextChannel().getGuild(), (String) pairToSynchronize.getValue());

            if (role == null) {
                DiscordSRV.warning("Could not find role with name \"" + pairToSynchronize.getValue() + "\" for use with group synchronization. Is the bot in the server?");
                continue;
            }

            synchronizables.put(pairToSynchronize.getKey(), role);
        }
        if (synchronizables.size() == 0) {
            DiscordSRV.debug("Tried to sync groups but no synchronizables existed");
            return;
        }

        List<Role> rolesToAdd = new ArrayList<>();
        List<Role> rolesToRemove = new ArrayList<>();

        for (Map.Entry<String, Role> pair : synchronizables.entrySet()) {
            if (Arrays.asList(newGroups).contains(pair.getKey())) {
                // this synchronizable pair was found in the user's groups thus they should be added to that role
                rolesToAdd.add(pair.getValue());
            } else {
                // this synchronizable pair was not found in the user's groups thus they should be removed from that role
                rolesToRemove.add(pair.getValue());
            }
        }

        // remove roles that the user already has from roles to add
        rolesToAdd.removeAll(member.getRoles());
        // remove roles that the user doesn't already have from roles to remove
        rolesToRemove.removeIf(role -> !member.getRoles().contains(role));

        if (rolesToAdd.size() > 0) DiscordUtil.addRolesToMember(member, rolesToAdd);
        if (rolesToRemove.size() > 0) DiscordUtil.removeRolesFromMember(member, rolesToRemove);
    }

}
