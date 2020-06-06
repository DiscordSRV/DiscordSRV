/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
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

import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    private final ExpiringDualHashBidiMap<String, UUID> cache = new ExpiringDualHashBidiMap<>(TimeUnit.SECONDS.toMillis(45));

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
                    accounts.put(result.getString("discord"), UUID.fromString(result.getString("uuid")));
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
        synchronized (cache) {
            if (cache.containsValue(uuid)) return cache.getKey(uuid);
        }
        String discordId = null;
        try (final PreparedStatement statement = connection.prepareStatement("select discord from " + accountsTable + " where uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (final ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    discordId = result.getString("discord");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        synchronized (cache) {
            cache.put(discordId, uuid);
        }
        return discordId;
    }

    @Override
    public Map<UUID, String> getManyDiscordIds(Set<UUID> uuids) {
        Map<UUID, String> results = new HashMap<>();

        try (final PreparedStatement statement = connection.prepareStatement("select uuid, discord from " + accountsTable + " where uuid in (?)")) {
            statement.setArray(1, connection.createArrayOf("varchar", uuids.toArray(new UUID[0])));
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID uuid = UUID.fromString(result.getString("uuid"));
                    String discordId = result.getString("discord");
                    results.put(uuid, discordId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public UUID getUuid(String discord) {
        synchronized (cache) {
            if (cache.containsKey(discord)) return cache.get(discord);
        }

        UUID uuid = null;
        try (final PreparedStatement statement = connection.prepareStatement("select uuid from " + accountsTable + " where discord = ?")) {
            statement.setString(1, discord);

            try (final ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    uuid = UUID.fromString(result.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        synchronized (cache) {
            cache.put(discord, uuid);
        }
        return uuid;
    }

    @Override
    public Map<String, UUID> getManyUuids(Set<String> discordIds) {
        Map<String, UUID> results = new HashMap<>();

        try (final PreparedStatement statement = connection.prepareStatement("select discord, uuid from " + accountsTable + " where discord in (?)")) {
            statement.setArray(1, connection.createArrayOf("varchar", discordIds.toArray(new String[0])));
            try (final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String discordId = result.getString("discord");
                    UUID uuid = UUID.fromString(result.getString("uuid"));
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
        DiscordSRV.debug("JDBC Account link: " + discordId + ": " + uuid);

        // make sure the user isn't linked
        unlink(discordId);
        unlink(uuid);

        try (final PreparedStatement statement = connection.prepareStatement("insert into " + accountsTable + " (discord, uuid) VALUES (?, ?)")) {
            statement.setString(1, discordId);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();

            // put in cache so after link procedures will for sure have the links available
            cache.put(discordId, uuid);
            afterLink(discordId, uuid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unlink(UUID uuid) {
        String discord = getDiscordId(uuid);
        if (discord == null) return;

        beforeUnlink(uuid, discord);
        try (final PreparedStatement statement = connection.prepareStatement("delete from " + accountsTable + " where `uuid` = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cache.removeValue(uuid);
        afterUnlink(uuid, discord);
    }

    @Override
    public void unlink(String discordId) {
        UUID uuid = getUuid(discordId);
        if (uuid == null) return;

        beforeUnlink(uuid, discordId);
        try (final PreparedStatement statement = connection.prepareStatement("delete from " + accountsTable + " where `discord` = ?")) {
            statement.setString(1, discordId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cache.remove(discordId);
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
