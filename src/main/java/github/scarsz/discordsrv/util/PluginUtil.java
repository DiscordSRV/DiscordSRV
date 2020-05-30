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

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unchecked")
public class PluginUtil {

    public static void unloadPlugin(Plugin plugin) {
        String name = plugin.getName();
        PluginManager pluginManager = Bukkit.getPluginManager();
        SimpleCommandMap commandMap = null;
        List<Plugin> plugins = null;
        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;

        boolean reloadListeners = true;
        pluginManager.disablePlugin(plugin);

        try {
            Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            plugins = (List<Plugin>) pluginsField.get(pluginManager);

            Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            names = (Map<String, Plugin>) lookupNamesField.get(pluginManager);

            try {
                Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                listenersField.setAccessible(true);
                listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
            } catch (Exception e) {
                reloadListeners = false;
            }

            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (SimpleCommandMap) commandMapField.get(pluginManager);

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            commands = (Map<String, Command>) knownCommandsField.get(commandMap);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        pluginManager.disablePlugin(plugin);

        if (plugins != null && plugins.contains(plugin))
            plugins.remove(plugin);

        if (names != null && names.containsKey(name))
            names.remove(name);

        if (listeners != null && reloadListeners) {
            for (SortedSet<RegisteredListener> set : listeners.values()) {
                set.removeIf(value -> value.getPlugin() == plugin);
            }
        }

        if (commandMap != null) {
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin() == plugin) {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }
        }

        ClassLoader cl = plugin.getClass().getClassLoader();
        if (cl instanceof URLClassLoader) {
            try {
                ((URLClassLoader) cl).close();
            } catch (IOException ex) {
                Logger.getLogger(PluginUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.gc();
    }

    /**
     * Check whether or not the given plugin is installed and enabled
     * @param pluginName The plugin name to check
     * @return Whether or not the plugin is installed and enabled
     */
    public static boolean checkIfPluginEnabled(String pluginName) {
        return checkIfPluginEnabled(pluginName, true);
    }

    /**
     * Check whether or not the given plugin is installed and enabled
     * @param pluginName The plugin name to check
     * @param startsWith Whether or not to to {@link String#startsWith(String)} checking
     * @return Whether or not the plugin is installed and enabled
     */
    public static boolean checkIfPluginEnabled(String pluginName, boolean startsWith) {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            boolean match = startsWith
                    ? plugin.getName().toLowerCase().startsWith(pluginName.toLowerCase())
                    : plugin.getName().equalsIgnoreCase(pluginName);
            if (match) {
                if (plugin.isEnabled()) {
                    return true;
                } else {
                    DiscordSRV.debug("Plugin " + plugin.getName() + " found but wasn't enabled. Returning false");
                    return false;
                }
            }
        }
        return false;
    }

    public static boolean pluginHookIsEnabled(String pluginName) {
        return pluginHookIsEnabled(pluginName, true);
    }

    public static boolean pluginHookIsEnabled(String pluginName, boolean startsWith) {
        boolean enabled = checkIfPluginEnabled(pluginName, startsWith);
        for (String pluginHookName : DiscordSRV.config().getStringList("DisabledPluginHooks")) {
            if (pluginName.toLowerCase().startsWith(pluginHookName.toLowerCase())) {
                enabled = false;
                break;
            }
        }
        return enabled;
    }

    public static JavaPlugin getPlugin(String pluginName) {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins())
            if (plugin.getName().toLowerCase().startsWith(pluginName.toLowerCase())) return (JavaPlugin) plugin;
        return null;
    }

}
