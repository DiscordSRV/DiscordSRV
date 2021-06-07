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

package github.scarsz.discordsrv.linking.store;

import lombok.NonNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;

public interface CodeStore {

    /**
     * @return map of currently active linking codes and their associated Minecraft UUID
     */
    @NonNull Map<String, UUID> getLinkingCodes();

    /**
     * Gets the UUID associated with the given code
     * @param code code to query
     * @return UUID associated with this code if present otherwise null
     */
    UUID lookupCode(String code);

    /**
     * Checks whether the given linking code is currently allocated to a player
     * @param code the code to check
     * @return whether or not the code is currently allocated
     */
    default boolean isCodeAllocated(String code) {
        return lookupCode(code) != null;
    }

    /**
     * Generate a code to be used for linking the given UUID
     * @param playerUuid the UUID to generate a code for
     * @return the generated, six-character code
     */
    default @NonNull String generateLinkingCode(@NonNull UUID playerUuid) {
        String code;
        do {
            code = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (isCodeAllocated(code));

        storeLinkingCode(code, playerUuid);

        return code;
    }

    /**
     * Stores the given Code-UUID pair into the {@link CodeStore}
     * @param code the linking code
     * @param playerUuid the target uuid
     */
    void storeLinkingCode(@NonNull String code, @NonNull UUID playerUuid);

    /**
     * Generate a code to be used for linking the given UUID
     * @param player the player to generate a code for
     * @return the generated, six-character code
     */
    default @NonNull String generateLinkingCode(@NonNull OfflinePlayer player) {
        return generateLinkingCode(player.getUniqueId());
    }

}
