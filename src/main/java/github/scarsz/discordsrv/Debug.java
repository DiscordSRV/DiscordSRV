package github.scarsz.discordsrv;

import lombok.Getter;

import java.util.Arrays;

@SuppressWarnings("SpellCheckingInspection")
public enum Debug {

    EVENTS("event"),
    GROUP_SYNC("group", "groups", "gsync", "role", "roles", "groupsync", "rolesync"),

    UNCATEGORIZED("all"),
    STACKTRACES("errors", "exceptions", "exception", "except", "stack", "trace", "traces", "stacktrace");

    @Getter private final String[] aliases;

    Debug(String... aliases) {
        this.aliases = aliases;
    }

    public boolean isVisible() {
        return DiscordSRV.config().getStringList("Debug").stream()
                .anyMatch(s ->
                        (s.equalsIgnoreCase("all") && this != STACKTRACES)
                        || s.equalsIgnoreCase(name())
                        || Arrays.stream(aliases).anyMatch(s::equalsIgnoreCase)
                );
    }

}
