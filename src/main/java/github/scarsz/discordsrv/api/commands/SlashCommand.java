/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.api.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to denote the attached method as a slash command handler.
 * <strong>Method must have exactly one parameter of type {@link SlashCommandEvent}.</strong>
 * <strong>If your command might take longer than 3 seconds to execute, you must defer your reply with {@link SlashCommand#deferReply()}.</strong>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SlashCommand {

    /**
     * The slash-separated command path that shall be handled by this method
     *
     * <p>Examples:
     * <ul>
     *     <li>{@code /mod ban -> "mod/ban"}</li>
     *     <li>{@code /admin config owner -> "admin/config/owner"}</li>
     *     <li>{@code /ban -> "ban"}</li>
     * </ul>
     * @return the command path
     * @see SlashCommandEvent#getCommandPath()
     */
    String path();

    /**
     * Tells DiscordSRV to automatically acknowledge the command & defer replying for you.
     * Reply deferring is required when your command might take longer than 3 seconds to execute.
     * If the reply is deferred, you have up to 15 minutes for your command to complete.
     * Deferring will show a "bot is thinking" message to the user until you send your response.
     * <p>
     * <strong>You cannot acknowledge the command yourself if you enable this.</strong>
     * Send your replies through the {@link InteractionHook} via <code>{@link SlashCommandEvent}.getHook().sendMessage(...)</code>
     * @return whether DiscordSRV should automatically defer the interaction's reply when routing the event to your handler
     */
    boolean deferReply() default false;
    /**
     * @return whether events that are automatically deferred with {@link SlashCommand#deferReply()} will be ephemeral
     */
    boolean deferEphemeral() default false;

}
