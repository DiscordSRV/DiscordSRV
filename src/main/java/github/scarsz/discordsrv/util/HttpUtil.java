package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/7/2017
 * @at 4:16 PM
 */
public class HttpUtil {

    public static String requestHttp(String requestUrl) {
        try {
            return IOUtils.toString(new URL(requestUrl), Charset.forName("UTF-8"));
        } catch (IOException e) {
            DiscordSRV.error(LangUtil.InternalMessage.HTTP_FAILED_TO_FETCH_URL + " " + requestUrl + ": " + e.getMessage());
            return "";
        }
    }

    public static void downloadFile(String requestUrl, File destination) {
        try {
            FileUtils.copyURLToFile(new URL(requestUrl), destination);
        } catch (IOException e) {
            DiscordSRV.error(LangUtil.InternalMessage.HTTP_FAILED_TO_DOWNLOAD_URL + " " + requestUrl + ": " + e.getMessage());
        }
    }

}
