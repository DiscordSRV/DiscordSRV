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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class BidiMultimap<K, V> implements Multimap<K, V> {

    @Nonnull
    private final Multimap<K, V> map;

    @Nonnull
    private final Map<V, K> inversedMap;

    @Nonnull
    public static <K, V> BidiMultimap<K, V> create(@Nonnull Supplier<Multimap<K, V>> mapGenerator) {
        return new BidiMultimap<>(mapGenerator, HashMap::new);
    }

    @Nonnull
    public static <K, V> BidiMultimap<K, V> create(@Nonnull Supplier<Multimap<K, V>> mapGenerator, @Nonnull Supplier<Map<V, K>> inversedMapGenerator) {
        return new BidiMultimap<>(mapGenerator, inversedMapGenerator);
    }

    private BidiMultimap(@Nonnull Supplier<Multimap<K, V>> mapGenerator, @Nonnull Supplier<Map<V, K>> inversedMapGenerator) {
        this.map = mapGenerator.get();
        this.inversedMap = inversedMapGenerator.get();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        if (key == null)
            return false;
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        if (value == null)
            return false;
        return inversedMap.containsKey(value);
    }

    @Override
    public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
        return map.containsEntry(key, value);
    }

    @Override
    public boolean put(K key, V value) {
        K oldKey = inversedMap.remove(value);
        if (oldKey != null) {
            map.remove(oldKey, value);
        }
        if (map.put(key, value)) {
            inversedMap.put(value, key);
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
        return map.remove(key, value) || inversedMap.remove(value, key);
    }

    @Override
    public boolean putAll(K key, Iterable<? extends V> values) {
        boolean result = map.putAll(key, values);
        for (V value : values) {
            inversedMap.put(value, key);
        }
        return result;
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
        return false;
    }

    @Override
    public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
        Collection<V> result = map.replaceValues(key, values);
        for (V value : result) {
            inversedMap.remove(value, key);
        }
        for (V value : values) {
            inversedMap.put(value, key);
        }
        return result;
    }

    @Override
    public Collection<V> removeAll(@Nullable Object key) {
        Collection<V> result = map.removeAll(key);
        for (V value : result) {
            inversedMap.remove(value, key);
        }
        return result;
    }

    public void removeValue(@Nullable V value) {
        if (value == null)
            return;
        K key = inversedMap.remove(value);
        if (key == null)
            return;
        map.remove(key, value);
    }

    @Override
    public void clear() {
        map.clear();
        inversedMap.clear();
    }

    @Override
    public Collection<V> get(K key) {
        return map.get(key);
    }

    public K getKey(V value) {
        return inversedMap.get(value);
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Multiset<K> keys() {
        return map.keys();
    }

    @Override
    public Collection<V> values() {
        return inversedMap.keySet();
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return map.entries();
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return map.asMap();
    }
}
