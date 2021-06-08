package github.scarsz.discordsrv.linking.impl.system.sql;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

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
    public String getIfCached(UUID playerUuid) {
        return playerCache.getIfPresent(playerUuid);
    }
    @Override
    public UUID getIfCached(String discordId) {
        return discordCache.getIfPresent(discordId);
    }

}
