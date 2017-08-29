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
