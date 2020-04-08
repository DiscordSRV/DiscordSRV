package github.scarsz.discordsrv.objects.managers;

import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.SQLUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("SqlResolve")
public class JdbcAccountLinkManager extends AccountLinkManager {

    private final static Pattern JDBC_PATTERN = Pattern.compile("([a-z]+)://(.+):(.+)/([A-z0-9]+)"); // https://regex101.com/r/7PSgv6

    private final Connection connection;
    private final String database;
    private final String accountsTable;
    private final String codesTable;
    private final Map<UUID, String> cache;

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
            Map<String, String> expected = new HashMap<>();
            expected.put("code", "char(4)");
            expected.put("uuid", "varchar(36)");
            expected.put("expiration", "bigint(20)");
            if (!SQLUtil.checkIfTableMatchesStructure(connection, codesTable, expected)) {
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

        if (DiscordSRV.config().getBoolean("Experiment_JdbcSingleServerCache")) {
            cache = new ConcurrentHashMap<>();
        } else {
            cache = null;
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

    @Override
    public Map<String, UUID> getLinkingCodes() {
        dropExpiredCodes();

        Map<String, UUID> codes = new HashMap<>();

        try (final PreparedStatement statement = connection.prepareStatement("select * from " + codesTable)) {
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    codes.put(result.getString("code"), UUID.fromString(result.getString("uuid")));
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
                    final UUID uuid = UUID.fromString(result.getString("uuid"));
                    final String discordId = result.getString("discord");
                    accounts.put(discordId, uuid);
                    if (cache != null) cache.put(uuid, discordId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accounts;
    }

    @Override
    public String generateCode(UUID playerUuid) {
        // delete an already existing code if one exists
        if (getLinkingCodes().values().stream().anyMatch(playerUuid::equals)) {
            try (final PreparedStatement statement = connection.prepareStatement("delete from " + codesTable + " where `uuid` = ?")) {
                statement.setString(1, playerUuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        String code;
        do {
            int numbers = DiscordSRV.getPlugin().getRandom().nextInt(10000);
            code = String.format("%04d", numbers);
        } while (getLinkingCodes().containsKey(code));

        try (final PreparedStatement statement = connection.prepareStatement("insert into " + codesTable + " (`code`, `uuid`, `expiration`) VALUES (?, ?, ?)")) {
            statement.setString(1, code);
            statement.setString(2, playerUuid.toString());
            statement.setLong(3, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
            statement.executeUpdate();
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

        UUID uuid = getLinkingCodes().get(code);
        if (uuid != null) {
            link(discordId, uuid);

            try (final PreparedStatement statement = connection.prepareStatement("delete from " + codesTable + " where `code` = ?")) {
                statement.setString(1, code);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.isOnline()) {
                player.getPlayer().sendMessage(LangUtil.Message.MINECRAFT_ACCOUNT_LINKED.toString()
                        .replace("%username%", DiscordUtil.getUserById(discordId).getName())
                        .replace("%id%", DiscordUtil.getUserById(discordId).getId())
                );
            }

            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", player.getName() != null ? player.getName() : "<Unknown>")
                    .replace("%uuid%", uuid.toString());
        }

        return code.length() == 4
                ? LangUtil.InternalMessage.UNKNOWN_CODE.toString()
                : LangUtil.InternalMessage.INVALID_CODE.toString();
    }

    @Override
    public String getDiscordId(UUID uuid) {
        String discordId = null;
        if (cache != null) discordId = cache.getOrDefault(uuid, null);
        if (discordId == null) {
            try (final PreparedStatement statement = connection.prepareStatement("select discord from " + accountsTable + " where uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        discordId = result.getString("discord");
                        if (cache != null) cache.put(uuid, discordId);
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

        if (cache == null) {
            for (UUID uuid : uuids) {
                final String discordId = cache.getOrDefault(uuid, null);
                if (discordId == null) continue;;
                results.put(uuid, discordId);
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
                    if (cache != null) cache.put(uuid, discordId);
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
        if (cache != null) uuid = cache
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(discordId))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);

        if (uuid == null) {
            try (final PreparedStatement statement = connection.prepareStatement("select uuid from " + accountsTable + " where discord = ?")) {
                statement.setString(1, discordId);

                try (final ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        uuid = UUID.fromString(result.getString("uuid"));
                        if (cache != null) cache.put(uuid, discordId);
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

        if (cache != null) {
            cache
                .entrySet()
                .stream()
                .filter(entry -> discordIds.contains(entry.getValue()))
                .forEach(entry -> results.put(entry.getValue(), entry.getKey()));
        }
        final Set<String> knownDiscordIds = results.keySet();

        final Set<String> unknownDiscordIds = new HashSet<>(discordIds);
        unknownDiscordIds.removeAll(knownDiscordIds);

        try (final PreparedStatement statement = connection.prepareStatement("select discord, uuid from " + accountsTable + " where discord in (?)")) {
            statement.setArray(1, connection.createArrayOf("varchar", unknownDiscordIds.toArray(new String[0])));
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final String discordId = result.getString("discord");
                    final UUID uuid = UUID.fromString(result.getString("uuid"));
                    if (cache != null) cache.put(uuid, discordId);
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
            statement.setString(1, discordId);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();

            if (cache != null) cache.put(uuid, discordId);

            afterLink(discordId, uuid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unlink(UUID uuid) {
        final String discordId = getDiscordId(uuid);
        if (discordId == null) return;
        if (cache != null) cache.remove(uuid);

        beforeUnlink(uuid, discordId);
        try (final PreparedStatement statement = connection.prepareStatement("delete from " + accountsTable + " where `uuid` = ?")) {
            statement.setString(1, uuid.toString());
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
        if (cache != null) cache.remove(uuid);

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
