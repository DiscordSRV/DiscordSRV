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

package github.scarsz.discordsrv.linking.impl.system.sql;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.linking.AccountSystem;
import github.scarsz.discordsrv.linking.impl.system.BaseAccountSystem;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * A SQL-backed {@link AccountSystem}.
 */
public abstract class SqlAccountSystem extends BaseAccountSystem {

    private boolean supportsArrays = true;

    public void initialize(String database) throws SQLException {
        if (this instanceof MySQLAccountSystem) {
            createDatabaseIfNotExists(database);
        }

        if (checkIfTableExists("accounts")) {
            Map<String, String> expected = new HashMap<>();
            expected.put("discord", "varchar(32)");
            expected.put("uuid", canStoreNativeUuids() ? "uuid" : "varchar(36)");
            if (!checkIfTableMatchesStructure("accounts", expected)) {
                throw new SQLException("SQL accounts table does not match expected structure");
            }
        } else {
            try (final PreparedStatement statement = getConnection().prepareStatement(
                    "create table accounts\n" +
                            "(\n" +
                            "    link    int auto_increment primary key,\n" +
                            "    discord varchar(32) not null,\n" +
                            "    uuid    " + (canStoreNativeUuids() ? "uuid" : "varchar(36)") + " not null,\n" +
                            "    constraint accounts_discord_uindex unique (discord),\n" +
                            "    constraint accounts_uuid_uindex unique (uuid)\n" +
                            ");")) {
                statement.executeUpdate();
            }
        }

        if (checkIfTableExists("codes")) {
            final Map<String, String> expected = new HashMap<>();
            expected.put("code", "char(4)");
            expected.put("uuid", canStoreNativeUuids() ? "uuid" : "varchar(36)");

            final Map<String, String> legacyExpected = new HashMap<>(expected);
            legacyExpected.put("expiration", "bigint(20)");
            expected.put("expiration", "bigint");
            if (!(checkIfTableMatchesStructure("codes", expected, false) || checkIfTableMatchesStructure("codes", legacyExpected))) {
                throw new SQLException("SQL codes table does not match expected structure");
            }
        } else {
            try (final PreparedStatement statement = getConnection().prepareStatement(
                    "create table codes\n" +
                            "(\n" +
                            "    code       char(4)     not null primary key,\n" +
                            "    uuid       " + (canStoreNativeUuids() ? "uuid" : "varchar(36)") +" not null,\n" +
                            "    expiration bigint(20)  not null,\n" +
                            "    constraint codes_uuid_uindex unique (uuid)\n" +
                            ");")) {
                statement.executeUpdate();
            }
        }
    }

    public abstract Connection getConnection();
    public abstract boolean canStoreNativeUuids();

    public void createDatabaseIfNotExists(String database) throws SQLException {
        try (final PreparedStatement statement = getConnection().prepareStatement("CREATE DATABASE IF NOT EXISTS `" + database + "`")) {
            statement.executeUpdate();
        }
    }

