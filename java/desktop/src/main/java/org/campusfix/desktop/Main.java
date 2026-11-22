package org.campusfix.desktop;

import javax.swing.SwingUtilities;
import org.campusfix.desktop.controller.CampusFixController;
import org.campusfix.model.auth.AuthenticationService;
import org.campusfix.model.persistence.CategoryDao;
import org.campusfix.model.persistence.IssueDao;
import org.campusfix.model.persistence.UserDao;
import org.campusfix.model.reports.CategoryService;
import org.campusfix.model.reports.IssueService;

public final class Main {
    public static void main(String[] args) {
        var database = DesktopDatabase.getInstance().connection();

        var authentication = new AuthenticationService(new UserDao(database));
        var categoryDao = new CategoryDao(database);
        var reports = new IssueService(new IssueDao(database), categoryDao);
        var categories = new CategoryService(categoryDao);
        SwingUtilities.invokeLater(
                () -> new CampusFixController(database, authentication, reports, categories).start());
    }
}

