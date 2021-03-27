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

package github.scarsz.discordsrv.hooks.vanish;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class PhantomAdminHook implements VanishHook {

    @Override
    public boolean isVanished(Player player) {
        try {
            Object phantomPlugin = Bukkit.getPluginManager().getPlugin("PhantomAdmin");
            Method isInvisible = phantomPlugin.getClass().getDeclaredMethod("isInvisible", Player.class);
            isInvisible.setAccessible(true);

            return (boolean) isInvisible.invoke(phantomPlugin, player);
        } catch (Exception e) {
            DiscordSRV.error(e);
            return false;
        }
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("PhantomAdmin");
    }

}
