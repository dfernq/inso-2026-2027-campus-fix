package org.campusfix.desktop;

import io.github.cdimascio.dotenv.Dotenv;
import org.campusfix.model.persistence.DatabaseConnection;

final class DesktopDatabase {
    private static DesktopDatabase instance;

    private final DatabaseConnection connection;

    private DesktopDatabase() {
        var settings = Dotenv.load();
        connection = new DatabaseConnection(
                settings.get("CAMPUSFIX_DB_URL"),
                settings.get("CAMPUSFIX_DB_USER"),
                settings.get("CAMPUSFIX_DB_PASSWORD"));
    }

    static synchronized DesktopDatabase getInstance() {
        if (instance == null) {
            instance = new DesktopDatabase();
        }
        return instance;
    }

    DatabaseConnection connection() {
        return connection;
    }
}
