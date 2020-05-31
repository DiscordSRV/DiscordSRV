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

package github.scarsz.discordsrv.objects;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.lang.ref.WeakReference;
import java.util.*;

public class ExpiringDualHashBidiMap<K, V> extends DualHashBidiMap<K, V> {

    private final HashMap<K, Long> creationTimes = new HashMap<>();
    private final long expireAfterMs;

    public ExpiringDualHashBidiMap(long expireAfterMs) {
        this.expireAfterMs = expireAfterMs;
        ExpiryThread.references.add(new WeakReference<>(this));
    }

    @Override
    public V put(K key, V value) {
        synchronized (creationTimes) {
            creationTimes.put(key, System.currentTimeMillis());
        }
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        synchronized (creationTimes) {
            creationTimes.remove(key);
        }
        return super.remove(key);
    }

    @Override
    public K removeValue(Object value) {
        K key = getKey(value);
        if (key != null) {
            synchronized (creationTimes) {
                creationTimes.remove(key);
            }
        }
        return super.removeValue(value);
    }

    public static class ExpiryThread extends Thread {

        private static final Set<WeakReference<ExpiringDualHashBidiMap<?, ?>>> references = new HashSet<>();

        private ExpiryThread() {
            super("DiscordSRV " + ExpiryThread.class.getSimpleName());
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                long currentTime = System.currentTimeMillis();
                for (WeakReference<ExpiringDualHashBidiMap<?, ?>> reference : new HashSet<>(references)) {
                    ExpiringDualHashBidiMap<?, ?> collection = reference.get();
                    if (collection == null) {
                        references.remove(reference);
                        continue;
                    }
                    Map<?, Long> creationTimes;
                    synchronized (collection.creationTimes) {
                        creationTimes = new HashMap<>(collection.creationTimes);
                    }
                    List<Object> removals = new ArrayList<>();
                    creationTimes.entrySet().stream()
                            .filter(entry -> entry.getValue() + collection.expireAfterMs < currentTime)
                            .forEach(entry -> removals.add(entry.getKey()));
                    synchronized (collection) {
                        removals.forEach(collection::remove);
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
