package github.scarsz.discordsrv.objects.managers;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.SQLUtil;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("SqlResolve")
public class JdbcAccountLinkManager extends AccountLinkManager {

    private final static Pattern JDBC_PATTERN = Pattern.compile("([a-z]+)://(.+):(.+)/([A-z0-9]+)"); // https://regex101.com/r/7PSgv6

    private final Connection connection;
    private final String database;
    private final String accountsTable;
    private final String codesTable;
    private final ScheduledExecutorService keyExpiryScheduler;
    private final JedisPool jedisPool;

    public static boolean shouldUseJdbc() {
        return shouldUseJdbc(false);
    }

    public static boolean shouldUseJdbc(boolean quiet) {
        String jdbc = DiscordSRV.config().getString("Experiment_JdbcAccountLinkBackend");
        if (StringUtils.isBlank(jdbc)) return false;

        Matcher matcher = JDBC_PATTERN.matcher(jdbc);
        if (!matcher.find() || matcher.groupCount() < 4) {
            if (!quiet) DiscordSRV.error("Not using JDBC because < 4 matches for JDBC url");
            return false;
        }

        try {
            String engine = matcher.group(1);
            String host = matcher.group(2);
            String port = matcher.group(3);
            String database = matcher.group(4);

            if (!engine.equalsIgnoreCase("mysql")) {
                if (!quiet) DiscordSRV.error("Only MySQL is supported for JDBC currently, not using JDBC");
                return false;
            }

            if (host.equalsIgnoreCase("host") ||
                port.equalsIgnoreCase("port") ||
                database.equalsIgnoreCase("database")) {
                if (!quiet) DiscordSRV.info("Not using JDBC, one of host/port/database was default");
                return false;
            }

            return true;
        } catch (Exception e) {
            if (!quiet) DiscordSRV.error("Not using JDBC because of exception while matching parts of JDBC url: " + e.getMessage() + "\n" + ExceptionUtils.getStackTrace(e));
            return false;
        }
    }

    public JdbcAccountLinkManager() throws SQLException {
        String jdbc = DiscordSRV.config().getString("Experiment_JdbcAccountLinkBackend");
        if (!shouldUseJdbc(true) || StringUtils.isBlank(jdbc)) throw new RuntimeException("JDBC is not wanted");

        String jdbcUsername = DiscordSRV.config().getString("Experiment_JdbcUsername");
        String jdbcPassword = DiscordSRV.config().getString("Experiment_JdbcPassword");

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}

        if (StringUtils.isBlank(jdbcUsername)) {
            this.connection = DriverManager.getConnection(jdbc);
        } else {
            this.connection = DriverManager.getConnection(jdbc, jdbcUsername, jdbcPassword);
        }

        database = connection.getCatalog();
        String tablePrefix = DiscordSRV.config().getString("Experiment_JdbcTablePrefix");
        if (StringUtils.isBlank(tablePrefix)) tablePrefix = ""; else tablePrefix += "_";
        accountsTable = "`" + database + "`." + tablePrefix + "accounts";
        codesTable = "`" + database + "`." + tablePrefix + "codes";

        if (SQLUtil.checkIfTableExists(connection, accountsTable)) {
            Map<String, String> expected = new HashMap<>();
            expected.put("discord", "varchar(32)");
            expected.put("uuid", "varchar(36)");
            if (!SQLUtil.checkIfTableMatchesStructure(connection, accountsTable, expected)) {
                throw new SQLException("JDBC table " + accountsTable + " does not match expected structure");
            }
        } else {
            try (final PreparedStatement statement = connection.prepareStatement(
                    "create table " + accountsTable + "\n" +
                            "(\n" +
                            "    link    int auto_increment primary key,\n" +
                            "    discord varchar(32) not null,\n" +
                            "    uuid    varchar(36) not null,\n" +
                            "    constraint accounts_discord_uindex unique (discord),\n" +
                            "    constraint accounts_uuid_uindex unique (uuid)\n" +
                            ");")) {
                statement.executeUpdate();
            }
        }

