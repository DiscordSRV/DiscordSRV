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
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a provider of linked Discord accounts, given a Minecraft account.
 */
public interface DiscordAccountProvider extends AccountProvider {

    /**
     * Look up the linked Discord user ID of the given {@link OfflinePlayer}
     * @param player the player to look up
     * @return the linked Discord user ID, null if not linked
     */
    @Nullable default String getDiscordId(@NonNull OfflinePlayer player) {
        return getDiscordId(player.getUniqueId());
    }
    /**
     * Look up the linked Discord user ID of the given {@link UUID}
     * @param player the uuid to look up
     * @return the linked Discord user ID, null if not linked
     */
    @Nullable String getDiscordId(@NonNull UUID player);

    default void getDiscordId(@NonNull OfflinePlayer player, Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getDiscordId(player)));
    }
    default void getDiscordId(@NonNull UUID uuid, Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> consumer.accept(getDiscordId(uuid)));
    }

    default boolean isLinked(@NonNull OfflinePlayer player) {
        return getDiscordId(player) != null;
    }
    default boolean isLinked(@NonNull UUID uuid) {
        return getDiscordId(uuid) != null;
    }

    default void ifLinked(@NonNull OfflinePlayer player, @NonNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(player)) {
                runnable.run();
            }
        });
    }
    default void ifLinked(@NonNull UUID uuid, @NonNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            if (isLinked(uuid)) {
                runnable.run();
            }
        });
    }

}
