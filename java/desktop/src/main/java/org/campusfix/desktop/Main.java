package org.campusfix.desktop;

import io.github.cdimascio.dotenv.Dotenv;
import javax.swing.SwingUtilities;
import org.campusfix.desktop.controller.CampusFixController;
import org.campusfix.model.auth.AuthenticationService;
import org.campusfix.model.persistence.CategoryDao;
import org.campusfix.model.persistence.DatabaseConnection;
import org.campusfix.model.persistence.IssueDao;
import org.campusfix.model.persistence.UserDao;
import org.campusfix.model.reports.CategoryService;
import org.campusfix.model.reports.IssueService;

public final class Main {
    public static void main(String[] args) {
        var settings = Dotenv.load();
        var database = new DatabaseConnection(
                settings.get("CAMPUSFIX_DB_URL"),
                settings.get("CAMPUSFIX_DB_USER"),
                settings.get("CAMPUSFIX_DB_PASSWORD"));

        var authentication = new AuthenticationService(new UserDao(database));
        var categoryDao = new CategoryDao(database);
        var reports = new IssueService(new IssueDao(database), categoryDao);
        var categories = new CategoryService(categoryDao);
        SwingUtilities.invokeLater(
                () -> new CampusFixController(database, authentication, reports, categories).start());
    }
}

