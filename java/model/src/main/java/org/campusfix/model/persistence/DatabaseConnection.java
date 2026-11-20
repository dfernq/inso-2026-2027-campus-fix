package org.campusfix.model.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection implements AutoCloseable {
    private final String url;
    private final Properties properties = new Properties();
    private Connection connection;

    public DatabaseConnection(String url, String username, String password) {
        requireSetting(url, "Database URL");
        requireSetting(username, "Database username");
        requireSetting(password, "Database password");

        this.url = url;
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "10");
    }

    private static void requireSetting(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must be provided.");
        }
    }

    // shortcut: one worker uses one autocommit connection; add transactions and recovery for multi-step workflows.
    public Connection getConnection() throws SQLException {
        if (connection == null) {
            connection = DriverManager.getConnection(url, properties);
        }
        return connection;
    }

    public boolean checkConnection() throws SQLException {
        try (var statement = getConnection().createStatement();
             var result = statement.executeQuery("SELECT 1")) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}

