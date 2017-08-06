package github.scarsz.discordsrv.hooks.vanish;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PhantomAdminHook {
    
    public static boolean isVanished(Player player) {
        try {
            Object phantomPlugin = Bukkit.getPluginManager().getPlugin("PhantomAdmin");
            Method isInvisible = phantomPlugin.getClass().getDeclaredMethod("isInvisible", Player.class);
            isInvisible.setAccessible(true);
            
            return (boolean) isInvisible.invoke(phantomPlugin, player);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
}
