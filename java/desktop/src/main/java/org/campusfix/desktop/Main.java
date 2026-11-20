package org.campusfix.desktop;

import io.github.cdimascio.dotenv.Dotenv;
import javax.swing.SwingUtilities;
import org.campusfix.desktop.controller.ConnectionController;
import org.campusfix.model.persistence.DatabaseConnection;

public final class Main {
    public static void main(String[] args) {
        var settings = Dotenv.load();
        var database = new DatabaseConnection(
                settings.get("CAMPUSFIX_DB_URL"),
                settings.get("CAMPUSFIX_DB_USER"),
                settings.get("CAMPUSFIX_DB_PASSWORD"));

        SwingUtilities.invokeLater(() -> new ConnectionController(database).start());
    }
}

