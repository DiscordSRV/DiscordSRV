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
     * @param uuid the UUID to generate a code for
     * @return the generated, six-character code
     */
    default @NonNull String generateLinkingCode(@NonNull UUID uuid) {
        String code;
        do {
            code = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (isCodeAllocated(code));

        storeLinkingCode(code, uuid);

        return code;
    }

    /**
     * Stores the given Code-UUID pair into the {@link CodeStore}
     * @param code the linking code
     * @param uuid the target uuid
     */
    void storeLinkingCode(@NonNull String code, @NonNull UUID uuid);

    /**
     * Generate a code to be used for linking the given UUID
     * @param player the player to generate a code for
     * @return the generated, six-character code
     */
    default @NonNull String generateLinkingCode(@NonNull OfflinePlayer player) {
        return generateLinkingCode(player.getUniqueId());
    }

}
