package github.scarsz.discordsrv.objects;

public class Lag implements Runnable {

    private static int TICK_COUNT= 0;
    private static long[] TICKS = new long[600];

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
