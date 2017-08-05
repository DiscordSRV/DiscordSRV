package github.scarsz.discordsrv.hooks.vanish;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PhantomAdminHook {
    
    public static boolean isVanished(Player player) {
        
        try {
            
            Plugin phantomAdmin = Bukkit.getPluginManager().getPlugin("PhantomAdmin");
            Method isInvisible = phantomAdmin.getClass().getDeclaredMethod("isInvisible", new Class[]{Player.class});
            isInvisible.setAccessible(true);
            return (boolean) isInvisible.invoke(phantomAdmin, player);
            
        } catch (NoSuchMethodException | SecurityException ex) {
            ex.printStackTrace(System.err);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace(System.err);
        }
        
        return false;
    }
}
