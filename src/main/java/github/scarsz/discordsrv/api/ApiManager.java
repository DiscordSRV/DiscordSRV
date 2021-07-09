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
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.Event;
import github.scarsz.discordsrv.util.LangUtil;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

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
 */
@SuppressWarnings("unused")
public class ApiManager {

    private final List<Object> apiListeners = new CopyOnWriteArrayList<>();
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
     * @throws RuntimeException if the object has zero methods that are annotated with {@link Subscribe}
     */
    public void subscribe(Object listener) {
        // ensure at least one method available in given object that is annotated with Subscribe
        int methodsAnnotatedSubscribe = 0;
        for (Method method : listener.getClass().getMethods()) if (method.isAnnotationPresent(Subscribe.class)) methodsAnnotatedSubscribe++;
        if (methodsAnnotatedSubscribe == 0) throw new RuntimeException(listener.getClass().getName() + " attempted DiscordSRV API registration but no public methods inside of it were annotated @Subscribe (github.scarsz.discordsrv.api.Subscribe)");

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
     * @return whether or not the instance was a listener
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
                    if (method.getParameters().length != 1) continue; // api listener methods always take one parameter
                    if (!method.getParameters()[0].getType().isAssignableFrom(event.getClass())) continue; // make sure the event wants this event
                    if (!method.isAnnotationPresent(Subscribe.class)) continue; // make sure method has a subscribe annotation somewhere

                    for (Annotation annotation : method.getAnnotations()) {
                        if (!(annotation instanceof Subscribe)) continue; // go through all the annotations until we get one of ours

                        Subscribe subscribeAnnotation = (Subscribe) annotation;
                        if (subscribeAnnotation.priority() != listenerPriority) continue; // this priority isn't being called right now

                        // make sure method is accessible
                        if (!method.isAccessible()) method.setAccessible(true);

                        try {
                            method.invoke(apiListener, event);
                        } catch (InvocationTargetException e) {
                            DiscordSRV.error(
                                    LangUtil.InternalMessage.API_LISTENER_THREW_ERROR.toString()
                                            .replace("{listenername}", apiListener.getClass().getName()),
                                    e.getCause());
                        } catch (IllegalAccessException e) {
                            // this should never happen
                            DiscordSRV.error(
                                    LangUtil.InternalMessage.API_LISTENER_METHOD_NOT_ACCESSIBLE.toString()
                                            .replace("{listenername}", apiListener.getClass().getName())
                                            .replace("{methodname}", method.toString()),
                                    e
                            );
                        }
                    }
                }
            }
        }

        return event;
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
