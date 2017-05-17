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
    public Event callEvent(Event event) {
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

}
