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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.CloseCode;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class DiscordDisconnectListener extends ListenerAdapter {

    public static CloseCode mostRecentCloseCode = null;

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        handleCode(event.getCloseCode());
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        handleCode(event.getCloseCode());
    }

    private void handleCode(CloseCode closeCode) {
        if (closeCode == null) {
            return;
        }
        mostRecentCloseCode = closeCode;
        if (closeCode == CloseCode.DISALLOWED_INTENTS) {
            Set<GatewayIntent> intents = DiscordSRV.api.getIntents();
            boolean presences = intents.contains(GatewayIntent.GUILD_PRESENCES);

            DiscordSRV.getPlugin().disablePlugin(); // make DiscordSRV go red in /plugins
            DiscordSRV.getPlugin().getLogger().severe("==============================================================");
            DiscordSRV.getPlugin().getLogger().severe(" ");
            DiscordSRV.getPlugin().getLogger().severe(" *** PLEASE FOLLOW THE INSTRUCTIONS BELOW TO GET DiscordSRV TO WORK *** ");
            DiscordSRV.getPlugin().getLogger().severe(" ");
            DiscordSRV.getPlugin().getLogger().severe(" Your DiscordSRV bot does not have the " + (presences ? "Guild Presences and/or " : "") + "Server Members Intent!");
            DiscordSRV.getPlugin().getLogger().severe(" DiscordSRV " + (presences && !DiscordSRV.config().getBooleanElse("EnablePresenceInformation", false)
                    ? "and its API hooks require these intents" : "requires " + (presences ? "these intents" : "this intent")) + " to function. Instructions:");
            DiscordSRV.getPlugin().getLogger().severe("  1. Go to https://discord.com/developers/applications");
            DiscordSRV.getPlugin().getLogger().severe("  2. Click on the DiscordSRV bot");
            DiscordSRV.getPlugin().getLogger().severe("  3. Click on \"Bot\" on the left");
            DiscordSRV.getPlugin().getLogger().severe("  4. Enable the " + (presences ? "\"PRESENCE INTENT\" and " : "") + "\"SERVER MEMBERS INTENT\"");
            DiscordSRV.getPlugin().getLogger().severe("  5. Restart your server");
            DiscordSRV.getPlugin().getLogger().severe(" ");
            DiscordSRV.getPlugin().getLogger().severe("==============================================================");
        } else if (!closeCode.isReconnect()) {
            DiscordSRV.getPlugin().disablePlugin(); // make DiscordSRV go red in /plugins
            printDisconnectMessage(false, closeCode == CloseCode.AUTHENTICATION_FAILED ? "The bot token is invalid" : closeCode.getMeaning());
        }
    }

    public static void printDisconnectMessage(boolean startup, String message) {
        DiscordSRV.getPlugin().getLogger().severe("===================================================");
        DiscordSRV.getPlugin().getLogger().severe(" ");
        DiscordSRV.getPlugin().getLogger().severe(" " + (startup ? "DiscordSRV could not connect to Discord because" : "DiscordSRV was disconnected from Discord because") + ":");
        DiscordSRV.getPlugin().getLogger().severe(" " + message);
        DiscordSRV.getPlugin().getLogger().severe(" ");
        DiscordSRV.getPlugin().getLogger().severe("===================================================");
    }
}
