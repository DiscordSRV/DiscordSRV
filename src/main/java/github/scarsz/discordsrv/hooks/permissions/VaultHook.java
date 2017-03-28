package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 5:41 PM
 */
public class VaultHook {

    public static String getPrimaryGroup(Player player) {
        if (!PluginUtil.pluginHookIsEnabled("vault")) {
            DiscordSRV.debug("Tried looking up group for player " + player.getName() + " but the Vault plugin hook wasn't enabled");
            return " ";
        }

        try {
            RegisteredServiceProvider service = Bukkit.getServer().getServicesManager().getRegistration(Class.forName("net.milkbowl.vault.permission.Permission"));
            if (service == null) {
                DiscordSRV.debug("Tried looking up group for player " + player.getName() + " but failed to get the registered service provider for Vault");
                return " ";
            }

            // ((net.milkbowl.vault.permission.Permission) service.getProvider()).getPrimaryGroup(Player)

            String primaryGroup = (String) service.getProvider().getClass().getMethod("getPrimaryGroup").invoke(service.getProvider(), player);
            if (!primaryGroup.equals("default")) {
                return primaryGroup;
            } else {
                DiscordSRV.debug("Tried looking up group for player " + player.getName() + " but the given group was \"default\"");
                return " ";
            }
        } catch (Exception e) {
            DiscordSRV.debug("Failed to look up group for player " + player.getName() + ": " + e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
            return " ";
        }
    }

    public static String[] getPlayersGroups(OfflinePlayer player) {
        if (!PluginUtil.pluginHookIsEnabled("vault")) return new String[] {};

        try {
            RegisteredServiceProvider service = Bukkit.getServer().getServicesManager().getRegistration(Class.forName("net.milkbowl.vault.permission.Permission"));
            if (service == null) return new String[] {};

            // ((net.milkbowl.vault.permission.Permission) service.getProvider()).getPlayerGroups(worldName, OfflinePlayer)

            List<String> playerGroups = new ArrayList<>();
            Method getPlayerGroupsMethod = service.getProvider().getClass().getMethod("getPlayerGroups");
            for (World world : Bukkit.getWorlds())
                for (String group : (String[]) getPlayerGroupsMethod.invoke(service.getProvider(), world.getName(), player))
                    if (!playerGroups.contains(group)) playerGroups.add(group);
            for (String group : (String[]) getPlayerGroupsMethod.invoke(service.getProvider(), null, player))
                if (!playerGroups.contains(group)) playerGroups.add(group);

            return playerGroups.toArray(new String[0]);
        } catch (Exception ignored) { }
        return new String[] {};
    }

}
