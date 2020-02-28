package github.scarsz.discordsrv;

import lombok.Getter;

import java.util.Arrays;

@SuppressWarnings("SpellCheckingInspection")
public enum Debug {

    EVENTS("event"),
    GROUP_SYNC("group", "groups", "gsync", "role", "roles", "groupsync", "rolesync"),
    PRESENCE("presence", "game", "gamestatus", "playing", "playingstatus", "status"),

    UNCATEGORIZED("all"),
    CALLSTACKS("stack", "stacks", "callstack", "callstacks",
            "trace", "traces", "stacktrace", "errors", "exceptions", "exception", "except");

    @Getter private final String[] aliases;

    Debug(String... aliases) {
        this.aliases = aliases;
    }

    public boolean isVisible() {
        return DiscordSRV.config().getStringList("Debug").stream()
                .anyMatch(s ->
                        (s.equalsIgnoreCase("all") && this != CALLSTACKS)
                        || s.equalsIgnoreCase(name())
                        || Arrays.stream(aliases).anyMatch(s::equalsIgnoreCase)
                );
    }

}
