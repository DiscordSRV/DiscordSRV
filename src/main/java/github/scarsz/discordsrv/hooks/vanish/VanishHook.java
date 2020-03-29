package github.scarsz.discordsrv.hooks.vanish;

import github.scarsz.discordsrv.hooks.PluginHook;
import org.bukkit.entity.Player;

public interface VanishHook extends PluginHook {

    boolean isVanished(Player player);

}
