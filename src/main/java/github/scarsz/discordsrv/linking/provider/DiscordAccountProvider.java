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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a provider of linked Discord accounts, given a Minecraft account.
 */
public interface DiscordAccountProvider extends AccountProvider {

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
     * Retrieve the linked Discord user ID of the given {@link OfflinePlayer}
     * @param player the player to look up
     * @param consumer the consumer to call when the Discord ID is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void retrieveDiscordId(@NotNull OfflinePlayer player, Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getDiscordId(player)));
    }
    /**
     * Retrieve the linked Discord user ID of the given {@link UUID}
     * @param playerUuid the uuid to look up
     * @param consumer the consumer to call when the Discord ID is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void retrieveDiscordId(@NotNull UUID playerUuid, Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getDiscordId(playerUuid)));
    }

    /**
     * Retrieve the Discord user ID associated with the given player
     * @param player the player to look up
     * @return future containing the Discord ID if linked, contains null otherwise
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default CompletableFuture<String> retrieveDiscordId(@NotNull OfflinePlayer player) {
        return CompletableFuture.supplyAsync(() -> getDiscordId(player));
    }
    /**
     * Retrieve the Discord user ID associated with the given player UUID
     * @param playerUuid the player uuid to look up
     * @return future containing the Discord ID if linked, contains null otherwise
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default CompletableFuture<String> retrieveDiscordId(@NotNull UUID playerUuid) {
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

}
