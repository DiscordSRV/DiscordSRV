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

package github.scarsz.discordsrv.linking.impl.system;

import github.scarsz.discordsrv.linking.AccountSystem;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.params.SetParams;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A Redis-backed {@link AccountSystem}.
 */
public class RedisAccountSystem extends BaseAccountSystem {

    @Getter private final JedisPool jedisPool;
    @Getter private String host;
    @Getter private int port;

    /**
     * Creates a Redis-backed account system utilizing the given server parameters
     */
    public RedisAccountSystem(String host, int port, @Nullable String password) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(4);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setBlockWhenExhausted(true);

        jedisPool = new JedisPool(
                poolConfig,
                this.host = host, this.port = port,
                Protocol.DEFAULT_TIMEOUT,
                StringUtils.isNotBlank(password) ? password : null
        );

        testConnection();
    }

    /**
     * Creates a Redis-backed account system utilizing the given {@link JedisPool}
     */
    public RedisAccountSystem(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        testConnection();
    }

    private void testConnection() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.info();
        } catch (Exception e) {
            throw new RuntimeException("Connection to Redis failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        jedisPool.close();
    }

    @Override
    public @Nullable String getDiscordId(@NotNull UUID playerUuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get("discordsrv:accounts:" + playerUuid);
        }
    }

    @Override
    @SneakyThrows
    public @Nullable UUID getUuid(@NotNull String discordId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String uuid = jedis.get("discordsrv:accounts:" + discordId);
            return uuid != null ? UUID.fromString(uuid) : null;
        }
    }

    @Override
    @SneakyThrows
    public void setLinkedDiscord(@NotNull UUID playerUuid, @Nullable String discordId) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (discordId != null) {
                jedis.set("discordsrv:accounts:" + discordId, String.valueOf(playerUuid));
                jedis.set("discordsrv:accounts:" + playerUuid, discordId);
                incrementLinkCount();
                callAccountLinkedEvent(discordId, playerUuid);
            } else {
                String previousDiscordId = getDiscordId(playerUuid);
                jedis.del("discordsrv:accounts:" + getDiscordId(playerUuid));
                jedis.del("discordsrv:accounts:" + playerUuid);
                if (previousDiscordId != null) {
                    decrementLinkCount();
                    callAccountUnlinkedEvent(previousDiscordId, playerUuid);
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public void setLinkedMinecraft(@NotNull String discordId, @Nullable UUID playerUuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (playerUuid != null) {
                jedis.set("discordsrv:accounts:" + discordId, String.valueOf(playerUuid));
                jedis.set("discordsrv:accounts:" + playerUuid, discordId);
                incrementLinkCount();
                callAccountLinkedEvent(discordId, playerUuid);
            } else {
                UUID previousPlayer = getUuid(discordId);
                jedis.del("discordsrv:accounts:" + getUuid(discordId));
                jedis.del("discordsrv:accounts:" + discordId);
                if (previousPlayer != null) {
                    decrementLinkCount();
                    callAccountUnlinkedEvent(discordId, previousPlayer);
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public int getLinkCount() {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get("discordsrv:accounts");
            return value != null ? Integer.parseInt(value) : -1;
        }
    }
    private void incrementLinkCount() {
        try (Jedis jedis = jedisPool.getResource()) {
            if (jedis.get("discordsrv:accounts") == null) {
                jedis.set("discordsrv:accounts", String.valueOf(0));
            }

            jedis.decr("discordsrv:accounts");
        }
    }
    private void decrementLinkCount() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.decr("discordsrv:accounts");
        }
    }

    @Override
    @SneakyThrows
    public UUID lookupCode(String code) {
        try (Jedis jedis = jedisPool.getResource()) {
            String uuid = jedis.get("discordsrv:codes:" + code);
            return uuid != null ? UUID.fromString(uuid) : null;
        }
    }

    @Override
    public void removeLinkingCode(@NonNull String code) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("discordsrv:codes:" + code);
        }
    }

    @Override
    @SneakyThrows
    public @NotNull Map<String, UUID> getLinkingCodes() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, UUID> codes = new HashMap<>();
            for (String key : jedis.keys("discordsrv:codes:*")) {
                if (key.contains("-")) {
                    codes.put(key.substring(key.lastIndexOf(":")), UUID.fromString(jedis.get(key)));
                }
            }
            return codes;
        }
    }

    @Override
    public void storeLinkingCode(@NotNull String code, @NotNull UUID playerUuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set("discordsrv:codes:" + code, String.valueOf(playerUuid), new SetParams().ex((int) TimeUnit.MINUTES.toSeconds(15)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "RedisAccountSystem{" +
                "host=" + host + ":" + port +
                "}";
    }

}
