package org.campusfix.android;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.campusfix.model.persistence.DatabaseConnection;

final class DatabaseAccess {
    private static final DatabaseAccess INSTANCE = new DatabaseAccess();

    private final Executor worker = Executors.newSingleThreadExecutor();
    private DatabaseConnection connection;

    private DatabaseAccess() {
    }

    static DatabaseAccess getInstance() {
        return INSTANCE;
    }

    Executor worker() {
        return worker;
    }

    // The shared worker serializes access to the connection manager.
    DatabaseConnection connection(File filesDirectory) {
        if (connection == null) {
            var settings = Dotenv.configure().directory(filesDirectory.getAbsolutePath()).load();
            connection = new DatabaseConnection(
                    settings.get("CAMPUSFIX_DB_URL"),
                    settings.get("CAMPUSFIX_DB_USER"),
                    settings.get("CAMPUSFIX_DB_PASSWORD"));
        }
        return connection;
    }
}
