package github.scarsz.discordsrv;

import lombok.Getter;

import java.util.Arrays;

@SuppressWarnings("SpellCheckingInspection")
public enum Debug {

    EVENTS("event"),
    GROUP_SYNC("group", "groups", "gsync", "role", "roles", "groupsync", "rolesync"),
    PRESENCE("presence", "game", "gamestatus", "playing", "playingstatus", "status"),
    VOICE("voice", "voicemodule"),
    REQUIRE_LINK("requirelink", "requirelink2play", "requirelinktoplay", "link2play", "linktoplay"),
    NICKNAME_SYNC("nickname", "nicknamesync"),

    UNCATEGORIZED("all"),
    JDA("jda"),
    JDA_REST_ACTIONS("jdarestactions", "jdarest", "restactions", "rest"),
    CALLSTACKS("stack", "stacks", "callstack", "callstacks",
            "trace", "traces", "stacktrace", "errors", "exceptions", "exception", "except");

    @Getter private final String[] aliases;

    Debug(String... aliases) {
        this.aliases = aliases;
    }

    public boolean isVisible() {
        return DiscordSRV.config().getStringList("Debug").stream()
                .anyMatch(s ->
                        (s.equalsIgnoreCase("all") && this != CALLSTACKS && this != JDA && this != JDA_REST_ACTIONS)
                        || s.equalsIgnoreCase(name())
                        || Arrays.stream(aliases).anyMatch(s::equalsIgnoreCase)
                );
    }

}
