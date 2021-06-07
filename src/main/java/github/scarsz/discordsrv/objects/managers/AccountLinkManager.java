/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2021 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.objects.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.linking.AccountSystem;
import github.scarsz.discordsrv.linking.impl.system.AccountSystemCachingProxy;
import github.scarsz.discordsrv.linking.impl.system.MemoryAccountSystem;

import java.util.*;

/**
 * @deprecated Replaced by {@link github.scarsz.discordsrv.linking.AccountSystem} access it via {@link DiscordSRV#getAccountSystem()}
 */
@Deprecated
@SuppressWarnings({"DeprecatedIsStillUsed", "unused"})
public class AccountLinkManager {

    private AccountSystem system() {
        return DiscordSRV.getPlugin().getAccountSystem();
    }

    private Optional<AccountSystemCachingProxy<?>> cachingSystem() {
        AccountSystem system = system();
        if (system instanceof AccountSystemCachingProxy) {
            return Optional.of((AccountSystemCachingProxy<?>) system);
        }
        return Optional.empty();
    }

    /**
     * Gets the Discord ID for a given player's linked account.
     *
     * @param uuid the player's UUID
     * @return the player's linked account's Discord user id or {@code null}.
     * @see net.dv8tion.jda.api.JDA#getUserById(String)
     * @see #isInCache(UUID)
     *
     * @deprecated {@link AccountSystem#getDiscordId(UUID)}
     */
    public String getDiscordId(UUID uuid) {
        return system().getDiscordId(uuid);
    }

    /**
     * Gets the Minecraft uuid for a given user's linked account.
     *
     * @param discordId the Discord user's id.
     * @return the user's linked account's uuid or {@code null}.
     * @see #isInCache(String)
     *
     * @deprecated {@link AccountSystem#getUuid(String)}
     */
    public UUID getUuid(String discordId) {
        return system().getUuid(discordId);
    }

    /**
     * Gets the amount of linked accounts. This is kept in memory and is recommended over doing {@code getLinkedAccounts().size()}.
     *
     * @return the amount of linked accounts
     *
     * @deprecated {@link AccountSystem#getLinkCount()}
     */
    public int getLinkedAccountCount() {
        return system().getLinkCount();
    }

    /**
     * Gets multiple Discord id's for multiple uuids at once.
     *
     * @param uuids the set of Minecraft player uuids.
     * @return the map of UUID-Discord id pairs, if a given player isn't linked there will be no entry for that player.
     * @see #getDiscordId(UUID)
     *
     * @deprecated {@link AccountSystem#getManyDiscordIds(Set)}
     */
    public Map<UUID, String> getManyDiscordIds(Set<UUID> uuids) {
        return system().getManyDiscordIds(uuids);
    }

    /**
     * Gets multiple player uuid's for multiple Discord user ids at once.
     *
     * @param discordIds the set of Discord user ids.
     * @return the map of Discord id-UUID pairs, if a given user isn't linked there will be no entry for that user.
     * @see #getUuid(String)
     *
     * @deprecated {@link AccountSystem#getManyUuids(Set)}
     */
    public Map<String, UUID> getManyUuids(Set<String> discordIds) {
        return system().getManyUuids(discordIds);
    }

    /**
     * Gets all linked accounts.
     *
     * @return all linked accounts in a Discord ID-UUID map.
     * @see #getUuid(String)
     * @see #getDiscordId(UUID)
     * @see #getManyUuids(Set)
     * @see #getManyDiscordIds(Set)
     *
     * @deprecated This feature is not supported in {@link AccountSystem}
     */
    public Map<String, UUID> getLinkedAccounts() {
        AccountSystem system = system();
        if (system instanceof MemoryAccountSystem) {
            return ((MemoryAccountSystem) system).getAccounts().inverseBidiMap();
        }
        return Collections.emptyMap();
    }

    /**
     * Gets the Discord ID for the given player from the cache
     * <p>WARNING, this may not represent the user's linking status</p>
     *
     * @param uuid the player's uuid
     * @return the given player's Discord id if it is in the cache
     * @see #isInCache(UUID)
     *
     * @deprecated If the system is of type {@link AccountSystemCachingProxy}, {@link AccountSystem#getDiscordId(UUID)} will return a cached value.
     */
    public String getDiscordIdFromCache(UUID uuid) {
        return cachingSystem().map(system -> system.getDiscordId(uuid)).orElse(null);
    }

