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

package github.scarsz.discordsrv.hooks.world;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class MultiverseCoreHook implements PluginHook {

    public static String getWorldAlias(String world) {
        try {
            if (!PluginUtil.pluginHookIsEnabled("Multiverse-Core")) return world;

            com.onarandombox.MultiverseCore.MultiverseCore multiversePlugin = (com.onarandombox.MultiverseCore.MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (multiversePlugin != null) {
                MultiverseWorld multiverseWorld = multiversePlugin.getMVWorldManager().getMVWorld(world);
                if (multiverseWorld != null) {
                    return multiverseWorld.getAlias();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return world;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Multiverse-Core");
    }

}
