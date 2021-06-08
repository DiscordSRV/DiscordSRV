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
import net.dv8tion.jda.api.entities.User;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Represents a storage media for Discord accounts
 */
public interface DiscordAccountStore {

    void setLinkedDiscord(@NonNull UUID playerUuid, @Nullable String discordId);
    default void setLinkedDiscord(@NonNull OfflinePlayer player, @Nullable String discordId) {
        setLinkedDiscord(player.getUniqueId(), discordId);
    }
    default void setLinkedDiscord(@NonNull UUID playerUuid, @Nullable User user) {
        setLinkedDiscord(playerUuid, user != null ? user.getId() : null);
    }
    default void setLinkedDiscord(@NonNull OfflinePlayer player, @Nullable User user) {
        setLinkedDiscord(player.getUniqueId(), user);
    }

    /**
     * Unlink the given Minecraft player UUID from it's linked Discord account, if present
     * @param playerUuid the player UUID to unlink
     */
    default void unlink(@NotNull UUID playerUuid) {
        setLinkedDiscord(playerUuid, (String) null);
    }
    /**
     * Unlink the given Minecraft player from their linked Discord account, if present
     * @param player the player to unlink
     */
    default void unlink(@NotNull OfflinePlayer player) {
        unlink(player.getUniqueId());
    }

}
