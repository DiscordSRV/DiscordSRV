/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.api;

import com.google.common.collect.Sets;
import com.hrakaroo.glob.GlobPattern;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.CommandRegistrationError;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandPriority;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.api.events.Event;
import github.scarsz.discordsrv.api.events.GuildSlashCommandUpdateEvent;
import github.scarsz.discordsrv.util.LangUtil;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * <p>The manager of all of DiscordSRV's API related functionality.</p>
 * <p>DiscordSRV's API works exactly like Bukkit's.</p>
 * <br>
 * <p>To register your class to receive events:</p>
 * <ol>
 *     <li>Have a class with methods marked {@link Subscribe} that take the event to listen for as it's <i>only</i> parameter</li>
 *     <li>Pass an instance of that class to {@link #subscribe(Object)}</li>
 * </ol>
 * <p>{@link ListenerPriority} can optionally be set, defaulting to ListenerPriority.NORMAL</p>
 *
 * @see #subscribe(Object) subscribe listener
 * @see #unsubscribe(Object) unsubscribe listener
 * @see #callEvent(Event) call an event
 * @see #requireIntent(GatewayIntent)
 * @see #requireCacheFlag(CacheFlag)
 * @see SlashCommandProvider to register Discord slash commands
 */
@SuppressWarnings("unused")
public class ApiManager extends ListenerAdapter {

    private final List<Object> apiListeners = new CopyOnWriteArrayList<>();
    private final Set<SlashCommandProvider> slashCommandProviders = new CopyOnWriteArraySet<>();
    private final Set<PluginSlashCommand> runningCommandData = new HashSet<>();
    private boolean anyHooked = false;

    private final EnumSet<GatewayIntent> intents = EnumSet.of(
            // required for DiscordSRV's use
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_BANS,
            GatewayIntent.GUILD_EMOJIS,
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.DIRECT_MESSAGES
    );

    private final EnumSet<CacheFlag> cacheFlags = EnumSet.of(
            // required for DiscordSRV's use
            CacheFlag.MEMBER_OVERRIDES,
            CacheFlag.VOICE_STATE,
            CacheFlag.EMOTE
    );

    /**
     * Subscribe the given instance to DiscordSRV events
     * @param listener the instance to subscribe DiscordSRV events to
     * @throws IllegalArgumentException if the object has zero methods that are annotated with {@link Subscribe}
     */
    public void subscribe(Object listener) {
        // ensure at least one method available in given object that is annotated with Subscribe
        int methodsAnnotatedSubscribe = 0;
        for (Method method : listener.getClass().getMethods()) if (method.isAnnotationPresent(Subscribe.class)) methodsAnnotatedSubscribe++;
        if (methodsAnnotatedSubscribe == 0) throw new IllegalArgumentException(listener.getClass().getName() + " attempted DiscordSRV API registration but no public methods inside of it were annotated @Subscribe (github.scarsz.discordsrv.api.Subscribe)");

        if (!listener.getClass().getPackage().getName().contains("scarsz.discordsrv")) {
            DiscordSRV.info(LangUtil.InternalMessage.API_LISTENER_SUBSCRIBED.toString()
                    .replace("{listenername}", listener.getClass().getName())
                    .replace("{methodcount}", String.valueOf(methodsAnnotatedSubscribe))
            );
            anyHooked = true;
        }
        apiListeners.add(listener);
    }

    /**
     * Unsubscribe the given instance from DiscordSRV events
     * @param listener the instance to unsubscribe DiscordSRV events from
     * @return whether the instance was a listener
     */
    public boolean unsubscribe(Object listener) {
        DiscordSRV.info(LangUtil.InternalMessage.API_LISTENER_UNSUBSCRIBED.toString()
                .replace("{listenername}", listener.getClass().getName())
        );
        return apiListeners.remove(listener);
    }

    /**
     * Call the given event to all subscribed API listeners
     * @param event the event to be called
     * @return the event that was called
     */
    public <E extends Event> E callEvent(E event) {
        for (ListenerPriority listenerPriority : ListenerPriority.values()) {
            for (Object apiListener : apiListeners) {
                for (Method method : apiListener.getClass().getMethods()) {
                    if (method.getParameters().length != 1)
                        continue; // api listener methods always take one parameter
                    if (!method.getParameters()[0].getType().isAssignableFrom(event.getClass()))
                        continue; // make sure this method wants this event

                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                    if (subscribeAnnotation == null) continue;

                    if (subscribeAnnotation.priority() != listenerPriority)
                        continue; // this priority isn't being called right now

                    invokeMethod(method, apiListener, event);
                }
            }
        }

        return event;
    }

    public void updateSlashCommands() {
        Set<PluginSlashCommand> commands = new HashSet<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin instanceof SlashCommandProvider) {
                SlashCommandProvider provider = (SlashCommandProvider) plugin;
                commands.addAll(provider.getSlashCommands());
            }
        }
        slashCommandProviders.forEach(p -> commands.addAll(p.getSlashCommands()));

        int conflictingCommands = 0;
        Map<String, PluginSlashCommand> conflictResolvedCommands = new HashMap<>();
        for (PluginSlashCommand pluginSlashCommand : commands) {
            String name = pluginSlashCommand.getCommandData().getName();
            PluginSlashCommand conflictingCommand = conflictResolvedCommands.putIfAbsent(name, pluginSlashCommand);
            if (conflictingCommand == null) continue;
            conflictingCommands++;
            if (pluginSlashCommand.getPriority().ordinal() > conflictingCommand.getPriority().ordinal())
                conflictResolvedCommands.put(name, pluginSlashCommand);
        }

        // update cached command data
        runningCommandData.clear();
        runningCommandData.addAll(conflictResolvedCommands.values());

        int cancelledGuilds = 0;
        Set<RestAction<List<Command>>> guildCommandUpdateActions = new HashSet<>();
        Set<CommandRegistrationError> errors = Collections.synchronizedSet(new HashSet<>());
        for (Guild guild : DiscordSRV.getPlugin().getJda().getGuilds()) {
            Set<CommandData> commandSet = conflictResolvedCommands.values().stream()
                    .filter(command -> command.isApplicable(guild))
                    .map(PluginSlashCommand::getCommandData)
                    .collect(Collectors.toSet());
            GuildSlashCommandUpdateEvent event = DiscordSRV.api.callEvent(new GuildSlashCommandUpdateEvent(guild, commandSet));
            if (event.isCancelled()) {
                cancelledGuilds++;
            } else {
                guildCommandUpdateActions.add(
                    guild.updateCommands().addCommands(commandSet).onErrorMap(throwable -> {
                        errors.add(new CommandRegistrationError(guild, throwable));
                        return null;
                    })
                );
            }
        }

        int finalCancelledGuilds = cancelledGuilds;
        int finalConflictingCommands = conflictingCommands;
        RestAction.allOf(guildCommandUpdateActions).queue(all -> {
            int successful = all.stream().filter(Objects::nonNull).mapToInt(List::size).sum();
            DiscordSRV.info("Successfully registered " + successful + " slash commands (" + finalConflictingCommands + " conflicted) for " + conflictResolvedCommands.values().stream().map(PluginSlashCommand::getPlugin).distinct().count() + " plugins in " + all.stream().filter(Objects::nonNull).count() + "/" + DiscordSRV.getPlugin().getJda().getGuilds().size() + " guilds (" + finalCancelledGuilds + " cancelled)");

            if (errors.isEmpty()) return;

            for (CommandRegistrationError error : errors) {
                Throwable exception = error.getException();
                Guild guild = error.getGuild();

                if (!(exception instanceof ErrorResponseException)) {
                    DiscordSRV.warning("Unexpected error adding slash commands in server " + guild.getName() + ": " + exception.toString());
                    continue;
                }

                ErrorResponseException errorResponseException = (ErrorResponseException) exception;
                ErrorResponse response = errorResponseException.getErrorResponse();
                if (response == ErrorResponse.MISSING_ACCESS) {
                    DiscordSRV.warning("Missing scopes in " + guild.getName() + " (" + guild.getId() + ")");
                } else {
                    DiscordSRV.warning("Failed to register slash commands in guild " + guild.getName() + " (" + guild.getId() + ") due to error: " + errorResponseException.getMeaning());
                }
            }
            DiscordSRV.error("Until this is fixed, plugin slash commands won't work properly in the specified guilds.");

            if (errors.stream().anyMatch(r -> r.getException() instanceof ErrorResponseException && ((ErrorResponseException) r.getException()).getErrorResponse() == ErrorResponse.MISSING_ACCESS)) {
                String invite = "https://scarsz.me/authorize#";
                try {
                    invite += DiscordSRV.getPlugin().getJda().getSelfUser().getApplicationId();
                } catch (IllegalStateException e) {
                    invite += DiscordSRV.getPlugin().getJda().retrieveApplicationInfo().complete().getId();
                }

                DiscordSRV.error("Re-authorize your bot at " + invite + " to the respective guild to grant the applications.commands slash commands scope.");
            }

            for (CommandRegistrationError error : errors) {
                DiscordSRV.debug(error.getException(), error.getGuild().toString());
            }
        });
    }

    /**
     * Add a provider for {@link PluginSlashCommand}s
     * @param provider the command data provider
     */
    public void addSlashCommandProvider(@NonNull SlashCommandProvider provider) {
        if (provider instanceof Plugin) return; // plugins are always registered
        this.slashCommandProviders.add(provider);
    }
    /**
     * Remove a {@link PluginSlashCommand} provider
     * @param provider the command data provider
     * @return whether the provider was previously registered
     */
    public boolean removeSlashCommandProvider(@NonNull SlashCommandProvider provider) {
        return this.slashCommandProviders.remove(provider);
    }

    /**
     * Event listener for JDA {@link SlashCommandEvent}. Automatically routes events to {@link SlashCommand}-annotated methods on registered command providers.
     */
    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        PluginSlashCommand commandData = runningCommandData.stream()
                .filter(command -> command.isApplicable(event.getGuild()))
                .filter(command -> command.getCommandData().getName().equals(event.getName()))
                .findFirst().orElse(null);
        if (commandData == null) return;

        Set<SlashCommandProvider> providers = new HashSet<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin instanceof SlashCommandProvider) {
                providers.add((SlashCommandProvider) plugin);
            }
        }
        providers.addAll(slashCommandProviders);

        for (SlashCommandPriority priority : SlashCommandPriority.values()) {
            for (SlashCommandProvider provider : providers) {
                handleSlashCommandEvent(provider, commandData, event, priority);
            }
        }

        ackCheck(event, commandData.getPlugin());
    }

    /**
     * Go through a {@link SlashCommandProvider} and invoke methods that listen to the provided slash command
     * @param provider the {@link SlashCommandProvider} to be searched and potentially invoked
     * @param commandData the {@link PluginSlashCommand} data associated with this {@link SlashCommandEvent}
     * @param event the {@link SlashCommandEvent} to be handled
     * @param priority only handlers with the given {@link SlashCommandPriority} will be invoked
     */
    private void handleSlashCommandEvent(SlashCommandProvider provider, PluginSlashCommand commandData, SlashCommandEvent event, SlashCommandPriority priority) {
        for (Method method : provider.getClass().getMethods()) {
            for (SlashCommand slashCommand : method.getAnnotationsByType(SlashCommand.class)) {
                if (slashCommand.priority() != priority) continue;
                if (!slashCommand.ignoreAcknowledged() && event.isAcknowledged()) continue;
                if (!GlobPattern.compile(slashCommand.path()).matches(event.getCommandPath())) continue;
                if (method.getParameters().length != 1 || !method.getParameters()[0].getType().equals(SlashCommandEvent.class)) continue;

                if (!slashCommand.deferReply()) {
                    invokeMethod(method, provider, event);
                } else {
                    event.deferReply(slashCommand.deferEphemeral())
                            .queue(hook -> invokeMethod(method, provider, event));
                }
            }
        }
    }

    /**
     * Invoke the given method on the given instance with the given args
     * @param method the method to invoke
     * @param instance the instance of the class to invoke on
     * @param args arguments for the method
     * @return whether the method executed without exception
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean invokeMethod(Method method, Object instance, Object... args) {
        // make sure method is accessible
        //noinspection deprecation
        if (!method.isAccessible()) method.setAccessible(true);

        try {
            method.invoke(instance, method.getParameterCount() == 0 ? null : args);
            return true;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            DiscordSRV.debug(instance.getClass().getName() + "#" + method.getName() + " threw an error: " + cause);
            if (!logException(method.getClass(), cause)) cause.printStackTrace();
        } catch (IllegalAccessException e) {
            // this should never happen
            DiscordSRV.error(
                    LangUtil.InternalMessage.API_LISTENER_METHOD_NOT_ACCESSIBLE.toString()
                            .replace("{listenername}", method.getClass().getName())
                            .replace("{methodname}", method.toString()),
                    e
            );
        }
        return false;
    }

    /**
     * Attempt to find the owning {@link Plugin} of the offending class and print the provided throwable to it's logger
     * @param offendingClass the offending plugin class
     * @param throwable throwable to print
     * @return whether the plugin was successfully determined
     */
    private boolean logException(Class<?> offendingClass, Throwable throwable) {
        try {
            ClassLoader classLoader = offendingClass.getClassLoader();
            if (classLoader instanceof PluginClassLoader) {
                Plugin owner = ((PluginClassLoader) classLoader).getPlugin();
                DiscordSRV.logThrowable(throwable, owner.getLogger()::severe);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Check if the given event is acknowledged. If not, prints an error shaming the given plugin
     * @param event the event to check
     * @param badPlugin the potentially bad plugin
     */
    private void ackCheck(SlashCommandEvent event, Plugin badPlugin) {
        if (!event.isAcknowledged()) {
            DiscordSRV.error(String.format(
                    "Slash command \"/%s\" was not acknowledged by %s's handler! The command will show as failed on Discord until this is fixed!",
                    event.getCommandPath().replace("/", " "),
                    badPlugin.getName()
            ));
        }
    }


    /**
     * <b>This must be executed before DiscordSRV's JDA is ready! (before DiscordSRV enables fully)</b><br/>
     * Some information will not be sent to us by Discord if not requested,
     * you can use this method to enable a gateway intent.
     *
     * <br/><br/>
     * Please not that DiscordSRV already uses some intents by default: {@link ApiManager#intents}.
     *
     * @throws IllegalStateException if this is executed after JDA was already initialized
     */
    public void requireIntent(GatewayIntent gatewayIntent) {
        if (DiscordSRV.getPlugin().getJda() != null) throw new IllegalStateException("Intents must be required before JDA initializes");
        intents.add(gatewayIntent);
        DiscordSRV.debug("Gateway intent " + gatewayIntent + " has been required through the API");
    }

    /**
     * <b>This must be executed before DiscordSRV's JDA is ready! (before DiscordSRV enables fully)</b><br/>
     * Some information will not be stored unless indicated,
     * you can use this method to enable a cache.
     *
     * <br/><br/>
     * Please note that DiscordSRV already uses some caches by default: {@link ApiManager#cacheFlags}.
     *
     * @throws IllegalStateException if this is executed after JDA was already initialized
     */
    public void requireCacheFlag(CacheFlag cacheFlag) {
        if (DiscordSRV.getPlugin().getJda() != null) throw new IllegalStateException("Cache flags must be required before JDA initializes");
        cacheFlags.add(cacheFlag);
        DiscordSRV.debug("Cache flag " + cacheFlag + " has been required through the API");
    }

    /**
     * Returns a immutable set of gateway intents DiscordSRV should use to initialize JDA.
     * @see ApiManager#requireIntent(GatewayIntent)
     */
    public Set<GatewayIntent> getIntents() {
        return Sets.immutableEnumSet(intents);
    }

    /**
     * Returns a immutable set of cache flags DiscordSRV should use to initialize JDA.
     * @see ApiManager#requireCacheFlag(CacheFlag)
     */
    public Set<CacheFlag> getCacheFlags() {
        return Sets.immutableEnumSet(cacheFlags);
    }

    /**
     * Returns true if <b>anything</b> has hooked into DiscordSRV's event bus.
     * Used internally in DiscordSRV.
     */
    public boolean isAnyHooked() {
        return anyHooked;
    }

}
