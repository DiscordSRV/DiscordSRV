package github.scarsz.discordsrv.util;

import java.util.Date;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 8:25 PM
 */
public class TimeUtil {

    private static Date date = new Date();

    public static String timeStamp() {
        date.setTime(System.currentTimeMillis());
        return date.toString();
    }

}
