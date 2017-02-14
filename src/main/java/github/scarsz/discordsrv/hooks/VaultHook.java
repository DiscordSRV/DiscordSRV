package github.scarsz.discordsrv.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 5:41 PM
 */
public class VaultHook {

    public static String getPrimaryGroup(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return " ";

        try {
            RegisteredServiceProvider service = Bukkit.getServer().getServicesManager().getRegistration(Class.forName("net.milkbowl.vault.permission.Permission"));
            if (service == null) return " ";
            String primaryGroup = (String) service.getProvider().getClass().getMethod("getPrimaryGroup").invoke(service.getProvider(), player);
            if (!primaryGroup.equals("default")) return primaryGroup;
        } catch (Exception ignored) { }
        return " ";
    }

}
