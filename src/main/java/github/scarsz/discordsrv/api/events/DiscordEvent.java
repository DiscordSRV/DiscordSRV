package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import net.dv8tion.jda.core.JDA;

/**
 * <p>The superclass of all Discord-related events</p>
 * <p>Provides {@link #getJda()} and {@link #getRawEvent()}</p>
 */
public abstract class DiscordEvent<T> extends Event {

    @Getter final private JDA jda;
    @Getter final private T rawEvent;

    DiscordEvent(JDA jda) {
        this.jda = jda;
        this.rawEvent = null;
    }
    DiscordEvent(JDA jda, T rawEvent) {
        this.jda = jda;
        this.rawEvent = rawEvent;
    }

}
