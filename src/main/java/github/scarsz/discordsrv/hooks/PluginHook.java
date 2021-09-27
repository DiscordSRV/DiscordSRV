/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public interface PluginHook extends Listener {

    Plugin getPlugin();

    default boolean isEnabled() {
        Plugin plugin = getPlugin();
        if (plugin == null) {
            return false;
        }

        if (!plugin.isEnabled()) {
            DiscordSRV.debug("Plugin hook " + getClass().getName() + " (" + plugin.getName()
                    + ") not enabled due to the plugin being disabled");
            return false;
        }

        if (!PluginUtil.pluginHookIsEnabled(getPlugin().getName())) {
            DiscordSRV.debug("Plugin hook " + getClass().getName() + " is disabled because "
                    + plugin.getName() + " is disabled via the configuration");
            return false;
        }

        return true;
    }

    default void hook() {}

}
