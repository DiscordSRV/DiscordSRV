package github.scarsz.discordsrv.linking.impl.system;

import github.scarsz.discordsrv.linking.AccountSystem;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.params.SetParams;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A Redis-backed {@link AccountSystem}.
 */
public class RedisAccountSystem extends BaseAccountSystem {

    @Getter private final JedisPool jedisPool;

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
                host, port,
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
    public String getDiscordId(@NonNull UUID player) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get("discordsrv:accounts:" + player);
        }
    }

    @Override
    @SneakyThrows
    public UUID getUuid(@NonNull String discordId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String uuid = jedis.get("discordsrv:accounts:" + discordId);
            return uuid != null ? UUID.fromString(uuid) : null;
        }
    }

    @Override
    @SneakyThrows
    public void setLinkedDiscord(@NonNull UUID uuid, @Nullable String discordId) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (discordId != null) {
                jedis.set("discordsrv:accounts:" + discordId, String.valueOf(uuid));
                jedis.set("discordsrv:accounts:" + uuid, discordId);
                callAccountLinkedEvent(discordId, uuid);
            } else {
                String previousDiscordId = getDiscordId(uuid);
                jedis.del("discordsrv:accounts:" + getDiscordId(uuid));
                jedis.del("discordsrv:accounts:" + uuid);
                if (previousDiscordId != null) {
                    callAccountUnlinkedEvent(previousDiscordId, uuid);
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public void setLinkedMinecraft(@NonNull String discordId, @Nullable UUID uuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (uuid != null) {
                jedis.set("discordsrv:accounts:" + discordId, String.valueOf(uuid));
                jedis.set("discordsrv:accounts:" + uuid, discordId);
                callAccountLinkedEvent(discordId, uuid);
            } else {
                UUID previousPlayer = getUuid(discordId);
                jedis.del("discordsrv:accounts:" + getUuid(discordId));
                jedis.del("discordsrv:accounts:" + discordId);
                if (previousPlayer != null) {
                    callAccountUnlinkedEvent(discordId, previousPlayer);
                }
            }
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
    @SneakyThrows
    public @NonNull Map<String, UUID> getLinkingCodes() {
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
    public void storeLinkingCode(@NonNull String code, @NonNull UUID uuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set("discordsrv:codes:" + code, String.valueOf(uuid), new SetParams().ex((int) TimeUnit.MINUTES.toSeconds(15)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
