/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.api.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.lang.annotation.*;

/**
 * Annotation to denote the attached method as a slash command handler.
 * Can be repeated to handle multiple command paths on the same method.
 * <strong>Method must have exactly one parameter of type {@link SlashCommandEvent}.</strong>
 * <strong>If your command might take longer than 3 seconds to execute, you must defer your reply with {@link SlashCommand#deferReply()}.</strong>
 */
@Repeatable(SlashCommands.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SlashCommand {

    /**
     * The slash-separated command path that shall be handled by this method.
     * You can use {@code *} to accept all slash command events and filter them yourself.
     *
     * <p><a href="https://en.wikipedia.org/wiki/Glob_(programming)">Glob patterns</a> are supported.
     * Use {@code *} to match many characters and {@code ?} to match single characters.
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
     * The priority of this slash command handler. Multiple slash commands handlers with the same priority will be fired
     * in an undefined order.
     * @return the priority of the slash command handler method
     */
    SlashCommandPriority priority() default SlashCommandPriority.NORMAL;

    /**
     * Tells DiscordSRV to automatically acknowledge the command & defer replying for you.
     * Reply deferring is required when your command might take longer than 3 seconds to execute.
     * If the reply is deferred, you have up to 15 minutes for your command to respond.
     * Until then, a "bot is thinking" message will be shown to the user that executed the command.
     * <p>
     * <strong>You cannot acknowledge the command yourself if you enable this.</strong>
     * Send your replies through the {@link InteractionHook} via <code>{@link SlashCommandEvent}.getHook().sendMessage(...)</code>
     * @return whether DiscordSRV should automatically defer the interaction's reply when routing the event to your handler
     */
    boolean deferReply() default false;

    /**
     * @return whether events that are automatically deferred with {@link SlashCommand#deferReply()} will be ephemeral;
     * ephemeral replies are only shown temporarily to the user that executed the command.
     */
    boolean deferEphemeral() default false;

    /**
     * @return whether DiscordSRV should still invoke this slash command handler if the event had already been
     * acknowledged in a prior handler.
     */
    boolean ignoreAcknowledged() default false;

}
