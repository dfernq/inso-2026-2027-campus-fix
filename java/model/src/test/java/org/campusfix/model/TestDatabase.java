package org.campusfix.model;

import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import org.campusfix.model.persistence.DatabaseConnection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TestDatabase {
    private TestDatabase() {
    }

    public static DatabaseConnection open() {
        var settings = Dotenv.load();
        String url = settings.get("CAMPUSFIX_TEST_DB_URL");
        assertNotNull(url, "Set CAMPUSFIX_TEST_DB_URL in .env.");
        assertTrue(url.startsWith("jdbc:postgresql://"), "Use a PostgreSQL JDBC URL.");
        assertTrue(URI.create(url.substring(5)).getPath().endsWith("_test"),
                "Integration tests require a dedicated database whose name ends with _test.");

        return new DatabaseConnection(url, settings.get("CAMPUSFIX_DB_USER"),
                settings.get("CAMPUSFIX_DB_PASSWORD"));
    }
}