        if (SQLUtil.checkIfTableExists(connection, codesTable)) {
            final Map<String, String> expected = new HashMap<>();
            expected.put("code", "char(4)");
            expected.put("uuid", "varchar(36)");

            final Map<String, String> legacyExpected = new HashMap<>(expected);
            legacyExpected.put("expiration", "bigint(20)");
            expected.put("expiration", "bigint");
            if (!(SQLUtil.checkIfTableMatchesStructure(connection, codesTable, expected, false)
            || SQLUtil.checkIfTableMatchesStructure(connection, codesTable, legacyExpected))) {
                throw new SQLException("JDBC table " + codesTable + " does not match expected structure");
            }
        } else {
            try (final PreparedStatement statement = connection.prepareStatement(
                    "create table " + codesTable + "\n" +
                            "(\n" +
                            "    code       char(4)     not null primary key,\n" +
                            "    uuid       varchar(36) not null,\n" +
                            "    expiration bigint(20)  not null,\n" +
                            "    constraint codes_uuid_uindex unique (uuid)\n" +
                            ");")) {
                statement.executeUpdate();
            }
        }

        DiscordSRV.info("JDBC tables passed validation, using JDBC account backend");

        File accountsFile = DiscordSRV.getPlugin().getLinkedAccountsFile();
        if (accountsFile.exists()) {
            try {
                if (DiscordSRV.getPlugin().getLinkedAccountsFile().length() != 0) {
                    DiscordSRV.info("linkedaccounts.json exists and we want to use JDBC backend, importing...");
                    File importFile = new File(accountsFile.getParentFile(), "linkedaccounts.json.imported");
                    if (!accountsFile.renameTo(importFile)) {
                        throw new RuntimeException("failed to move file to " + importFile.getName());
                    }

                    Map<String, UUID> accounts = new HashMap<>();
                    DiscordSRV.getPlugin().getGson().fromJson(FileUtils.readFileToString(importFile, StandardCharsets.UTF_8), JsonObject.class).entrySet().forEach(entry -> {
                        try {
                            accounts.put(entry.getKey(), UUID.fromString(entry.getValue().getAsString()));
                        } catch (Exception e) {
                            try {
                                accounts.put(entry.getValue().getAsString(), UUID.fromString(entry.getKey()));
                            } catch (Exception f) {
                                throw new RuntimeException("failed to parse");
                            }
                        }
                    });

                    connection.setAutoCommit(false);
                    for (Map.Entry<String, UUID> entry : accounts.entrySet()) {
                        String discord = entry.getKey();
                        UUID uuid = entry.getValue();

                        unlink(discord);
                        unlink(uuid);

                        try (final PreparedStatement statement = connection.prepareStatement("insert into " + accountsTable + " (discord, uuid) VALUES (?, ?)")) {
                            statement.setString(1, discord);
                            statement.setString(2, uuid.toString());
                            statement.executeUpdate();
                        }
                    }
                    DiscordSRV.info("Imported " + accounts.size() + " accounts to JDBC, committing...");
                    connection.setAutoCommit(true); // commit all changes at once
                    DiscordSRV.info("Finished importing accounts to JDBC backend");
                } else {
                    DiscordSRV.getPlugin().getLinkedAccountsFile().delete();
                }
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    DiscordSRV.error("Failed to import linkedaccounts.json: " + e.getMessage());
                } else {
                    DiscordSRV.error("Failed to import linkedaccounts.json:");
                    e.printStackTrace();
                }
            }
        }

        dropExpiredCodes();

        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("DiscordSRV - Key Expiry Scheduler").build();
        keyExpiryScheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);

        final String jedisHost = DiscordSRV.config().getString("Experiment_RedisHost");
        if (jedisHost == null || jedisHost.equals("HOST")) {
            jedisPool = null;
        } else {
            final int jedisPort = DiscordSRV.config().getInt("Experiment_RedisPort");
            final String jedisPassword = DiscordSRV.config().getString("Experiment_RedisPassword");
            if (jedisPassword == null || jedisPassword.equals("")) {
                jedisPool = new JedisPool(new JedisPoolConfig(), jedisHost, jedisPort);
            } else {
                jedisPool = new JedisPool(new JedisPoolConfig(), jedisHost, jedisPort, 0, jedisPassword);
            }
        }
    }

    private void dropCode(UUID uuid, String code) {
        final String uuidString = uuid.toString();
        try (final PreparedStatement statement = connection.prepareStatement("delete from " + codesTable + " where `uuid` = ? and `code` = ?")) {
            statement.setString(1, uuidString);
            statement.setString(2, code);
            statement.executeUpdate();
            if (jedisPool != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.del("linking/code/" + code, "linking/uuid/" + uuidString);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void dropExpiredCodes() {
        try (final PreparedStatement statement = connection.prepareStatement("delete from " + codesTable + " where `expiration` < ?")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public UUID getLinkingCode(String code) {
        String uuidString = null;
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                uuidString = jedis.get("linking/code/" + code);
            }
        }
        if (uuidString == null) {
            try (final PreparedStatement statement = connection.prepareStatement("select `uuid`, `expiration` from " + codesTable + " where `code` = ? and `expiration` >= ?")) {
                statement.setString(1, code);
                statement.setLong(2, System.currentTimeMillis());
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        uuidString = result.getString("uuid");
                        if (jedisPool != null) {
                            final long expiration = result.getLong("expiration");
                            final String codeKey = "linking/code/" + code;
                            final String uuidKey = "linking/uuid/" + uuidString;
                            try (Jedis jedis = jedisPool.getResource()) {
                                jedis.set(codeKey, uuidString);
                                jedis.expireAt(codeKey, expiration);
                                jedis.set(uuidKey, code);
                                jedis.expireAt(uuidKey, expiration);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (uuidString == null) return null;
        return UUID.fromString(uuidString);
    }

    public String getLinkingCode(UUID uuid) {
        final String uuidString = uuid.toString();
        String code = null;
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                code = jedis.get("linking/uuid/" + uuidString);
            }
        }
        if (code == null) {
            try (final PreparedStatement statement = connection.prepareStatement("select `code`, `expiration` from " + codesTable + " where `uuid` = ? and `expiration` >= ?")) {
                statement.setString(1, uuidString);
                statement.setLong(2, System.currentTimeMillis());
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        code = result.getString("code");
                        if (jedisPool != null) {
                            final long expiration = result.getLong("expiration");
                            final String codeKey = "linking/code/" + code;
                            final String uuidKey = "linking/uuid/" + uuidString;
                            try (Jedis jedis = jedisPool.getResource()) {
                                jedis.set(codeKey, uuidString);
                                jedis.expireAt(codeKey, expiration);
                                jedis.set(uuidKey, code);
                                jedis.expireAt(uuidKey, expiration);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return code;
    }

    @Override
    public Map<String, UUID> getLinkingCodes() {
        final Map<String, UUID> codes = new HashMap<>();

        try (final PreparedStatement statement = connection.prepareStatement("select * from " + codesTable + " where `expiration` >= ?")) {
            statement.setLong(1, System.currentTimeMillis());
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final String code = result.getString("code");
                    final String uuidString = result.getString("uuid");
                    final UUID uuid = UUID.fromString(uuidString);
                    codes.put(code, uuid);
                    if (jedisPool != null) {
                        final long expiration = result.getLong("expiration");
                        final String codeKey = "linking/code/" + code;
                        final String uuidKey = "linking/uuid/" + uuidString;
                        try (Jedis jedis = jedisPool.getResource()) {
                            jedis.set(codeKey, uuidString);
                            jedis.expireAt(codeKey, expiration);
                            jedis.set(uuidKey, code);
                            jedis.expireAt(uuidKey, expiration);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return codes;
    }

    @Override
    public Map<String, UUID> getLinkedAccounts() {
        Map<String, UUID> accounts = new HashMap<>();

        try (final PreparedStatement statement = connection.prepareStatement("select * from " + accountsTable)) {
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final String uuidString = result.getString("uuid");
                    final UUID uuid = UUID.fromString(uuidString);
                    final String discordId = result.getString("discord");
                    accounts.put(discordId, uuid);
                    if (jedisPool != null) {
                        try (Jedis jedis = jedisPool.getResource()) {
                            jedis.set("linked/discordid/" + discordId, uuidString);
                            jedis.set("linked/uuid/" + uuidString, discordId);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accounts;
    }

    @Override
    public String generateCode(UUID uuid) {
        final String uuidString = uuid.toString();
        String code = getLinkingCode(uuid);

        // delete an already existing code if one exists
        if (code != null) dropCode(uuid, code);

        final Set<String> linkingCodes = getLinkingCodes().keySet();
        if (linkingCodes.size() == 10000) throw new RuntimeException("Too many concurrent linking codes.");
        do {
            int numbers = DiscordSRV.getPlugin().getRandom().nextInt(10000);
            code = String.format("%04d", numbers);
        } while (linkingCodes.contains(code));

        try (final PreparedStatement statement = connection.prepareStatement("insert into " + codesTable + " (`code`, `uuid`, `expiration`) VALUES (?, ?, ?)")) {
            statement.setString(1, code);
            statement.setString(2, uuidString);
            final long expiration = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
            statement.setLong(3, expiration);
            statement.executeUpdate();
            if (jedisPool != null) {
                final String codeKey = "linking/code/" + code;
                final String uuidKey = "linking/uuid/" + uuidString;
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.set(codeKey, uuidString);
                    jedis.expireAt(codeKey, expiration);
                    jedis.set(uuidKey, code);
                    jedis.expireAt(uuidKey, expiration);
                }
            }
            final String finalCode = code;
            keyExpiryScheduler.schedule(() -> dropCode(uuid, finalCode), 5, TimeUnit.MINUTES);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return code;
    }

    @Override
    public String process(String code, String discordId) {
        UUID existingUuid = getUuid(discordId);
        boolean alreadyLinked = existingUuid != null;
        if (alreadyLinked) {
            if (DiscordSRV.config().getBoolean("MinecraftDiscordAccountLinkedAllowRelinkBySendingANewCode")) {
                unlink(discordId);
            } else {
                OfflinePlayer offlinePlayer = DiscordSRV.getPlugin().getServer().getOfflinePlayer(existingUuid);
                return LangUtil.InternalMessage.ALREADY_LINKED.toString()
                        .replace("{username}", String.valueOf(offlinePlayer.getName()))
                        .replace("{uuid}", offlinePlayer.getUniqueId().toString());
            }
        }

        // strip the code to get rid of non-numeric characters
        code = code.replaceAll("[^0-9]", "");

        final UUID uuid = getLinkingCode(code);
        if (uuid != null) {
            link(discordId, uuid);

            try (final PreparedStatement statement = connection.prepareStatement("delete from " + codesTable + " where `code` = ?")) {
                statement.setString(1, code);
                statement.executeUpdate();
                if (jedisPool != null) {
                    final String uuidString = uuid.toString();
                    try (Jedis jedis = jedisPool.getResource()) {
                        jedis.del("linking/code/" + code, "linking/uuid/" + uuidString);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            final Player player = offlinePlayer.getPlayer();
            if (player != null) {
                final User user = DiscordUtil.getUserById(discordId);
                if (user != null) player.sendMessage(LangUtil.Message.MINECRAFT_ACCOUNT_LINKED.toString()
                        .replace("%username%", user.getName())
                        .replace("%id%", user.getId()));
            }

            final String playerName = offlinePlayer.getName();
            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", playerName != null ? playerName : "<Unknown>")
                    .replace("%uuid%", uuid.toString());
        }

        return code.length() == 4
                ? LangUtil.InternalMessage.UNKNOWN_CODE.toString()
                : LangUtil.InternalMessage.INVALID_CODE.toString();
    }

    @Override
    public String getDiscordId(UUID uuid) {
        final String uuidString = uuid.toString();
        String discordId = null;
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                discordId = jedis.get("linked/uuid/" + uuidString);
            }
        }
        if (discordId == null) {
            try (final PreparedStatement statement = connection.prepareStatement("select discord from " + accountsTable + " where uuid = ?")) {
                statement.setString(1, uuidString);
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        discordId = result.getString("discord");
                        if (jedisPool != null) {
                            try (Jedis jedis = jedisPool.getResource()) {
                                jedis.set("linked/discordid/" + discordId, uuidString);
                                jedis.set("linked/uuid/" + uuidString, discordId);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return discordId;
    }

    @Override
    public Map<UUID, String> getManyDiscordIds(Set<UUID> uuids) {
        final Map<UUID, String> results = new HashMap<>();

        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                final Pipeline pipeline = jedis.pipelined();
                final Map<UUID, Response<String>> responses = new HashMap<>();
                for (UUID uuid : uuids) {
                    final String uuidString = uuid.toString();
                    final Response<String> response = pipeline.get("linked/uuid/" + uuidString);
                    responses.put(uuid, response);
                }
                final Map<UUID, String> responseResults = responses
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
                responseResults
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() != null)
                        .forEach(entry -> results.put(entry.getKey(), entry.getValue()));
            }
        }
        final Set<UUID> knownUuids = results.keySet();
        final Set<UUID> unknownUuids = new HashSet<>(uuids);
        unknownUuids.removeAll(knownUuids);

        try (final PreparedStatement statement = connection.prepareStatement("select uuid, discord from " + accountsTable + " where uuid in (?)")) {
            statement.setArray(1, connection.createArrayOf("varchar", unknownUuids.toArray(new UUID[0])));
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final UUID uuid = UUID.fromString(result.getString("uuid"));
                    final String discordId = result.getString("discord");
                    if (jedisPool != null) {
                        try (Jedis jedis = jedisPool.getResource()) {
                            final String uuidString = uuid.toString();
                            jedis.set("linked/discordid/" + discordId, uuidString);
                            jedis.set("linked/uuid/" + uuidString, discordId);
                        }
                    }
                    results.put(uuid, discordId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public UUID getUuid(String discordId) {
        UUID uuid = null;
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                final String uuidString = jedis.get("linked/discordid/" + discordId);
                if (uuidString != null) uuid = UUID.fromString(uuidString);
            }
        }

        if (uuid == null) {
            try (final PreparedStatement statement = connection.prepareStatement("select uuid from " + accountsTable + " where discord = ?")) {
                statement.setString(1, discordId);

                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        final String uuidString = result.getString("uuid");
                        uuid = UUID.fromString(uuidString);
                        if (jedisPool != null) {
                            try (Jedis jedis = jedisPool.getResource()) {
                                jedis.set("linked/discordid/" + discordId, uuidString);
                                jedis.set("linked/uuid/" + uuidString, discordId);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return uuid;
    }

    @Override
    public Map<String, UUID> getManyUuids(Set<String> discordIds) {
        final Map<String, UUID> results = new HashMap<>();

        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                final Pipeline pipeline = jedis.pipelined();
                final Map<String, Response<String>> responses = new HashMap<>();
                for (String discordId : discordIds) {
                    final Response<String> response = pipeline.get("linked/discordid/" + discordId);
                    responses.put(discordId, response);
                }
                final Map<String, String> responseResults = responses
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
                responseResults
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() != null)
                        .forEach(entry -> results.put(entry.getKey(), UUID.fromString(entry.getValue())));
            }
        }
        final Set<String> knownDiscordIds = results.keySet();

        final Set<String> unknownDiscordIds = new HashSet<>(discordIds);
        unknownDiscordIds.removeAll(knownDiscordIds);

        try (final PreparedStatement statement = connection.prepareStatement("select discord, uuid from " + accountsTable + " where discord in (?)")) {
            statement.setArray(1, connection.createArrayOf("varchar", unknownDiscordIds.toArray(new String[0])));
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final String discordId = result.getString("discord");
                    final String uuidString = result.getString("uuid");
                    final UUID uuid = UUID.fromString(uuidString);
                    if (jedisPool != null) {
                        try (Jedis jedis = jedisPool.getResource()) {
                            jedis.set("linked/discordid/" + discordId, uuidString);
                            jedis.set("linked/uuid/" + uuidString, discordId);
                        }
                    }
                    results.put(discordId, uuid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public void link(String discordId, UUID uuid) {
        unlink(discordId);
        unlink(uuid);

        try (final PreparedStatement statement = connection.prepareStatement("insert into " + accountsTable + " (discord, uuid) VALUES (?, ?)")) {
            final String uuidString = uuid.toString();;
            statement.setString(1, discordId);
            statement.setString(2, uuidString);
            statement.executeUpdate();

            if (jedisPool != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.set("linked/discordid/" + discordId, uuidString);
                    jedis.set("linked/uuid/" + uuidString, discordId);
                }
            }

            afterLink(discordId, uuid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unlink(UUID uuid) {
        final String discordId = getDiscordId(uuid);
        if (discordId == null) return;
        final String uuidString = uuid.toString();
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del("linked/discordid/" + discordId, "linked/uuid/" + uuidString);
            }
        }

        beforeUnlink(uuid, discordId);
        try (final PreparedStatement statement = connection.prepareStatement("delete from " + accountsTable + " where `uuid` = ?")) {
            statement.setString(1, uuidString);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        afterUnlink(uuid, discordId);
    }

    @Override
    public void unlink(String discordId) {
        final UUID uuid = getUuid(discordId);
        if (uuid == null) return;
        if (jedisPool != null) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del("linked/discordid/" + discordId, "linked/uuid/" + uuid.toString());
            }
        }

        beforeUnlink(uuid, discordId);
        try (final PreparedStatement statement = connection.prepareStatement("delete from " + accountsTable + " where `discord` = ?")) {
            statement.setString(1, discordId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        afterUnlink(uuid, discordId);
    }

    @Override
    public void save() {
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
