/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
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
 * END
 */

package github.scarsz.discordsrv.util;

import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;

public class UpdateUtil {

    public static boolean checkForUpdates() {
        return checkForUpdates(true);
    }

    /**
     * Check the build hash of DiscordSRV against the latest hashes from GitHub
     * @return boolean indicating if an update to DiscordSRV is available
     */
    public static boolean checkForUpdates(boolean verbose) {
        JsonObject jsonObject = null;
        try {
            String buildHash = ManifestUtil.getManifestValue("Git-Revision");

            if (buildHash == null || buildHash.equalsIgnoreCase("unknown") || buildHash.length() != 40) {
                DiscordSRV.warning("Git-Revision wasn't available, plugin is a dev build");
                DiscordSRV.warning("You will receive no support for this plugin version.");
                return false;
            }

            String minimumHash = HttpUtil.requestHttp("https://raw.githubusercontent.com/DiscordSRV/DiscordSRV/randomaccessfiles/minimumbuild").trim();
            if (minimumHash.length() == 40) { // make sure we have a hash
                JsonObject minimumComparisonResult = jsonObject = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/compare/" + minimumHash + "..." + buildHash + "?per_page=1"), JsonObject.class);
                boolean minimumAhead = minimumComparisonResult.get("status").getAsString().equalsIgnoreCase("behind");
                if (minimumAhead) {
                    printUpdateMessage("The current build of DiscordSRV does not meet the minimum required to be secure! DiscordSRV will not start.");
                    DiscordSRV.getPlugin().disablePlugin();
                    return true;
                }
            } else {
                DiscordSRV.warning("Failed to check against minimum version of DiscordSRV: received minimum build was not 40 characters long & thus not a commit hash");
            }

            // build is ahead of minimum so that's good

            String masterHash = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/git/refs/heads/master"), JsonObject.class).getAsJsonObject("object").get("sha").getAsString();
            JsonObject masterComparisonResult = jsonObject = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/compare/" + masterHash + "..." + buildHash + "?per_page=1"), JsonObject.class);
            String masterStatus = masterComparisonResult.get("status").getAsString();
            switch (masterStatus.toLowerCase()) {
                case "ahead":
                case "diverged":
                    if (!verbose) {
                        return false;
                    }
                    String developHash = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/git/refs/heads/develop"), JsonObject.class).getAsJsonObject("object").get("sha").getAsString();
                    JsonObject developComparisonResult = jsonObject = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/compare/" + developHash + "..." + buildHash + "?per_page=1"), JsonObject.class);
                    String developStatus = developComparisonResult.get("status").getAsString();
                    switch (developStatus.toLowerCase()) {
                        case "ahead":
                            DiscordSRV.info("This build of DiscordSRV is ahead of master and develop. [latest private dev build]");
                            return false;
                        case "identical":
                            DiscordSRV.info("This build of DiscordSRV is identical to develop. [latest public dev build]");
                            return false;
                        case "behind":
                            DiscordSRV.warning("This build of DiscordSRV is ahead of master but behind develop. Update your development build!");
                            return true;
                    }
                    return false;
                case "behind":
                    jsonObject = masterComparisonResult;
                    printUpdateMessage("The current build of DiscordSRV is outdated by " + masterComparisonResult.get("behind_by").getAsInt() + " commits!");
                    return true;
                case "identical":
                    if (verbose) DiscordSRV.info("DiscordSRV is up-to-date. (" + buildHash + ")");
                    return false;
                default:
                    DiscordSRV.warning("Got weird build comparison status from GitHub: " + masterStatus + ". Assuming plugin is up-to-date.");
                    return false;
            }
        } catch (Exception e) {
            if (e.getMessage() != null) {
                if (e.getMessage().contains("google.gson") && jsonObject != null) {
                    try {
                        // Not required, make log messages huge
                        jsonObject.remove("files");
                        jsonObject.remove("commits");
                    } catch (Throwable ignored) {}
                    DiscordSRV.warning("Update check failed due to unexpected json response: " + e.getMessage() + " (" + jsonObject + ")");
                } else {
                    DiscordSRV.warning("Update check failed: " + e.getMessage());
                }
            } else {
                DiscordSRV.warning("Update check failed: " + e.getClass().getName());
            }
            DiscordSRV.debug(e);
            return false;
        }
    }

    private static void printUpdateMessage(String explanation) {
        DiscordSRV.warning("\n\n" + explanation + " Get the latest build at your favorite distribution center.\n\n" +
                "Spigot: https://www.spigotmc.org/resources/discordsrv.18494/\n" +
                "Github: https://github.com/DiscordSRV/DiscordSRV/releases\n" +
                "Direct Download: https://get.discordsrv.com\n");
    }

}
