package github.scarsz.discordsrv.linking.impl.system;

import github.scarsz.discordsrv.linking.AccountSystem;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implements a proxy for other {@link AccountSystem}s that caches values
 * for a given timeframe before re-querying the backing account system.
 */
public class AccountSystemCachingProxy extends BaseAccountSystem {

    private final AccountSystem base;
    private final ExpiringDualHashBidiMap<UUID, String> cache;

    public AccountSystemCachingProxy(AccountSystem base) {
        this(base, 10, TimeUnit.SECONDS);
    }
    public AccountSystemCachingProxy(AccountSystem base, long expiration, TimeUnit expirationUnit) {
        this.base = base;
        this.cache = new ExpiringDualHashBidiMap<>(expirationUnit.toMillis(expiration));
    }

    @Override
    public String getDiscordId(@NonNull UUID player) {
        return cache.computeIfAbsent(player, uuid -> base.getDiscordId(player));
    }
    @Override
    public UUID getUuid(@NonNull String userId) {
        return cache.inverseBidiMap().computeIfAbsent(userId, s -> base.getUuid(userId));
    }

    @Override
    public String toString() {
        return "CachedAccountSystem{" +
                "base=" + base +
                ", cached=" + cache.size() +
                '}';
    }

    @Override
    public @NonNull Map<String, UUID> getLinkingCodes() {
        return base.getLinkingCodes();
    }
    @Override
    public UUID lookupCode(String code) {
        return base.lookupCode(code);
    }
    @Override
    public void storeLinkingCode(@NonNull String code, @NonNull UUID playerUuid) {
        base.storeLinkingCode(code, playerUuid);
    }
    @Override
    public void setLinkedDiscord(@NonNull UUID playerUuid, @Nullable String discordId) {
        base.setLinkedDiscord(playerUuid, discordId);
        cache.put(playerUuid, discordId);
    }
    @Override
    public void setLinkedMinecraft(@NonNull String discordId, @Nullable UUID playerUuid) {
        base.setLinkedMinecraft(discordId, playerUuid);
        cache.inverseBidiMap().put(discordId, playerUuid);
    }

}
