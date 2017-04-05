package github.scarsz.discordsrv.hooks.vanish;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class SuperVanishHook {

    public static boolean isVanished(Player player) {
        try {
            Class<?> VanishAPI = Class.forName("de.myzelyam.api.vanish.VanishAPI");
            Method getInvisiblePlayers = VanishAPI.getMethod("getInvisiblePlayers");
            List<UUID> invisiblePlayers = (List<UUID>) getInvisiblePlayers.invoke(VanishAPI);
            return invisiblePlayers != null && invisiblePlayers.contains(player.getUniqueId());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
