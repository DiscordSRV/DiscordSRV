/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
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

import github.scarsz.discordsrv.DiscordSRV;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtil {

    private static final Date date = new Date();
    private static final SimpleDateFormat timestampFormat;
    private static final SimpleDateFormat dateFormat;
    private static final SimpleDateFormat consoleTimeFormat;
    private static final TimeZone zone;

    static {
        timestampFormat = new SimpleDateFormat(DiscordSRV.config().getOptionalString("TimestampFormat").orElse("EEE, d. MMM yyyy HH:mm:ss z"));
        dateFormat = new SimpleDateFormat(DiscordSRV.config().getOptionalString("DateFormat").orElse("yyyy-MM-dd"));
        consoleTimeFormat = new SimpleDateFormat(DiscordSRV.config().getOptionalString("DiscordConsoleChannelTimestampFormat").orElse("EEE HH:mm:ss"));

        String timezone = DiscordSRV.config().getOptionalString("Timezone").orElse("default");
        zone = timezone.equalsIgnoreCase("default") ? TimeZone.getDefault() : TimeZone.getTimeZone(timezone);
        timestampFormat.setTimeZone(zone);
        dateFormat.setTimeZone(zone);
        consoleTimeFormat.setTimeZone(zone);
    }

    public static String format(String format) {
        return format(new SimpleDateFormat(format));
    }
    public static String format(long time) {
        return format(time, timestampFormat);
    }
    public static String format(SimpleDateFormat format) {
        return format(System.currentTimeMillis(), format);
    }
    public static String format(long timestamp, SimpleDateFormat format) {
        date.setTime(timestamp);
        return format.format(date);
    }

    public static String date() {
        return format(dateFormat);
    }
    public static String timeStamp() {
        return format(timestampFormat);
    }
    public static String consoleTimeStamp() {
        return format(System.currentTimeMillis());
    }
    public static String consoleTimeStamp(long timestamp) {
        return format(timestamp, consoleTimeFormat);
    }

}
