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

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SQLUtil {

    public static void createDatabaseIfNotExists(Connection connection, String database) throws SQLException {
        try (final PreparedStatement statement = connection.prepareStatement("CREATE DATABASE IF NOT EXISTS `" + database + "`")) {
            statement.executeUpdate();
        }
    }

    public static boolean checkIfTableExists(Connection connection, String table) {
        boolean tableExists = false;
        try (final PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1")) {
            statement.executeQuery();
            tableExists = true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("doesn't exist")) e.printStackTrace();
        }
        return tableExists;
    }

    public static Map<String, String> getTableColumns(Connection connection, String table) throws SQLException {
        final Map<String, String> columns = new HashMap<>();
        try (final PreparedStatement statement = connection.prepareStatement("SHOW COLUMNS FROM " + table)) {
            ResultSet result = statement.executeQuery();

            while (result.next()) {
                columns.put(result.getString("Field"), result.getString("Type"));
            }
        }
        return columns;
    }

    public static boolean checkIfTableMatchesStructure(Connection connection, String table, Map<String, String> expectedColumns) throws SQLException {
        return checkIfTableMatchesStructure(connection, table, expectedColumns, true);
    }

    public static boolean checkIfTableMatchesStructure(Connection connection, String table, Map<String, String> expectedColumns, boolean showErrors) throws SQLException {
        final List<String> found = new LinkedList<>();
        for (Map.Entry<String, String> entry : SQLUtil.getTableColumns(connection, table).entrySet()) {
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

}
