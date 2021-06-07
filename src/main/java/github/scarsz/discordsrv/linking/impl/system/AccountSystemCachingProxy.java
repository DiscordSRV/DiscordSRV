package github.scarsz.discordsrv.linking.impl.system;

import github.scarsz.discordsrv.linking.AccountSystem;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implements a proxy for other {@link AccountSystem}s that caches values
 * for a given timeframe before re-querying the backing account system.
 */
public class AccountSystemCachingProxy<B extends AccountSystem> extends BaseAccountSystem {

    @Getter private final B base;
    private final ExpiringDualHashBidiMap<UUID, String> cache;

    public AccountSystemCachingProxy(B base) {
        this(base, 10, TimeUnit.SECONDS);
    }
    public AccountSystemCachingProxy(B base, long expiration, TimeUnit expirationUnit) {
        this.base = base;
        this.cache = new ExpiringDualHashBidiMap<>(expirationUnit.toMillis(expiration));
    }

    /**
     * Directly query the backing {@link AccountSystem} for the current Discord ID of the given player.
     * <strong>This is potentially a very expensive operation and should be used sparingly.</strong>
     * @param player the player to query
     * @return the Discord ID of the queried player, null if not linked
     */
    public @Nullable String queryDiscordId(@NotNull UUID player) {
        return base.getDiscordId(player);
    }
    @Override
    public String getDiscordId(@NotNull UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, uuid -> queryDiscordId(playerUuid));
    }

    /**
     * Directly query the backing {@link AccountSystem} for the current UUID of the given Discord ID.
     * <strong>This is potentially a very expensive operation and should be used sparingly.</strong>
     * @param userId the Discord user ID to query
     * @return the UUID of the queried user, null if not linked
     */
    public @Nullable UUID queryUuid(@NotNull String userId) {
        return base.getUuid(userId);
    }
    @Override
    public UUID getUuid(@NotNull String userId) {
        return cache.inverseBidiMap().computeIfAbsent(userId, s -> queryUuid(userId));
    }

    @Override
    public String toString() {
        return "CachedAccountSystem{" +
                "base=" + base +
                ", cached=" + cache.size() +
                '}';
    }

    @Override
    public @NotNull Map<String, UUID> getLinkingCodes() {
        return base.getLinkingCodes();
    }
    @Override
    public UUID lookupCode(String code) {
        return base.lookupCode(code);
    }
    @Override
    public void storeLinkingCode(@NotNull String code, @NotNull UUID playerUuid) {
        base.storeLinkingCode(code, playerUuid);
    }
    @Override
    public void setLinkedDiscord(@NotNull UUID playerUuid, @Nullable String discordId) {
        base.setLinkedDiscord(playerUuid, discordId);
        cache.put(playerUuid, discordId);
    }
    @Override
    public void setLinkedMinecraft(@NotNull String discordId, @Nullable UUID playerUuid) {
        base.setLinkedMinecraft(discordId, playerUuid);
        cache.inverseBidiMap().put(discordId, playerUuid);
    }

}
