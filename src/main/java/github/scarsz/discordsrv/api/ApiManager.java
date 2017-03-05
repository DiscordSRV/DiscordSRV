package github.scarsz.discordsrv.api;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.Event;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The manager of all of DiscordSRV's API related functionality.
 *
 * @see #subscribe(Object) subscribe listener
 * @see #unsubscribe(Object) unsubscribe listener
 * @see #callEvent(Event) call an event
 */
public class ApiManager {

    private List<Object> apiListeners = new ArrayList<>();

    /**
     * Subscribe the given instance to DiscordSRV events
     * @param listener the instance to subscribe DiscordSRV events to
     * @throws RuntimeException if the object has zero methods that are annotated with @github.scarsz.discordsrv.api.Subscribe
     */
    public void subscribe(Object listener) {
        // ensure at least one method available in given object that is annotated with Subscribe
        int methodsAnnotatedSubscribe = 0;
        for (Method method : listener.getClass().getMethods()) if (method.isAnnotationPresent(Subscribe.class)) methodsAnnotatedSubscribe++;
        if (methodsAnnotatedSubscribe == 0) throw new RuntimeException(listener.getClass().getName() + " attempted DiscordSRV API registration but no methods inside of it were annotated @Subscribe (github.scarsz.discordsrv.api.Subscribe)");

        DiscordSRV.info("API listener " + listener.getClass().getName() + " subscribed (" + methodsAnnotatedSubscribe + " methods)");
        if (!apiListeners.contains(listener)) apiListeners.add(listener);
    }

    /**
     * Unsubscribe the given instance from DiscordSRV events
     * @param listener the instance to unsubscribe DiscordSRV events from
     * @return whether or not the instance was a listener
     */
    public boolean unsubscribe(Object listener) {
        DiscordSRV.info("API listener " + listener.getClass().getName() + " unsubscribed");
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
                    if (method.isAnnotationPresent(Subscribe.class)) continue; // make sure method has a subscribe annotation somewhere

                    for (Annotation annotation : method.getAnnotations()) {
                        if (!(annotation instanceof Subscribe)) continue; // go through all the annotations until we get one of ours

                        Subscribe subscribeAnnotation = (Subscribe) annotation;
                        if (subscribeAnnotation.priority() != listenerPriority) continue; // this priority isn't being called right now

                        // make sure method is accessible
                        if (!method.isAccessible()) method.setAccessible(true);

                        try {
                            method.invoke(apiListener, event);
                        } catch (InvocationTargetException e) {
                            DiscordSRV.error("DiscordSRV API Listener " + apiListener.getClass().getName() + " threw an error:\n" + ExceptionUtils.getStackTrace(e));
                        } catch (IllegalAccessException e) {
                            // this should never happen
                            DiscordSRV.error("DiscordSRV API Listener " + apiListener.getClass().getName() + " method " + method.toString() + " was inaccessible despite efforts to make it accessible\n" + ExceptionUtils.getStackTrace(e));
                        }
                    }
                }
            }
        }

        return event;
    }

}
