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

package github.scarsz.discordsrv;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

@SuppressWarnings("SpellCheckingInspection")
public enum Debug {

    MINECRAFT_TO_DISCORD("minecrafttodiscord", "mctodiscord", "todiscord", "minecraft", "minecraftchat"),
    DISCORD_TO_MINECRAFT("discordtominecraft", "discordtomc", "tominecraft", "discord", "discordchat"),

    GROUP_SYNC("group", "groups", "gsync", "role", "roles", "groupsync", "rolesync"),
    PRESENCE("game", "gamestatus", "playing", "playingstatus", "status"),
    VOICE("voicemodule"),
    REQUIRE_LINK("requirelink", "requirelink2play", "requirelinktoplay", "link2play", "linktoplay"),
    NICKNAME_SYNC("nickname", "nicknamesync"),
    ALERTS("alert"),
    WATCHDOG("watchdog"),
    BAN_SYNCHRONIZATION("ban", "bans"),
    LP_CONTEXTS("luckpermscontexts", "luckpermscontext", "lpcontexts", "lpcontext"),
    ACCOUNT_LINKING("linkedaccounts", "accountlinking"),

    UNCATEGORIZED("all"),
    JDA(),
    JDA_REST_ACTIONS("jdarestactions", "jdarest", "restactions", "rest"),
    CALLSTACKS("stack", "stacks", "callstack",
            "trace", "traces", "stacktrace", "errors", "exceptions", "exception", "except");

    @Getter private final String[] aliases;

    Debug(String... aliases) {
        this.aliases = aliases;
    }

    public boolean matches(String s) {
        return (s.equalsIgnoreCase("all") && this != CALLSTACKS && this != JDA && this != JDA_REST_ACTIONS)
                || s.equalsIgnoreCase("chat") && (this == MINECRAFT_TO_DISCORD || this == DISCORD_TO_MINECRAFT)
                || s.equalsIgnoreCase(name())
                || Arrays.stream(aliases).anyMatch(s::equalsIgnoreCase);
    }

    public boolean isVisible() {
        boolean oldLevel = DiscordSRV.config().getIntElse("DebugLevel", 0) > 0;
        if (oldLevel && matches("all")) {
            return true;
        }

        Set<String> debuggerCategories = DiscordSRV.getPlugin().getDebuggerCategories();
        if (!debuggerCategories.isEmpty() && debuggerCategories.stream().anyMatch(this::matches)) {
            return true;
        }
        return DiscordSRV.config().getStringList("Debug").stream().anyMatch(this::matches);
    }

    public static boolean anyEnabled() {
        for (Debug value : values()) {
            if (value.isVisible()) {
                return true;
            }
        }
        return false;
    }

}
