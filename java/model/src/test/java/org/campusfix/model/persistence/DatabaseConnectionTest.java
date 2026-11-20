package org.campusfix.model.persistence;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseConnectionTest {
    @Test
    void requiresConnectionSettings() {
        assertThrows(IllegalArgumentException.class,
                () -> new DatabaseConnection("", "user", "password"));
        assertThrows(IllegalArgumentException.class,
                () -> new DatabaseConnection("jdbc:postgresql://localhost/campusfix", null, "password"));
        assertThrows(IllegalArgumentException.class,
                () -> new DatabaseConnection("jdbc:postgresql://localhost/campusfix", "user", " "));
    }

    @Test
    void reportsConnectionFailureWithoutOpeningResources() throws SQLException {
        try (var database = new DatabaseConnection("jdbc:unsupported:campusfix", "user", "password")) {
            assertThrows(SQLException.class, database::checkConnection);
        }
    }
}

