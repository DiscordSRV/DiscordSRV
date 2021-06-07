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

package github.scarsz.discordsrv.linking;

import github.scarsz.discordsrv.linking.store.AccountStore;
import github.scarsz.discordsrv.linking.store.CodeStore;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface AccountSystem extends AccountStore, CodeStore {

    @NotNull AccountLinkResult process(String code, String discordId);

    /**
     * Link the given player UUID to the given Discord ID
     * @param playerUuid the player UUID to link
     * @param discordId the Discord ID to link
     */
    default void link(@NotNull UUID playerUuid, @NotNull String discordId) {
        setLinkedDiscord(playerUuid, discordId);
    }
    /**
     * Link the given player to the given Discord ID
     * @param player the player to link
     * @param discordId the Discord ID to link
     */
    default void link(@NotNull OfflinePlayer player, @NotNull String discordId) {
        setLinkedDiscord(player, discordId);
    }
    /**
     * Link the given player UUID to the given Discord user
     * @param playerUuid the player UUID to link
     * @param discordUser the Discord user to link
     */
    default void link(@NotNull UUID playerUuid, @NotNull User discordUser) {
        setLinkedDiscord(playerUuid, discordUser);
    }
    /**
     * Link the given player to the given Discord ID
     * @param player the player to link
     * @param discordUser the Discord user to link
     */
    default void link(@NotNull OfflinePlayer player, @NotNull User discordUser) {
        setLinkedDiscord(player, discordUser);
    }

    /**
     * Check the total amount of linked accounts in this {@link AccountSystem}
     * <strong>This is a potentially costly calculation and is not cached, use sparingly</strong>
     * @return the total amount of linked accounts
     */
    int getLinkCount();

    void close();

}
