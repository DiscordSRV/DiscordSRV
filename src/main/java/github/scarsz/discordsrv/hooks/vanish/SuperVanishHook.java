/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2018 Austin "Scarsz" Shapiro
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

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class SuperVanishHook {

    public static boolean isVanished(Player player) {
        try {
            Class<?> vanishAPI = Class.forName("de.myzelyam.api.vanish.VanishAPI");
            Method getInvisiblePlayers = vanishAPI.getMethod("getInvisiblePlayers");
            List<UUID> invisiblePlayers = (List<UUID>) getInvisiblePlayers.invoke(vanishAPI);
            return invisiblePlayers != null && invisiblePlayers.contains(player.getUniqueId());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
