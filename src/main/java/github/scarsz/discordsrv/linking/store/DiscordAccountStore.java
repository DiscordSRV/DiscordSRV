package github.scarsz.discordsrv.linking.store;

import github.scarsz.discordsrv.linking.provider.DiscordAccountProvider;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Represents a storage media for Discord accounts
 */
public interface DiscordAccountStore extends AccountStore, DiscordAccountProvider {

    void setLinkedDiscord(@NonNull UUID target, @Nullable String discordId);
    default void setLinkedDiscord(@NonNull OfflinePlayer target, @Nullable String discordId) {
        setLinkedDiscord(target.getUniqueId(), discordId);
    }
    default void setLinkedDiscord(@NonNull UUID target, @Nullable User user) {
        setLinkedDiscord(target, user != null ? user.getId() : null);
    }
    default void setLinkedDiscord(@NonNull OfflinePlayer target, @Nullable User user) {
        setLinkedDiscord(target.getUniqueId(), user);
    }

}
