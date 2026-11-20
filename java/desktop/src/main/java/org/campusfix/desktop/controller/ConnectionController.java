package org.campusfix.desktop.controller;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.campusfix.desktop.view.ConnectionView;
import org.campusfix.model.persistence.DatabaseConnection;

public final class ConnectionController {
    private static final Logger LOGGER = Logger.getLogger(ConnectionController.class.getName());

    private final DatabaseConnection database;
    private final ConnectionView view;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean closing;

    public ConnectionController(DatabaseConnection database) {
        this.database = database;
        view = new ConnectionView();
        view.onCheck(this::checkConnection);
        view.onClose(this::close);
    }

    public void start() {
        view.show();
    }

    private void checkConnection() {
        view.showStatus("Checking PostgreSQL...", true);
        worker.execute(() -> {
            String message;
            try {
                message = database.checkConnection()
                        ? "Connected to PostgreSQL."
                        : "PostgreSQL returned an unexpected result.";
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Database connection check failed.", exception);
                message = "Connection failed. Check your settings and PostgreSQL.";
            }
            String result = message;
            SwingUtilities.invokeLater(() -> {
                if (!closing) {
                    view.showStatus(result, false);
                }
            });
        });
    }

    private void close() {
        if (closing) {
            return;
        }
        closing = true;
        view.close();
        worker.execute(() -> {
            try {
                database.close();
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Database connection could not be closed.", exception);
            }
        });
        worker.shutdown();
    }
}

