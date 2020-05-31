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

package github.scarsz.discordsrv.hooks.vanish;

import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VanishNoPacketHook implements VanishHook {

    @Override
    public boolean isVanished(Player player) {
        try {
            Object vanishPlugin = Bukkit.getPluginManager().getPlugin("VanishNoPacket");
            Object vanishManager = vanishPlugin.getClass().getMethod("getManager").invoke(vanishPlugin);

            return (boolean) vanishManager.getClass().getMethod("isVanished", Player.class).invoke(vanishManager, player);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("VanishNoPacket");
    }

}
