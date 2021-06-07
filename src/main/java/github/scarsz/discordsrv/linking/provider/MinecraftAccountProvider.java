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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
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
 * Represents a provider of linked Minecraft accounts, given a Discord account.
 */
public interface MinecraftAccountProvider {

    /**
     * Look up the Minecraft UUID associated with the given Discord member
     *
     * @param member the member to look up
     * @return the linked {@link UUID}, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    @Nullable default UUID getUuid(@NotNull Member member) {
        return getUuid(member.getUser());
    }
    /**
     * Look up the Minecraft UUID associated with the given Discord user
     *
     * @param user the user to look up
     * @return the linked {@link UUID}, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    @Nullable default UUID getUuid(@NotNull User user) {
        return getUuid(user.getId());
    }
    /**
     * Look up the Minecraft UUID associated with the given Discord user ID
     * @param userId the user id to look up
     * @return the linked {@link UUID}, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    @Nullable UUID getUuid(@NotNull String userId);

    /**
     * Look up many UUIDs for many Discord user IDs
     * @param discordIds Discord IDs to query
     * @return Map containing passed Discord IDs and if present their linked player UUID
     */
    default @NotNull Map<String, UUID> getManyUuids(@NotNull Set<String> discordIds) {
        Map<String, UUID> map = new HashMap<>();
        for (String id : discordIds) {
            map.put(id, getUuid(id));
        }
        return map;
    }

    /**
     * Look up the Minecraft UUID associated with the given Discord member
     *
     * @param member the member to look up
     * @param consumer the consumer to call when the {@link UUID} is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void getUuid(@NotNull Member member, Consumer<UUID> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getUuid(member)));
    }
    /**
     * Look up the Minecraft UUID associated with the given Discord user
     *
     * @param user the user to look up
     * @param consumer the consumer to call when the {@link UUID} is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void getUuid(@NotNull User user, Consumer<UUID> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getUuid(user)));
    }
    /**
     * Look up the Minecraft UUID associated with the given Discord user ID
     * @param userId the user id to look up
     * @param consumer the consumer to call when the {@link UUID} is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void getUuid(@NotNull String userId, Consumer<UUID> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getUuid(userId)));
    }

    /**
     * Get the {@link OfflinePlayer} associated with the given Discord member
     * @param member the member to look up
     * @return the linked {@link OfflinePlayer}, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default OfflinePlayer getOfflinePlayer(@NotNull Member member) {
        return getOfflinePlayer(member.getUser());
    }
    /**
     * Get the {@link OfflinePlayer} associated with the given Discord user
     * @param user the user to look up
     * @return the linked {@link OfflinePlayer}, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default OfflinePlayer getOfflinePlayer(@NotNull User user) {
        return getOfflinePlayer(user.getId());
    }
    /**
     * Get the {@link OfflinePlayer} associated with the given Discord user
     * @param userId the user id to look up
     * @return the linked {@link OfflinePlayer}, null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default OfflinePlayer getOfflinePlayer(@NotNull String userId) {
        UUID uuid = getUuid(userId);
        return uuid != null ? Bukkit.getOfflinePlayer(uuid) : null;
    }

    /**
     * Retrieve the {@link OfflinePlayer} associated with the given Discord member
     * @param member the member to look up
     * @param consumer the consumer to call when the {@link OfflinePlayer} is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void retrieveOfflinePlayer(@NotNull Member member, Consumer<OfflinePlayer> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getOfflinePlayer(member)));
    }
    /**
     * Retrieve the {@link OfflinePlayer} associated with the given Discord user
     * @param user the user to look up
     * @param consumer the consumer to call when the {@link OfflinePlayer} is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void retrieveOfflinePlayer(@NotNull User user, Consumer<OfflinePlayer> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getOfflinePlayer(user)));
    }
    /**
     * Retrieve the {@link OfflinePlayer} associated with the given Discord user
     * @param userId the user id to look up
     * @param consumer the consumer to call when the {@link OfflinePlayer} is retrieved, will pass null if not linked
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default void retrieveOfflinePlayer(@NotNull String userId, Consumer<OfflinePlayer> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getOfflinePlayer(userId)));
    }

    /**
     * Retrieve the {@link UUID} associated with the given Discord member
     * @param member the member to look up
     * @return future containing the UUID if linked, contains null otherwise
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default CompletableFuture<UUID> retrieveUuid(@NotNull Member member) {
        return CompletableFuture.supplyAsync(() -> getUuid(member));
    }
    /**
     * Retrieve the {@link UUID} associated with the given Discord member
     * @param user the user to look up
     * @return future containing the UUID if linked, contains null otherwise
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default CompletableFuture<UUID> retrieveUuid(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> getUuid(user));
    }
    /**
     * Retrieve the {@link UUID} associated with the given Discord member
     * @param userId the discord user id to look up
     * @return future containing the UUID if linked, contains null otherwise
     * @throws SQLException if this provider is backed by a SQL database and a SQL exception occurs
     */
    default CompletableFuture<UUID> retrieveUuid(@NotNull String userId) {
        return CompletableFuture.supplyAsync(() -> getUuid(userId));
    }

    default boolean isLinked(@NotNull Member member) {
        return getUuid(member.getUser()) != null;
    }
    default boolean isLinked(@NotNull User user) {
        return getUuid(user) != null;
    }
    default boolean isLinked(@NotNull String userId) {
        return getUuid(userId) != null;
    }

    default void ifLinked(@NotNull Member member, @NotNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(member)) {
                runnable.run();
            }
        });
    }
    default void ifLinked(@NotNull User user, @NotNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(user)) {
                runnable.run();
            }
        });
    }
    default void ifLinked(@NotNull String userId, @NotNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(userId)) {
                runnable.run();
            }
        });
    }

}
