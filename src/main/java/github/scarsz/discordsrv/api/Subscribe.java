package github.scarsz.discordsrv.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * <p>Marks a {@link Method} as one that should receive DiscordSRV API events</p>
 * <p>Functionally identical to Bukkit's EventHandler annotation</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * The priority of which the event should be ran. Multiple listeners with the same priority will be ran haphazardly.
     * @return the priority of the event listener method
     */
    ListenerPriority priority() default ListenerPriority.NORMAL;

}
