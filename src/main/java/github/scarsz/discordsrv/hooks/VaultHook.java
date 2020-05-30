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

package github.scarsz.discordsrv.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VaultHook implements PluginHook {

    public static String getPrimaryGroup(Player player) {
        if (!PluginUtil.pluginHookIsEnabled("vault")) {
            DiscordSRV.debug("Tried looking up primary group for player " + player.getName() + " but the Vault plugin hook wasn't enabled");
            return " ";
        }

        try {
            net.milkbowl.vault.permission.Permission permissionProvider = (net.milkbowl.vault.permission.Permission) Bukkit.getServer().getServicesManager().getRegistration(Class.forName("net.milkbowl.vault.permission.Permission")).getProvider();
            if (permissionProvider == null) {
                DiscordSRV.debug("Tried looking up group for player " + player.getName() + " but failed to get the registered service provider for Vault");
                return " ";
            }

            String primaryGroup = permissionProvider.getPrimaryGroup(player);
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
            RegisteredServiceProvider<?> service = Bukkit.getServer().getServicesManager().getRegistration(Class.forName("net.milkbowl.vault.permission.Permission"));
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

    public static String[] getGroups() {
        if (!PluginUtil.pluginHookIsEnabled("vault")) return new String[] {};

        try {
            RegisteredServiceProvider<?> service = Bukkit.getServer().getServicesManager().getRegistration(Class.forName("net.milkbowl.vault.permission.Permission"));
            if (service == null) return new String[] {};

            Method getGroupsMethod = service.getProvider().getClass().getMethod("getGroups");
            return (String[]) getGroupsMethod.invoke(service.getProvider());
        } catch (Exception ignored) { }
        return new String[] {};
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Vault");
    }

}
