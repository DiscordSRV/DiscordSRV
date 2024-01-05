/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.objects;

import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

public class CancellationDetector<E extends Cancellable> {

    private final Plugin plugin;
    private final Class<?> eventClass;
    private final BiConsumer<RegisteredListener, E> cancelListener;
    private final ThreadLocal<Boolean> cancelled = ThreadLocal.withInitial(() -> false);
    private final EnumMap<EventPriority, CancellationDetectorList<E>> proxies = new EnumMap<>(EventPriority.class);
    private EnumMap<EventPriority, ArrayList<RegisteredListener>> originalMap;

    public CancellationDetector(Plugin plugin, @NotNull Class<E> eventClass, BiConsumer<RegisteredListener, E> cancelListener) {
        this.plugin = plugin;
        this.eventClass = eventClass;
        this.cancelListener = cancelListener;
        inject();
    }

    public void close() {
        if (originalMap == null) {
            return;
        }

        try {
            for (Map.Entry<EventPriority, ArrayList<RegisteredListener>> entry : originalMap.entrySet()) {
                ArrayList<RegisteredListener> list = entry.getValue();
                list.clear();
                list.addAll(proxies.get(entry.getKey()).getRaw());
            }

            HandlerList handlerList = getHandlerList();
            setSlots(handlerList, originalMap);

            // Reset the handlers so it gets them again
            Field handlers = handlerList.getClass().getDeclaredField("handlers");
            handlers.setAccessible(true);
            handlers.set(handlerList, null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to clean up handler list.", e);
        }

        originalMap = null;
    }

    private void inject() {
        HandlerList handlerList = getHandlerList();
        EnumMap<EventPriority, ArrayList<RegisteredListener>> slots = getSlots(handlerList);
        originalMap = slots.clone();

        for (EventPriority eventPriority : slots.keySet()) {
            List<RegisteredListener> original = originalMap.get(eventPriority);
            CancellationDetectorList<E> proxy = new CancellationDetectorList<>(original, this);

            slots.put(eventPriority, proxy);
            proxies.put(eventPriority, proxy);
        }
    }

    private HandlerList getHandlerList() {
        Class<?> currentClass = eventClass;
        while (currentClass != null && Event.class.isAssignableFrom(currentClass)) {
            try {
                Method method = currentClass.getDeclaredMethod("getHandlerList");
                if (!method.isAccessible()) method.setAccessible(true);
                return (HandlerList) method.invoke(null);
            } catch (NoSuchMethodException ignored) {
                currentClass = currentClass.getSuperclass();
            } catch (Throwable e) {
                throw new RuntimeException("Could not get HandlerList", e);
            }
        }
        throw new RuntimeException("Unable to find HandlerList");
    }

    public void setSlots(HandlerList handlerList, EnumMap<EventPriority, ArrayList<RegisteredListener>> slots) {
        try {
            getSlotsField().set(handlerList, slots);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Unable to set handlerslots field", e);
        }
    }

    @SuppressWarnings("unchecked")
    public EnumMap<EventPriority, ArrayList<RegisteredListener>> getSlots(HandlerList handlerList) {
        try {
            return (EnumMap<EventPriority, ArrayList<RegisteredListener>>) getSlotsField().get(handlerList);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to get handlerslots field", e);
        }
    }

    private static Field getSlotsField() throws NoSuchFieldException {
        Field field = HandlerList.class.getDeclaredField("handlerslots");
        if (!field.isAccessible()) field.setAccessible(true);
        return field;
    }

    public static class CancellationDetectorList<E extends Cancellable> extends ArrayList<RegisteredListener> {

        private final CancellationDetector<E> detector;

        public CancellationDetectorList(Collection<RegisteredListener> original, CancellationDetector<E> detector) {
            super(original);
            this.detector = detector;
        }

        private List<RegisteredListener> getRaw() {
            // Avoid #toArray
            Iterator<RegisteredListener> iterator = iterator();
            List<RegisteredListener> listeners = new ArrayList<>();
            while (iterator.hasNext()) {
                listeners.add(iterator.next());
            }
            return listeners;
        }

        private List<RegisteredListener> getListeners() {
            List<RegisteredListener> listeners = new ArrayList<>();
            listeners.add(new CancellationDetectingListener<>(null, detector));
            for (RegisteredListener listener : this) {
                listeners.add(listener);
                listeners.add(new CancellationDetectingListener<>(listener, detector));
            }
            return listeners;
        }

        @Override
        public Object @NotNull [] toArray() {
            return getListeners().toArray();
        }

        @SuppressWarnings("SuspiciousToArrayCall")
        @Override
        public <T> T @NotNull [] toArray(T[] a) {
            return getListeners().toArray(a);
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof CancellationDetectingListener) {
                // Prevent adding these to the collection
                return true;
            }
            return super.remove(o);
        }

        @Override
        public boolean add(RegisteredListener registeredListener) {
            if (registeredListener instanceof CancellationDetectingListener) {
                // Prevent adding these to the collection
                return true;
            }
            return super.add(registeredListener);
        }
    }

    public static class CancellationDetectingListener<E extends Cancellable> extends RegisteredListener {

        private final RegisteredListener listener;
        private final CancellationDetector<E> detector;

        public CancellationDetectingListener(
                RegisteredListener listener,
                CancellationDetector<E> detector
        ) {
            super(
                    new Listener() {},
                    (l, e) -> {},
                    listener != null ? listener.getPriority() : EventPriority.LOWEST,
                    detector.plugin,
                    false
            );
            this.listener = listener;
            this.detector = detector;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void callEvent(@NotNull Event event) throws EventException {
            boolean cancelled = event instanceof Cancellable && ((Cancellable) event).isCancelled();
            boolean wasCancelled = detector.cancelled.get();

            if (cancelled && !wasCancelled && listener != null) {
                detector.cancelListener.accept(listener, (E) event);
            }
            if (cancelled != wasCancelled) {
                this.detector.cancelled.set(cancelled);
            }
        }
    }
}
