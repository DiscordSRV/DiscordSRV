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
