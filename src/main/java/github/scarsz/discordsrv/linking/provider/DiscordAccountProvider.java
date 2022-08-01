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

package github.scarsz.discordsrv.linking.provider;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a provider of linked Discord accounts, given a Minecraft account.
 */
public interface DiscordAccountProvider {

    /**
     * Get the linked Discord user ID of the given {@link OfflinePlayer}
     * @param player the player to look up
     * @return the linked Discord user ID, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    @Nullable default String getDiscordId(@NotNull OfflinePlayer player) {
        return getDiscordId(player.getUniqueId());
    }
    /**
     * Get the linked Discord user ID of the given {@link UUID}
     * @param playerUuid the uuid to look up
     * @return the linked Discord user ID, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    @Nullable String getDiscordId(@NotNull UUID playerUuid);

    /**
     * Query the <strong>current</strong> linked Discord user ID of the given {@link UUID}.
     * <strong>This bypasses any caching that may be present. Use sparingly.</strong>
     * @param playerUuid the uuid to look up
     * @return the linked Discord user ID, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default @Nullable String queryDiscordId(@NotNull UUID playerUuid) {
        return getDiscordId(playerUuid);
    }

    /**
     * Look up many Discord user IDs for many player UUIDs
     * @param playerUuids player UUIDs to query
     * @return Map containing passed Discord IDs and if present their linked player UUID
     */
    default @NotNull Map<UUID, String> getManyDiscordIds(@NotNull Set<UUID> playerUuids) {
        Map<UUID, String> map = new HashMap<>();
        for (UUID uuid : playerUuids) {
            map.put(uuid, getDiscordId(uuid));
        }
        return map;
    }

    /**
     * Retrieve the linked Discord user ID of the given {@link OfflinePlayer}
     * @param player the player to look up
     * @param consumer the consumer to call when the Discord ID is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void consumeDiscordId(@NotNull OfflinePlayer player, Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getDiscordId(player)));
    }
    /**
     * Retrieve the linked Discord user ID of the given {@link UUID}
     * @param playerUuid the uuid to look up
     * @param consumer the consumer to call when the Discord ID is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void consumeDiscordId(@NotNull UUID playerUuid, Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getDiscordId(playerUuid)));
    }

    /**
     * Retrieve the Discord user ID associated with the given player
     * @param player the player to look up
     * @return future containing the Discord ID if linked, contains null otherwise
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default CompletableFuture<String> completeDiscordId(@NotNull OfflinePlayer player) {
        return CompletableFuture.supplyAsync(() -> getDiscordId(player));
    }
    /**
     * Retrieve the Discord user ID associated with the given player UUID
     * @param playerUuid the player uuid to look up
     * @return future containing the Discord ID if linked, contains null otherwise
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default CompletableFuture<String> completeDiscordId(@NotNull UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> getDiscordId(playerUuid));
    }

    default boolean isLinked(@NotNull OfflinePlayer player) {
        return getDiscordId(player) != null;
    }
    default boolean isLinked(@NotNull UUID playerUuid) {
        return getDiscordId(playerUuid) != null;
    }

    default void ifLinked(@NotNull OfflinePlayer player, @NotNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(player)) {
                runnable.run();
            }
        });
    }
    default void ifLinked(@NotNull UUID uuid, @NotNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(uuid)) {
                runnable.run();
            }
        });
    }

    default boolean isInCache(UUID playerUuid) {
        // all items are considered "cached" unless an implementation overrides implements caching and this behavior
        return true;
    }
    default @Nullable String getIfCached(UUID playerUuid) {
        // default to potentially non-cached, implementers will override this
        return getDiscordId(playerUuid);
    }

}
