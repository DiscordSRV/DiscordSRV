/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.api;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.Event;
import github.scarsz.discordsrv.util.LangUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
 */
@SuppressWarnings("unused")
public class ApiManager {

    private List<Object> apiListeners = new ArrayList<>();
    private boolean anyHooked = false;

    /**
     * Subscribe the given instance to DiscordSRV events
     * @param listener the instance to subscribe DiscordSRV events to
     * @throws RuntimeException if the object has zero methods that are annotated with {@link Subscribe}
     */
    public void subscribe(Object listener) {
        // ensure at least one method available in given object that is annotated with Subscribe
        int methodsAnnotatedSubscribe = 0;
        for (Method method : listener.getClass().getMethods()) if (method.isAnnotationPresent(Subscribe.class)) methodsAnnotatedSubscribe++;
        if (methodsAnnotatedSubscribe == 0) throw new RuntimeException(listener.getClass().getName() + " attempted DiscordSRV API registration but no methods inside of it were annotated @Subscribe (github.scarsz.discordsrv.api.Subscribe)");

        DiscordSRV.info(LangUtil.InternalMessage.API_LISTENER_SUBSCRIBED.toString()
                .replace("{listenername}", listener.getClass().getName())
                .replace("{methodcount}", String.valueOf(methodsAnnotatedSubscribe))
        );
        if (!apiListeners.contains(listener)) apiListeners.add(listener);
        anyHooked = true;
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
                            DiscordSRV.error((LangUtil.InternalMessage.API_LISTENER_THREW_ERROR + ":\n" + ExceptionUtils.getStackTrace(e))
                                    .replace("{listenername}", apiListener.getClass().getName())
                            );
                        } catch (IllegalAccessException e) {
                            // this should never happen
                            DiscordSRV.error((LangUtil.InternalMessage.API_LISTENER_METHOD_NOT_ACCESSIBLE + ":\n" + ExceptionUtils.getStackTrace(e))
                                    .replace("{listenername}", apiListener.getClass().getName())
                                    .replace("{methodname}", method.toString())
                            );
                        }
                    }
                }
            }
        }

        return event;
    }

    /**
     * Internal method to see if anything has hooked to DiscordSRV's API
     */
    public boolean isAnyHooked() {
        return anyHooked;
    }
}
