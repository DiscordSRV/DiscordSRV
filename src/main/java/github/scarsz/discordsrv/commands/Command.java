package github.scarsz.discordsrv.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 1:15 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {

    String[] commandNames();
    String helpMessage();
    String permission();
    String usageExample() default "";

}
