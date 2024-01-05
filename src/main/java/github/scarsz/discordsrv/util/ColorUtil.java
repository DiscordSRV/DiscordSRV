/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.util;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.TreeMap;
import java.util.function.Supplier;

public class ColorUtil {
    private static final ChatColor DEFAULT_COLOR = ChatColor.WHITE;
    private static final boolean MC_1_16;
    private final static TreeMap<Integer, ChatColor> COLOR_MAP = new TreeMap<>();
    private static final String DIGITS = "0123456789abcdef";

    static {
        boolean available = false;
        try {
            Material.valueOf("NETHERITE_PICKAXE").getKey();
            available = true;
        } catch (Throwable ignored) {}

        MC_1_16 = available;
        COLOR_MAP.put(0x000000, ChatColor.BLACK);
        COLOR_MAP.put(0x0000aa, ChatColor.DARK_BLUE);
        COLOR_MAP.put(0x00aa00, ChatColor.DARK_GREEN);
        COLOR_MAP.put(0x00aaaa, ChatColor.DARK_AQUA);
        COLOR_MAP.put(0xaa0000, ChatColor.DARK_RED);
        COLOR_MAP.put(0xffaa00, ChatColor.DARK_PURPLE);
        COLOR_MAP.put(0xaaaaaa, ChatColor.GRAY);
        COLOR_MAP.put(0x555555, ChatColor.DARK_GRAY);
        COLOR_MAP.put(0x5555ff, ChatColor.BLUE);
        COLOR_MAP.put(0x55ff55, ChatColor.GREEN);
        COLOR_MAP.put(0x55ffff, ChatColor.AQUA);
        COLOR_MAP.put(0xff5555, ChatColor.RED);
        COLOR_MAP.put(0xff55ff, ChatColor.LIGHT_PURPLE);
        COLOR_MAP.put(0xffff55, ChatColor.YELLOW);
        COLOR_MAP.put(0xffffff, ChatColor.WHITE);
    }

    public static String getLegacyColor(int color) {
        if (color <= 0) return DEFAULT_COLOR.toString();
        if (MC_1_16) {
            StringBuilder colorBuilder = new StringBuilder();
            while (color > 0) {
                int digit = color % 16;
                colorBuilder.insert(0, "§" + DIGITS.charAt(digit));
                color = color / 16;
            }
            colorBuilder.insert(0, "§x");
            return colorBuilder.toString();
        }
        try {
            int key = COLOR_MAP.floorKey(color);
            ChatColor c = COLOR_MAP.get(key);
            if (c == null)
                return DEFAULT_COLOR.toString();
        } catch (Exception ignore) {}
        return DEFAULT_COLOR.toString();
    }
}
