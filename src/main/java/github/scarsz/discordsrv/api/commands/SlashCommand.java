package github.scarsz.discordsrv.api.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to denote the attached method as a slash command handler.
 * <strong>Method must have exactly one parameter of type {@link SlashCommandEvent}.</strong>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SlashCommand {

    /**
     * The command path that shall be handled by this method
     * @return the command path
     * @see SlashCommandEvent#getCommandPath()
     */
    String path();

    /**
     * @return whether DiscordSRV should automatically defer the interaction's reply when routing the event to your handler
     */
    boolean deferReply() default false;
    /**
     * @return whether events that are automatically deferred with {@link SlashCommand#deferReply()} will be ephemeral
     */
    boolean deferEphemeral() default false;

}
