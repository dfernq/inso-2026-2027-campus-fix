package org.campusfix.model.persistence;

import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class DatabaseConnectionIntegrationTest {
    @Test
    void checksPostgresReusesTheConnectionAndClosesIt() throws SQLException {
        var settings = Dotenv.load();
        String url = settings.get("CAMPUSFIX_TEST_DB_URL");
        assertNotNull(url, "Set CAMPUSFIX_TEST_DB_URL in .env.");
        assertTrue(url.startsWith("jdbc:postgresql://"), "Use a PostgreSQL JDBC URL.");
        assertTrue(URI.create(url.substring(5)).getPath().endsWith("_test"),
                "Integration tests require a dedicated database whose name ends with _test.");

        Connection connection;
        try (var database = new DatabaseConnection(url,
                settings.get("CAMPUSFIX_DB_USER"),
                settings.get("CAMPUSFIX_DB_PASSWORD"))) {
            connection = database.getConnection();
            assertTrue(connection.getAutoCommit());
            assertTrue(database.checkConnection());
            assertSame(connection, database.getConnection());
            assertFalse(connection.isClosed());
        }
        assertTrue(connection.isClosed());
    }
}

