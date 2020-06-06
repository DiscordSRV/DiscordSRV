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

import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;

public class UpdateUtil {

    /**
     * Check the build hash of DiscordSRV against the latest hashes from GitHub
     * @return boolean indicating if an update to DiscordSRV is available
     */
    public static boolean checkForUpdates() {
        try {
            String buildHash = ManifestUtil.getManifestValue("Git-Revision");

            if (buildHash == null || buildHash.equalsIgnoreCase("unknown") || buildHash.length() != 40) {
                DiscordSRV.warning("Git-Revision wasn't available, plugin is a dev build");
                DiscordSRV.warning("You will receive no support for this plugin version.");
                return false;
            }

            String minimumHash = HttpUtil.requestHttp("https://raw.githubusercontent.com/DiscordSRV/DiscordSRV/randomaccessfiles/minimumbuild").trim();
            if (minimumHash.length() == 40) { // make sure we have a hash
                JsonObject minimumComparisonResult = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/compare/" + minimumHash + "..." + buildHash), JsonObject.class);
                boolean minimumAhead = minimumComparisonResult.get("status").getAsString().equalsIgnoreCase("behind");
                if (minimumAhead) {
                    printUpdateMessage("The current build of DiscordSRV does not meet the minimum required to be secure! DiscordSRV will not start.");
                    Bukkit.getPluginManager().disablePlugin(DiscordSRV.getPlugin());
                    return true;
                }
            } else {
                DiscordSRV.warning("Failed to check against minimum version of DiscordSRV: received minimum build was not 40 characters long & thus not a commit hash");
            }

            // build is ahead of minimum so that's good

            String masterHash = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/git/refs/heads/master"), JsonObject.class).getAsJsonObject("object").get("sha").getAsString();
            JsonObject masterComparisonResult = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/compare/" + masterHash + "..." + buildHash), JsonObject.class);
            String masterStatus = masterComparisonResult.get("status").getAsString();
            switch (masterStatus.toLowerCase()) {
                case "ahead":
                case "diverged":
                    String developHash = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/git/refs/heads/develop"), JsonObject.class).getAsJsonObject("object").get("sha").getAsString();
                    JsonObject developComparisonResult = DiscordSRV.getPlugin().getGson().fromJson(HttpUtil.requestHttp("https://api.github.com/repos/DiscordSRV/DiscordSRV/compare/" + developHash + "..." + buildHash), JsonObject.class);
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
                    printUpdateMessage("The current build of DiscordSRV is outdated by " + masterComparisonResult.get("behind_by").getAsInt() + " commits!");
                    return true;
                case "identical":
                    DiscordSRV.info("DiscordSRV is up-to-date. (" + buildHash + ")");
                    return false;
                default:
                    DiscordSRV.warning("Got weird build comparison status from GitHub: " + masterStatus + ". Assuming plugin is up-to-date.");
                    return false;
            }
        } catch (Exception e) {
            DiscordSRV.warning("Update check failed: " + e.getMessage());
            return false;
        }
    }

    private static void printUpdateMessage(String explanation) {
        DiscordSRV.warning("\n\n" + explanation + " Get the latest build at your favorite distribution center.\n\n" +
                "Spigot: https://www.spigotmc.org/resources/discordsrv.18494/\n" +
                "Bukkit Dev: http://dev.bukkit.org/bukkit-plugins/discordsrv/\n" +
                "Source: https://github.com/DiscordSRV/DiscordSRV\n");
    }

}
