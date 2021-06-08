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

package github.scarsz.discordsrv.linking.impl.system.sql;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class CachedSqlAccountSystem extends SqlAccountSystem {

    @Getter private final LoadingCache<UUID, String> playerCache = Caffeine.newBuilder()
            .refreshAfterWrite(10, TimeUnit.SECONDS)
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build(this::getDiscordId);

    @Getter private final LoadingCache<String, UUID> discordCache = Caffeine.newBuilder()
            .refreshAfterWrite(10, TimeUnit.SECONDS)
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build(this::getUuid);

    @Override
    @SneakyThrows
    public String getDiscordId(@NotNull UUID playerUuid) {
        String discordId = playerCache.get(playerUuid);
        if (discordId != null) discordCache.put(discordId, playerUuid);
        return discordId;
    }
    @Override
    @SneakyThrows
    public UUID getUuid(@NotNull String discordId) {
        UUID uuid = discordCache.get(discordId);
        if (uuid != null) playerCache.put(uuid, discordId);
        return uuid;
    }

    @Override
    public void cacheLink(UUID playerUuid, String discordId) {
        playerCache.put(playerUuid, discordId);
        discordCache.put(discordId, playerUuid);
    }

    @Override
    public boolean isCaching() {
        return true;
    }

    @Override
    public boolean isInCache(UUID playerUuid) {
        return playerCache.getIfPresent(playerUuid) != null;
    }
    @Override
    public boolean isInCache(String discordId) {
        return discordCache.getIfPresent(discordId) != null;
    }

    @Override
    public @Nullable String getIfCached(UUID playerUuid) {
        return playerCache.getIfPresent(playerUuid);
    }
    @Override
    public @Nullable UUID getIfCached(String discordId) {
        return discordCache.getIfPresent(discordId);
    }

}
