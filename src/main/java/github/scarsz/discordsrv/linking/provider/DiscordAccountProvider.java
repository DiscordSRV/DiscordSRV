package github.scarsz.discordsrv.linking.provider;

import lombok.NonNull;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Represents a provider of linked Discord accounts, given a Minecraft account.
 */
public interface DiscordAccountProvider extends AccountProvider {

    /**
     * Look up the linked Discord user ID of the given {@link OfflinePlayer}
     * @param player the player to look up
     * @return the linked Discord user ID, null if not linked
     */
    default String getDiscordId(@NonNull OfflinePlayer player) {
        return getDiscordId(player.getUniqueId());
    }
    /**
     * Look up the linked Discord user ID of the given {@link UUID}
     * @param player the uuid to look up
     * @return the linked Discord user ID, null if not linked
     */
    String getDiscordId(@NonNull UUID player);

    default boolean isLinked(@NonNull UUID uuid) {
        return getDiscordId(uuid) != null;
    }
    default boolean isLinked(@NonNull OfflinePlayer player) {
        return getDiscordId(player) != null;
    }

}
