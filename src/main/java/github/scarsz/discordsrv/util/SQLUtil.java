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
        connection.prepareStatement("CREATE DATABASE IF NOT EXISTS " + database).executeUpdate();
    }

    public static boolean checkIfTableExists(Connection connection, String table) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1");
            statement.executeQuery();
            return true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("doesn't exist")) e.printStackTrace();
            return false;
        }
    }

    public static Map<String, String> getTableColumns(Connection connection, String table) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SHOW COLUMNS FROM " + table);
        ResultSet result = statement.executeQuery();

        Map<String, String> columns = new HashMap<>();
        while (result.next()) {
            columns.put(result.getString("Field"), result.getString("Type"));
        }
        return columns;
    }

    public static boolean checkIfTableMatchesStructure(Connection connection, String table, Map<String, String> expectedColumns) throws SQLException {
        List<String> found = new LinkedList<>();
        for (Map.Entry<String, String> entry : SQLUtil.getTableColumns(connection, table).entrySet()) {
            if (!expectedColumns.containsKey(entry.getKey())) continue; // only check columns that we're expecting
            String expectedType = expectedColumns.get(entry.getKey());
            String actualType = entry.getValue();
            if (!expectedType.equals(actualType)) {
                DiscordSRV.error("Expected type " + expectedType + " for column " + entry.getKey() + ", got " + actualType);
                return false;
            }
            found.add(entry.getKey());
        }

        return found.containsAll(expectedColumns.keySet());
    }

}
