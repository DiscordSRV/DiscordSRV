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

package github.scarsz.discordsrv.objects;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.lang.ref.WeakReference;
import java.util.*;

public class ExpiringDualHashBidiMap<K, V> extends DualHashBidiMap<K, V> {

    private final HashMap<K, Long> expiryTimes = new HashMap<>();
    private final long expiryDelay;

    public ExpiringDualHashBidiMap(long expiryDelayMillis) {
        this.expiryDelay = expiryDelayMillis;
        ExpiryThread.references.add(new WeakReference<>(this));
    }

    @Override
    public V put(K key, V value) {
        synchronized (expiryTimes) {
            expiryTimes.put(key, System.currentTimeMillis() + expiryDelay);
        }
        return super.put(key, value);
    }

    @SuppressWarnings("UnusedReturnValue")
    public V putNotExpiring(K key, V value) {
        return super.put(key, value);
    }

    public V putExpiring(K key, V value, long expiryTime) {
        if (expiryTime < System.currentTimeMillis()) throw new IllegalArgumentException("The expiry time must be in the future");
        synchronized (expiryTimes) {
            expiryTimes.put(key, expiryTime);
        }
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        synchronized (expiryTimes) {
            expiryTimes.remove(key);
        }
        return super.remove(key);
    }

    @Override
    public K removeValue(Object value) {
        K key = getKey(value);
        if (key != null) {
            synchronized (expiryTimes) {
                expiryTimes.remove(key);
            }
        }
        return super.removeValue(value);
    }

    public long getExpiryTime(K key) {
        if (!containsKey(key)) throw new IllegalArgumentException("The given key is not in the map");
        return expiryTimes.get(key);
    }

    public void setExpiryTime(K key, long expiryTimeMillis) {
        if (!containsKey(key)) throw new IllegalArgumentException("The given key is not in the map");
        expiryTimes.put(key, expiryTimeMillis);
    }

    public long getExpiryDelay() {
        return expiryDelay;
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    private void keyExpired(Object key) {
        remove(key);
        expiryTimes.remove(key);
    }

    public static class ExpiryThread extends Thread {

        private static final Set<WeakReference<ExpiringDualHashBidiMap<?, ?>>> references = new HashSet<>();

        private ExpiryThread() {
            super("DiscordSRV " + ExpiryThread.class.getSimpleName());
            Runtime.getRuntime().addShutdownHook(new Thread(this::interrupt));
        }

        @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
        @Override
        public void run() {
            while (!isInterrupted()) {
                long currentTime = System.currentTimeMillis();
                for (WeakReference<ExpiringDualHashBidiMap<?, ?>> reference : new HashSet<>(references)) {
                    final ExpiringDualHashBidiMap<?, ?> collection = reference.get();
                    if (collection == null) {
                        references.remove(reference);
                        continue;
                    }
                    Map<?, Long> expiryTimes;
                    synchronized (collection.expiryTimes) {
                        expiryTimes = new HashMap<>(collection.expiryTimes);
                    }
                    List<Object> removals = new ArrayList<>();
                    expiryTimes.entrySet().stream()
                            .filter(entry -> entry.getValue() < currentTime)
                            .forEach(entry -> removals.add(entry.getKey()));
                    synchronized (collection) {
                        removals.forEach(collection::keyExpired);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }

        static {
            new ExpiryThread().start();
        }

    }

}