    public boolean checkIfTableExists(String table) {
        boolean tableExists = false;
        try (final PreparedStatement statement = getConnection().prepareStatement("SELECT 1 FROM " + table + " LIMIT 1")) {
            statement.executeQuery();
            tableExists = true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("doesn't exist")) DiscordSRV.error(e);
        }
        return tableExists;
    }

    public Map<String, String> getTableColumns(String table) throws SQLException {
        final Map<String, String> columns = new HashMap<>();
        try (final PreparedStatement statement = getConnection().prepareStatement("SHOW COLUMNS FROM " + table)) {
            ResultSet result = statement.executeQuery();

            while (result.next()) {
                columns.put(result.getString("Field"), result.getString("Type"));
            }
        }
        return columns;
    }

    public boolean checkIfTableMatchesStructure(String table, Map<String, String> expectedColumns) throws SQLException {
        return checkIfTableMatchesStructure(table, expectedColumns, true);
    }

    public boolean checkIfTableMatchesStructure(String table, Map<String, String> expectedColumns, boolean showErrors) throws SQLException {
        final List<String> found = new LinkedList<>();
        for (Map.Entry<String, String> entry : getTableColumns(table).entrySet()) {
            if (!expectedColumns.containsKey(entry.getKey())) continue; // only check columns that we're expecting
            final String expectedType = expectedColumns.get(entry.getKey());
            final String actualType = entry.getValue();
            if (!expectedType.equals(actualType)) {
                if (showErrors) {
                    DiscordSRV.error("Expected type " + expectedType + " for column " + entry.getKey() + ", got " + actualType);
                }
                return false;
            }
            found.add(entry.getKey());
        }

        return found.containsAll(expectedColumns.keySet());
    }

    @Override
    public void close() {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    @SneakyThrows
    public @Nullable String queryDiscordId(@NotNull UUID playerUuid) {
        try (PreparedStatement statement = getConnection().prepareStatement("select discord from `accounts` where uuid = ?")) {
            statement.setObject(1, canStoreNativeUuids() ? playerUuid : playerUuid.toString());
            ResultSet result = statement.executeQuery();
            return result.next() ? result.getString("discord") : null;
        } catch (SQLException e) {
            DiscordSRV.error("[" + getClass().getSimpleName() + "] Converting Minecraft UUID " + playerUuid + " to Discord UID failed: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @SneakyThrows
    public @Nullable String getDiscordId(@NotNull UUID playerUuid) {
        return queryDiscordId(playerUuid);
    }

    @Override
    public @NotNull Map<UUID, String> getManyDiscordIds(@NotNull Set<UUID> playerUuids) {
        ensureOffThread();
        Map<UUID, String> results = new HashMap<>();

        if (supportsArrays) {
            try (final PreparedStatement statement = getConnection().prepareStatement("select uuid, discord from `accounts` where uuid in (?)")) {
                Array uuidArray = getConnection().createArrayOf(canStoreNativeUuids() ? "uuid" : "varchar", playerUuids.toArray(new UUID[0]));
                statement.setArray(1, uuidArray);
                try (final ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        results.put(
                                canStoreNativeUuids() ? (UUID) result.getObject("uuid") : UUID.fromString(result.getString("uuid")),
                                result.getString("discord")
                        );
                    }
                }
                return results;
            } catch (SQLFeatureNotSupportedException e) {
                supportsArrays = false;
            } catch (SQLException e) {
                DiscordSRV.error(e);
            }
        }

        try {
            for (UUID uuid : playerUuids) {
                try (final PreparedStatement statement = getConnection().prepareStatement("select discord from `accounts` where uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    try (final ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            results.put(uuid, result.getString("discord"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            DiscordSRV.error(e);
        }

        return results;
    }

    @Override
    @SneakyThrows
    public @Nullable UUID queryUuid(@NotNull String discordId) {
        try (PreparedStatement statement = getConnection().prepareStatement("select uuid from `accounts` where discord = ?")) {
            statement.setString(1, discordId);
            ResultSet result = statement.executeQuery();

            return result.next()
                    ? canStoreNativeUuids()
                           ? (UUID) result.getObject("uuid")
                           : UUID.fromString(result.getString("uuid"))
                    : null;
        } catch (SQLException e) {
            DiscordSRV.error("[" + getClass().getSimpleName() + "] Converting Discord UID " + discordId + " to Minecraft UUID failed: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @SneakyThrows
    public @Nullable UUID getUuid(@NotNull String discordId) {
        return queryUuid(discordId);
    }

    @Override
    public @NotNull Map<String, UUID> getManyUuids(Set<String> discordIds) {
        ensureOffThread();
        Map<String, UUID> results = new HashMap<>();

        if (supportsArrays) {
            try (final PreparedStatement statement = getConnection().prepareStatement("select discord, uuid from `accounts` where discord in (?)")) {
                Array discordIdArray = getConnection().createArrayOf("varchar", discordIds.toArray(new String[0]));
                statement.setArray(1, discordIdArray);
                try (final ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        results.put(
                                result.getString("discord"),
                                canStoreNativeUuids() ? (UUID) result.getObject("uuid") : UUID.fromString(result.getString("uuid"))
                        );
                    }
                }
                return results;
            } catch (SQLFeatureNotSupportedException e) {
                supportsArrays = false;
            } catch (SQLException e) {
                DiscordSRV.error(e);
            }
        }

        for (String discordId : discordIds) {
            try (final PreparedStatement statement = getConnection().prepareStatement("select uuid from `accounts` where discord = ?")) {
                statement.setString(1, discordId);
                try (final ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        results.put(
                                discordId,
                                canStoreNativeUuids() ? (UUID) result.getObject("uuid") : UUID.fromString(result.getString("uuid"))
                        );
                    }
                }
            } catch (SQLException e) {
                DiscordSRV.error(e);
            }
        }

        return results;
    }

    @Override
    @SneakyThrows
    public void setLinkedDiscord(@NotNull UUID playerUuid, @Nullable String discordId) {
        if (discordId != null) {
            if (isLinked(playerUuid)) {
                try (PreparedStatement statement = getConnection().prepareStatement("update `accounts` set discord = ? where uuid = ?")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, canStoreNativeUuids() ? playerUuid : playerUuid.toString());
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = getConnection().prepareStatement("insert into `accounts` (discord, uuid) values (?, ?)")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, canStoreNativeUuids() ? playerUuid : playerUuid.toString());
                    statement.executeUpdate();
                }
            }
            callAccountLinkedEvent(discordId, playerUuid);
        } else {
            String previousDiscordId = getDiscordId(playerUuid);
            try (PreparedStatement statement = getConnection().prepareStatement("delete from `accounts` where uuid = ?")) {
                statement.setObject(1, canStoreNativeUuids() ? playerUuid : playerUuid.toString());
                statement.executeUpdate();
            }
            if (previousDiscordId != null) {
                callAccountUnlinkedEvent(previousDiscordId, playerUuid);
            }
        }

        // tell other DiscordSRV servers to update their caches
        DiscordSRV.getPlugin().getBungeeChannelApi().forward(
                "ALL",
                "discordsrv:accounts",
                (playerUuid + " " + discordId).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    @SneakyThrows
    public void setLinkedMinecraft(@NotNull String discordId, @Nullable UUID playerUuid) {
        if (playerUuid != null) {
            if (isLinked(discordId)) {
                try (PreparedStatement statement = getConnection().prepareStatement("update `accounts` set uuid = ? where discord = ?")) {
                    statement.setObject(1, canStoreNativeUuids() ? playerUuid : playerUuid.toString());
                    statement.setString(2, discordId);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = getConnection().prepareStatement("insert into `accounts` (discord, uuid) values (?, ?)")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, canStoreNativeUuids() ? playerUuid : playerUuid.toString());
                    statement.executeUpdate();
                }
            }
            callAccountLinkedEvent(discordId, playerUuid);
        } else {
            UUID previousPlayer = getUuid(discordId);
            try (PreparedStatement statement = getConnection().prepareStatement("delete from `accounts` where discord = ?")) {
                statement.setString(1, discordId);
                statement.executeUpdate();
            }
            if (previousPlayer != null) {
                callAccountUnlinkedEvent(discordId, previousPlayer);
            }
        }

        // tell other DiscordSRV servers to update their caches
        DiscordSRV.getPlugin().getBungeeChannelApi().forward(
                "ALL",
                "discordsrv:accounts",
                (playerUuid + " " + discordId).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    @SneakyThrows
    public int getLinkCount() {
        try (PreparedStatement statement = getConnection().prepareStatement("select count(*) from `accounts`")) {
            ResultSet result = statement.executeQuery();
            return result.next() ? result.getInt(1) : -1;
        }
    }

    @Override
    @SneakyThrows
    public UUID lookupCode(String code) {
        try (PreparedStatement statement = getConnection().prepareStatement("select uuid from `codes` where code = ?")) {
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                if (canStoreNativeUuids()) {
                    return (UUID) result.getObject("uuid");
                } else {
                    return UUID.fromString(result.getString("uuid"));
                }
            }
        }
        return null;
    }

    @Override
    @SneakyThrows
    public @NotNull Map<String, UUID> getLinkingCodes() {
        try (PreparedStatement statement = getConnection().prepareStatement("select code, uuid from `codes`")) {
            ResultSet result = statement.executeQuery();
            Map<String, UUID> codes = new HashMap<>();
            while (result.next()) {
                codes.put(
                        result.getString("code"),
                        canStoreNativeUuids() ? (UUID) result.getObject("uuid") : UUID.fromString(result.getString("uuid"))
                );
            }
            return codes;
        }
    }

    @Override
    @SneakyThrows
    public void removeLinkingCode(@NonNull String code) {
        try (final PreparedStatement statement = getConnection().prepareStatement("delete from codes where `code` = ?")) {
            statement.setString(1, code);
            statement.executeUpdate();
        }
    }

    @Override
    @SneakyThrows
    public void dropExpiredCodes() {
        try (final PreparedStatement statement = getConnection().prepareStatement("delete from `codes` where `expiration` < ?")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    @Override
    @SneakyThrows
    public void storeLinkingCode(@NotNull String code, @NotNull UUID playerUuid) {
        try (PreparedStatement statement = getConnection().prepareStatement("insert into `codes` (code, uuid) values (?, ?)")) {
            statement.setString(1, code);
            statement.setObject(2, canStoreNativeUuids() ? playerUuid : playerUuid.toString());
            statement.executeUpdate();
        }
    }

    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + "{" +
                    "database=" + getConnection().getCatalog() +
                    "}";
        } catch (SQLException e) {
            return getClass().getSimpleName() + "{exception}";
        }
    }

}
