/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
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

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    private static Date date = new Date();
    private static SimpleDateFormat format = new SimpleDateFormat("EEE, d. MMM yyyy HH:mm:ss z");

    public static String timeStamp() {
        date.setTime(System.currentTimeMillis());
        return format.format(date);
    }

}
