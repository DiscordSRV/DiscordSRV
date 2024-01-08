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

package org.ricetea.discordsrv;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Immutable
@ThreadSafe
public final class JBUser implements Iterable<UUID> {

    @Nonnull
    public static final JBUser EMPTY = new JBUser(new UUID[0]);

    @Nonnull
    private final UUID[] uuids;

    @Nonnull
    public static JBUser of(@Nullable Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty())
            return EMPTY;
        UUID uuid1, uuid2;
        Iterator<UUID> iterator = uuids.iterator();
        uuid1 = iterator.next();
        uuid2 = iterator.hasNext() ? iterator.next() : null;
        return uuid2 == null ? new JBUser(uuid1) : new JBUser(uuid1, uuid2);
    }

    @Nonnull
    public static JBUser of(@Nullable UUID[] uuids) {
        if (uuids == null)
            return EMPTY;
        return switch (uuids.length) {
            case 0 -> EMPTY;
            case 1 -> new JBUser(uuids[0]);
            default -> new JBUser(uuids[0], uuids[1]);
        };
    }

    public JBUser(@Nonnull UUID uuid) {
        this(new UUID[]{uuid});
    }

    public JBUser(@Nonnull UUID uuid1, @Nonnull UUID uuid2) {
        this(new UUID[]{uuid1, uuid2});
    }

    private JBUser(@Nonnull UUID[] uuids) {
        this.uuids = uuids;
    }

    private int indexOf(@Nonnull UUID uuid) {
        int length = uuids.length;
        for (int i = 0; i < length; i++) {
            if (Objects.equals(uuids[i], uuid)) {
                return i;
            }
        }
        return -1;
    }

    private int indexOf(@Nonnull Predicate<UUID> predicate) {
        int length = uuids.length;
        for (int i = 0; i < length; i++) {
            if (predicate.test(uuids[i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(@Nonnull UUID uuid) {
        return indexOf(uuid) >= 0;
    }

    @Nonnull
    public JBUser put(@Nonnull UUID uuid) {
        if (isEmpty())
            return new JBUser(uuid);
        int length = uuids.length;
        if (isBedrockUUID(uuid)) {
            int indexOf = indexOf(JBUser::isBedrockUUID);
            if (indexOf < 0)
                return new JBUser(uuids[0], uuid);
            else {
                if (length == 1)
                    return new JBUser(uuid);
                else
                    return new JBUser(uuids[indexOf == 0 ? 1 : 0], uuid);
            }
        } else {
            int indexOf = indexOf(Predicate.not(JBUser::isBedrockUUID));
            if (indexOf < 0)
                return new JBUser(uuid, uuids[0]);
            else {
                if (length == 1)
                    return new JBUser(uuid);
                else
                    return new JBUser(uuid, uuids[indexOf == 0 ? 1 : 0]);
            }
        }
    }

    @Nonnull
    public JBUser remove(@Nonnull UUID uuid) {
        int length = uuids.length;
        if (length == 0)
            return this;
        int indexOf = indexOf(uuid);
        if (indexOf < 0) {
            return this;
        } else if (length == 2) {
            return new JBUser(uuids[indexOf == 0 ? 1 : 0]);
        } else {
            return EMPTY;
        }
    }

    @Nullable
    public UUID testReplace(@Nonnull UUID uuid) {
        int indexOf;
        if (isBedrockUUID(uuid)) {
            indexOf = indexOf(JBUser::isBedrockUUID);
        } else {
            indexOf = indexOf(Predicate.not(JBUser::isBedrockUUID));
        }
        return indexOf < 0 ? null : uuids[indexOf];
    }

    public boolean isEmpty() {
        return this == EMPTY || uuids.length == 0;
    }

    public boolean hasBedrockUUID() {
        return indexOf(JBUser::isBedrockUUID) >= 0;
    }

    public boolean hasJavaUUID() {
        return indexOf(Predicate.not(JBUser::isBedrockUUID)) >= 0;
    }

    public static boolean isBedrockUUID(UUID uuid) {
        return uuid.getMostSignificantBits() == 1;
    }

    @Nonnull
    public Stream<UUID> stream() {
        return Arrays.stream(uuids);
    }

    @Nonnull
    public Stream<UUID> parallelStream() {
        return Arrays.stream(uuids).parallel();
    }

    @Override
    @Nonnull
    public Iterator<UUID> iterator() {
        return stream().iterator();
    }

    @Override
    public void forEach(Consumer<? super UUID> forEachConsumer) {
        Objects.requireNonNull(forEachConsumer);
        for (UUID uuid : uuids) {
            forEachConsumer.accept(uuid);
        }
    }

    public int size() {
        return uuids.length;
    }

    public boolean equals(Object another) {
        if (this == another)
            return true;
        if (another instanceof JBUser user) {
            if (isEmpty())
                return user.isEmpty();
            UUID[] uuids = this.uuids;
            UUID[] anotherUUIDs = user.uuids;
            int length = uuids.length;
            if (length == anotherUUIDs.length) {
                if (length == 1) {
                    return Objects.equals(uuids[0], anotherUUIDs[0]);
                } else {
                    return Objects.equals(uuids[0], anotherUUIDs[0]) && Objects.equals(uuids[1], anotherUUIDs[1]) ||
                            Objects.equals(uuids[1], anotherUUIDs[0]) && Objects.equals(uuids[0], anotherUUIDs[1]);
                }
            }
        }
        return false;
    }

    @Nonnull
    public UUID[] toArray() {
        return uuids.clone();
    }

    @Nonnull
    public List<UUID> toList() {
        return stream().toList();
    }
}
