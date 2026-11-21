package org.campusfix.model.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import org.campusfix.model.TestDatabase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class DatabaseConnectionIntegrationTest {
    @Test
    void checksPostgresReusesTheConnectionAndClosesIt() throws SQLException {
        Connection connection;
        try (var database = TestDatabase.open()) {
            connection = database.getConnection();
            assertTrue(connection.getAutoCommit());
            assertTrue(database.checkConnection());
            assertSame(connection, database.getConnection());
            assertFalse(connection.isClosed());
        }
        assertTrue(connection.isClosed());
    }
}

