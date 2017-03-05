package github.scarsz.discordsrv.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * todo
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
