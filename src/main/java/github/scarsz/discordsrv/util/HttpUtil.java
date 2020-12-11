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

package github.scarsz.discordsrv.util;

import com.github.kevinsawicki.http.HttpRequest;
import github.scarsz.discordsrv.DiscordSRV;

import java.io.File;

public class HttpUtil {

    public static String requestHttp(String requestUrl) {
        try {
            return HttpRequest.get(requestUrl).body();
        } catch (HttpRequest.HttpRequestException e) {
            DiscordSRV.error(LangUtil.InternalMessage.HTTP_FAILED_TO_FETCH_URL + " " + requestUrl + ": " + e.getMessage());
            return "";
        }
    }

    public static void downloadFile(String requestUrl, File destination) {
        try {
            HttpRequest.get(requestUrl).receive(destination);
        } catch (HttpRequest.HttpRequestException e) {
            DiscordSRV.error(LangUtil.InternalMessage.HTTP_FAILED_TO_DOWNLOAD_URL + " " + requestUrl + ": " + e.getMessage());
        }
    }

    public static boolean exists(String url) {
        try {
            HttpRequest request = HttpRequest.head(url);
            return request.code() / 100 == 2;
        } catch (Exception e) {
            return false;
        }
    }

}
