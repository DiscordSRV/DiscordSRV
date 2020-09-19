package github.scarsz.discordsrv.linking.provider;

import github.scarsz.discordsrv.DiscordSRV;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a provider of linked Minecraft accounts, given a Discord account.
 */
public interface MinecraftAccountProvider extends AccountProvider {

    /**
     * Look up the Minecraft UUID associated with the given Discord member
     *
     * @param member the member to look up
     * @return the linked {@link UUID}, null if not linked
     */
    default UUID getUuid(@NonNull Member member) {
        return getUuid(member.getUser());
    }
    /**
     * Look up the Minecraft UUID associated with the given Discord user
     *
     * @param user the user to look up
     * @return the linked {@link UUID}, null if not linked
     */
    default UUID getUuid(@NonNull User user) {
        return getUuid(user.getId());
    }
    /**
     * Look up the Minecraft UUID associated with the given Discord user ID
     * @param userId the user id to look up
     * @return the linked {@link UUID}, null if not linked
     */
    UUID getUuid(@NonNull String userId);

    default void getUuid(@NonNull Member member, Consumer<UUID> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getUuid(member)));
    }
    default void getUuid(@NonNull User user, Consumer<UUID> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getUuid(user)));
    }
    default void getUuid(@NonNull String userId, Consumer<UUID> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getUuid(userId)));
    }

    /**
     * Get the {@link OfflinePlayer} associated with the given Discord member
     * @param member the member to look up
     * @return the linked {@link OfflinePlayer}, null if not linked
     */
    default OfflinePlayer getOfflinePlayer(@NonNull Member member) {
        return getOfflinePlayer(member.getUser());
    }
    /**
     * Get the {@link OfflinePlayer} associated with the given Discord user
     * @param user the user to look up
     * @return the linked {@link OfflinePlayer}, null if not linked
     */
    default OfflinePlayer getOfflinePlayer(@NonNull User user) {
        return getOfflinePlayer(user.getId());
    }
    /**
     * Get the {@link OfflinePlayer} associated with the given Discord user
     * @param userId the user id to look up
     * @return the linked {@link OfflinePlayer}, null if not linked
     */
    default OfflinePlayer getOfflinePlayer(@NonNull String userId) {
        return Bukkit.getOfflinePlayer(getUuid(userId));
    }

    default void getOfflinePlayer(@NonNull Member member, Consumer<OfflinePlayer> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getOfflinePlayer(member)));
    }
    default void getOfflinePlayer(@NonNull User user, Consumer<OfflinePlayer> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getOfflinePlayer(user)));
    }
    default void getOfflinePlayer(@NonNull String userId, Consumer<OfflinePlayer> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getOfflinePlayer(userId)));
    }

    default boolean isLinked(@NonNull Member member) {
        return getUuid(member.getUser()) != null;
    }
    default boolean isLinked(@NonNull User user) {
        return getUuid(user) != null;
    }
    default boolean isLinked(@NonNull String userId) {
        return getUuid(userId) != null;
    }

    default void ifLinked(@NonNull Member member, @NonNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(member)) {
                runnable.run();
            }
        });
    }
    default void ifLinked(@NonNull User user, @NonNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(user)) {
                runnable.run();
            }
        });
    }
    default void ifLinked(@NonNull String userId, @NonNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(userId)) {
                runnable.run();
            }
        });
    }

}
