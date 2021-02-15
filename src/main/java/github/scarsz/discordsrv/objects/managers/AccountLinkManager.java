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

package github.scarsz.discordsrv.objects.managers;

import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Class for accessing and managing linked accounts.
 */
public abstract class AccountLinkManager implements Listener {

    /**
     * Gets the Discord ID for a given player's linked account.
     *
     * @param uuid the player's UUID
     * @return the player's linked account's Discord user id or {@code null}.
     * @see net.dv8tion.jda.api.JDA#getUserById(String)
     * @throws IllegalStateException if this is requested on Bukkit's main thread for a player that isn't online when DiscordSRV is using a non-memory storage backend (in the future)
     * @see #isInCache(UUID)
     */
    public abstract String getDiscordId(UUID uuid);

    /**
     * Gets the Minecraft uuid for a given user's linked account.
     *
     * @param discordId the Discord user's id.
     * @return the user's linked account's uuid or {@code null}.
     * @throws IllegalStateException if this is requested on Bukkit's main thread for a player that isn't online when DiscordSRV is using a non-memory storage backend (in the future)
     * @see #isInCache(String)
     */
    public abstract UUID getUuid(String discordId);

    /**
     * Gets the amount of linked accounts. This is kept in memory and is recommended over doing {@code getLinkedAccounts().size()}.
     *
     * @return the amount of linked accounts
     */
    public abstract int getLinkedAccountCount();

    /**
     * Gets multiple Discord id's for multiple uuids at once.
     *
     * @param uuids the set of Minecraft player uuids.
     * @return the map of UUID-Discord id pairs, if a given player isn't linked there will be no entry for that player.
     * @see #getDiscordId(UUID)
     * @throws IllegalStateException if this is requested on Bukkit's main thread when DiscordSRV is using a non-memory storage backend (in the future)
     */
    public abstract Map<UUID, String> getManyDiscordIds(Set<UUID> uuids);

    /**
     * Gets multiple player uuid's for multiple Discord user ids at once.
     *
     * @param discordIds the set of Discord user ids.
     * @return the map of Discord id-UUID pairs, if a given user isn't linked there will be no entry for that user.
     * @see #getUuid(String)
     * @throws IllegalStateException if this is requested on Bukkit's main thread when DiscordSRV is using a non-memory storage backend (in the future)
     */
    public abstract Map<String, UUID> getManyUuids(Set<String> discordIds);

    /**
     * Gets all linked accounts.
     *
     * @return all linked accounts in a Discord ID-UUID map.
     * @see #getUuid(String)
     * @see #getDiscordId(UUID)
     * @see #getManyUuids(Set)
     * @see #getManyDiscordIds(Set)
     * @throws IllegalStateException if this is requested on Bukkit's main thread when DiscordSRV is using a non-memory storage backend (in the future)
     */
    public abstract Map<String, UUID> getLinkedAccounts();

    /**
     * Gets the Discord ID for the given player from the cache
     * <p>WARNING, this may not represent the user's linking status</p>
     *
     * @param uuid the player's uuid
     * @return the given player's Discord id if it is in the cache
     * @see #isInCache(UUID)
     */
    public abstract String getDiscordIdFromCache(UUID uuid);

    /**
     * Gets the Player UUID for the given user from the cache
     * <p>WARNING, this may not represent the player's linking status</p>
     *
     * @param discordId the user's id
     * @return the given user's Minecraft uuid if it is in the cache
     * @see #isInCache(String)
     */
    public abstract UUID getUuidFromCache(String discordId);

    /**
     * <p>Not recommended, may lead to blocking requests to storage backends</p>
     * Requests the Discord id for the given player bypassing any caches or main thread checks. Unsafe.
     *
     * @see #getDiscordId(UUID)
     */
    public abstract String getDiscordIdBypassCache(UUID uuid);

    /**
     * <p>Not recommended, may lead to blocking requests to storage backends</p>
     * Requests the Minecraft player UUID for the given Discord user id bypassing any caches or main thread checks. Unsafe.
     *
     * @see #getUuid(String)
     */
    public abstract UUID getUuidBypassCache(String discordId);

    /**
     * Checks if a given player's Discord account is in cache.
     *
     * @param uuid the uuid for the player to check
     * @return weather or not the player's Discord account is in cache
     */
    public abstract boolean isInCache(UUID uuid);

    /**
     * Checks if a given Discord user's player uuid is in cache.
     *
     * @param discordId the discord id
     * @return weather or not the Discord user's Minecraft uuid is in cache
     */
    public abstract boolean isInCache(String discordId);

    public abstract String generateCode(UUID playerUuid);
    public abstract Map<String, UUID> getLinkingCodes();
    public abstract String process(String linkCode, String discordId);
    public abstract void link(String discordId, UUID uuid);
    public abstract void unlink(UUID uuid);
    public abstract void unlink(String discordId);

    public abstract void save();

}
