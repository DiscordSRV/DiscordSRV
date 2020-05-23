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

package github.scarsz.discordsrv.objects;

public class Lag implements Runnable {

    private static final long[] TICKS = new long[600];
    private static int TICK_COUNT = 0;

    public static String getTPSString() {
        try {
            double tpsDouble = getTPS();
            if (tpsDouble > 19.5) tpsDouble = 20.0;
            String tps = Double.toString(tpsDouble);
            return tps.length() > 4 ? tps.substring(0, 4) : tps;
        } catch (Exception e) {
            return "3.14";
        }
    }

    private static double getTPS() {
        return getTPS(100);
    }

    private static double getTPS(int ticks) {
        if (TICK_COUNT < ticks) return 20.0D;
        int target = (TICK_COUNT - 1 - ticks) % TICKS.length;
        long elapsed = System.currentTimeMillis() - TICKS[target];
        return ticks / (elapsed / 1000.0D);
    }

    public void run() {
        TICKS[(TICK_COUNT % TICKS.length)] = System.currentTimeMillis();
        TICK_COUNT += 1;
    }

}
