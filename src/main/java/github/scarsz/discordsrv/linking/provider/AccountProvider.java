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

import java.util.UUID;

/**
 * Represents a provider of linked Discord and Minecraft accounts.
 */
public interface AccountProvider extends DiscordAccountProvider, MinecraftAccountProvider {

    default boolean isCaching() {
        // caching account systems will override this
        return false;
    }
    default boolean isInCache(String discordId) {
        // all items are considered "cached" unless an implementation overrides implements caching and this behavior
        return true;
    }
    default boolean isInCache(UUID playerUuid) {
        // all items are considered "cached" unless an implementation overrides implements caching and this behavior
        return true;
    }
    default UUID getIfCached(String discordId) {
        // default to potentially non-cached, implementers will override this
        return getUuid(discordId);
    }
    default String getIfCached(UUID playerUuid) {
        // default to potentially non-cached, implementers will override this
        return getDiscordId(playerUuid);
    }

}