    /**
     * Gets the Player UUID for the given user from the cache
     * <p>WARNING, this may not represent the player's linking status</p>
     *
     * @param discordId the user's id
     * @return the given user's Minecraft uuid if it is in the cache
     * @see #isInCache(String)
     *
     * @deprecated If the system is of type {@link AccountSystemCachingProxy}, {@link AccountSystem#getUuid(String)} will return a cached value.
     */
    public UUID getUuidFromCache(String discordId) {
        return cachingSystem().map(system -> system.getUuid(discordId)).orElse(null);
    }

    /**
     * <p>Not recommended, may lead to blocking requests to storage backends</p>
     * Requests the Discord id for the given player bypassing any caches or main thread checks. Unsafe.
     *
     * @see #getDiscordId(UUID)
     *
     * @deprecated If the account system is of type {@link AccountSystemCachingProxy}, {@link AccountSystemCachingProxy#queryDiscordId(UUID)} otherwise {@link AccountSystem#getDiscordId(UUID)}
     */
    public String getDiscordIdBypassCache(UUID uuid) {
        AccountSystem system = system();
        if (system instanceof AccountSystemCachingProxy) {
            return ((AccountSystemCachingProxy<?>) system).queryDiscordId(uuid);
        }
        return system.getDiscordId(uuid);
    }

    /**
     * <p>Not recommended, may lead to blocking requests to storage backends</p>
     * Requests the Minecraft player UUID for the given Discord user id bypassing any caches or main thread checks. Unsafe.
     *
     * @see #getUuid(String)
     *
     * @deprecated If the account system is of type {@link AccountSystemCachingProxy}, {@link AccountSystemCachingProxy#queryUuid(String)} (UUID)} (UUID)} otherwise {@link AccountSystem#getUuid(String)}
     */
    public UUID getUuidBypassCache(String discordId) {
        AccountSystem system = system();
        if (system instanceof AccountSystemCachingProxy) {
            return ((AccountSystemCachingProxy<?>) system).queryUuid(discordId);
        }
        return system.getUuid(discordId);
    }

    /**
     * Checks if a given player's Discord account is in cache.
     *
     * @param uuid the uuid for the player to check
     * @return weather or not the player's Discord account is in cache
     *
     * @deprecated If the account system is of type {@link AccountSystemCachingProxy}, {@link AccountSystemCachingProxy#isInCache(UUID)} otherwise false
     */
    public boolean isInCache(UUID uuid) {
        return cachingSystem().map(system -> system.isInCache(uuid)).orElse(false);
    }

    /**
     * Checks if a given Discord user's player uuid is in cache.
     *
     * @param discordId the discord id
     * @return weather or not the Discord user's Minecraft uuid is in cache
     *
     * @deprecated If the account system is of type {@link AccountSystemCachingProxy}, {@link AccountSystemCachingProxy#isInCache(String)} otherwise false
     */
    public boolean isInCache(String discordId) {
        return cachingSystem().map(system -> system.isInCache(discordId)).orElse(false);
    }

    /**
     * @deprecated {@link AccountSystem#generateLinkingCode(UUID)}
     */
    public String generateCode(UUID playerUuid) {
        return system().generateLinkingCode(playerUuid);
    }

    /**
     * @deprecated {@link AccountSystem#getLinkingCodes()}
     */
    public Map<String, UUID> getLinkingCodes() {
        return system().getLinkingCodes();
    }

    /**
     * @deprecated {@link AccountSystem#process(String, String)}
     */
    public String process(String linkCode, String discordId) {
        return system().process(linkCode, discordId).getMessage();
    }

    /**
     * @deprecated {@link AccountSystem#link(UUID, String)}
     */
    public void link(String discordId, UUID uuid) {
        system().link(uuid, discordId);
    }

    /**
     * @deprecated {@link AccountSystem#unlink(UUID)}
     */
    public void unlink(UUID uuid) {
        system().unlink(uuid);
    }

    /**
     * @deprecated {@link AccountSystem#unlink(String)}
     */
    public void unlink(String discordId) {
        system().unlink(discordId);
    }

    /**
     * @deprecated This feature is not supported in {@link AccountSystem}, saving is automatic/based on database driver
     */
    public void save() {}

}
