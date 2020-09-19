package github.scarsz.discordsrv.linking.store;

import github.scarsz.discordsrv.linking.provider.MinecraftAccountProvider;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Represents a storage media for Minecraft accounts
 */
public interface MinecraftAccountStore extends AccountStore, MinecraftAccountProvider {

    void setLinkedMinecraft(@NonNull String target, @Nullable UUID uuid);
    default void setLinkedMinecraft(@NonNull String target, @Nullable OfflinePlayer player) {
        setLinkedMinecraft(target, player != null ? player.getUniqueId() : null);
    }
    default void setLinkedMinecraft(@NonNull User target, @Nullable UUID uuid) {
        setLinkedMinecraft(target.getId(), uuid);
    }
    default void setLinkedMinecraft(@NonNull User target, @Nullable OfflinePlayer player) {
        setLinkedMinecraft(target, player != null ? player.getUniqueId() : null);
    }

}
