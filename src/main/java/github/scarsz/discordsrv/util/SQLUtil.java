package github.scarsz.discordsrv.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLUtil {

    public static void createDatabaseIfNotExists(Connection connection, String database) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("CREATE DATABASE IF NOT EXISTS ?");
        statement.setString(1, database);
        statement.executeUpdate();
    }

    public static boolean checkIfTableExists(Connection connection, String table) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM ? LIMIT 1");
            statement.setString(1, table);
            ResultSet result = statement.executeQuery();
            return result.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Map<String, String> getTableColumns(Connection connection, String table) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SHOW COLUMNS FROM ?");
        statement.setString(1, table);
        ResultSet result = statement.executeQuery();

        Map<String, String> columns = new HashMap<>();
        while (result.next()) {
            columns.put(result.getString("Field"), result.getString("Type"));
        }
        return columns;
    }

    public static boolean checkIfTableMatchesStructure(Connection connection, String table, Map<String, String> expectedColumns) throws SQLException {
        List<String> checkedColumns = new ArrayList<>();
        for (Map.Entry<String, String> entry : SQLUtil.getTableColumns(connection, table).entrySet()) {
            if (!expectedColumns.containsKey(entry.getKey())) continue; // only check columns that we're expecting
            String expectedType = expectedColumns.get(entry.getKey());
            String actualType = entry.getValue();
            if (!expectedType.equals(actualType)) return false;
            checkedColumns.add(entry.getKey());
        }

        return checkedColumns.containsAll(expectedColumns.keySet());
    }

}
