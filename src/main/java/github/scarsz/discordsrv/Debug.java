package github.scarsz.discordsrv;

import lombok.Getter;

import java.util.Arrays;

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

    UNCATEGORIZED("all"),
    JDA(),
    JDA_REST_ACTIONS("jdarestactions", "jdarest", "restactions", "rest"),
    CALLSTACKS("stack", "stacks", "callstack",
            "trace", "traces", "stacktrace", "errors", "exceptions", "exception", "except");

    @Getter private final String[] aliases;

    Debug(String... aliases) {
        this.aliases = aliases;
    }

    public boolean isVisible() {
        return DiscordSRV.config().getStringList("Debug").stream()
                .anyMatch(s ->
                        (s.equalsIgnoreCase("all") && this != CALLSTACKS && this != JDA && this != JDA_REST_ACTIONS)
                        || s.equalsIgnoreCase("chat") && (this == MINECRAFT_TO_DISCORD || this == DISCORD_TO_MINECRAFT)
                        || s.equalsIgnoreCase(name())
                        || Arrays.stream(aliases).anyMatch(s::equalsIgnoreCase)
                );
    }

}
