package github.scarsz.discordsrv.linking.impl.system;

import github.scarsz.discordsrv.linking.AccountSystem;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A memory-backed {@link AccountSystem}.
 * <strong>Not intended for actual usage. This system does not persist data.</strong>
 */
public class MemoryAccountSystem extends BaseAccountSystem {

    @Getter BidiMap<UUID, String> accounts = new DualHashBidiMap<>();
    @Getter BidiMap<String, UUID> linkingCodes = new ExpiringDualHashBidiMap<>(TimeUnit.MINUTES.toMillis(15));

    @Override
    public String getDiscordId(@NonNull UUID player) {
        return accounts.get(player);
    }

    @Override
    public UUID getUuid(@NonNull String discordId) {
        return accounts.getKey(discordId);
    }

    @Override
    public void setLinkedDiscord(@NonNull UUID uuid, @Nullable String discordId) {
        if (discordId != null) {
            accounts.put(uuid, discordId);
            callAccountLinkedEvent(discordId, uuid);
        } else {
            String previousDiscordId = getDiscordId(uuid);
            accounts.remove(uuid);
            if (previousDiscordId != null) {
                callAccountUnlinkedEvent(previousDiscordId, uuid);
            }
        }
    }

    @Override
    public void setLinkedMinecraft(@NonNull String discordId, @Nullable UUID uuid) {
        UUID previousPlayer = accounts.getKey(discordId);
        accounts.removeValue(discordId);
        if (uuid != null) {
            accounts.put(uuid, discordId);
            callAccountLinkedEvent(discordId, uuid);
        } else {
            if (previousPlayer != null) {
                callAccountUnlinkedEvent(discordId, previousPlayer);
            }
        }
    }

    @Override
    public UUID lookupCode(String code) {
        return linkingCodes.get(code);
    }

    @Override
    public void storeLinkingCode(@NonNull String code, @NonNull UUID uuid) {
        linkingCodes.put(code, uuid);
    }

}
