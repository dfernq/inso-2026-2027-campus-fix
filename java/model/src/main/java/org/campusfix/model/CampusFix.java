package org.campusfix.model;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.campusfix.model.auth.AuthenticationService;
import org.campusfix.model.domain.Category;
import org.campusfix.model.domain.Issue;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.CategoryDao;
import org.campusfix.model.persistence.DatabaseConnection;
import org.campusfix.model.persistence.IssueDao;
import org.campusfix.model.persistence.UserDao;
import org.campusfix.model.reports.CategoryService;
import org.campusfix.model.reports.IssueService;

public final class CampusFix implements AutoCloseable {
    private final DatabaseConnection database;
    private final AuthenticationService authentication;
    private final IssueService reports;
    private final CategoryService categories;

    public CampusFix(DatabaseConnection database) {
        this.database = database;
        var categoryDao = new CategoryDao(database);
        authentication = new AuthenticationService(new UserDao(database));
        reports = new IssueService(new IssueDao(database), categoryDao);
        categories = new CategoryService(categoryDao);
    }

    public boolean checkConnection() throws SQLException {
        return database.checkConnection();
    }

    public Optional<User> signIn(String username, char[] password) throws SQLException {
        return authentication.signIn(username, password);
    }

    public List<Issue.Summary> listReports(User viewer) throws SQLException {
        return reports.list(viewer);
    }

    public List<Category> activeCategories() throws SQLException {
        return reports.activeCategories();
    }

    public Issue submitReport(User reporter, long categoryId, String title,
                              String description, String location) throws SQLException {
        return reports.submit(reporter, categoryId, title, description, location);
    }

    public void editReport(User reporter, long issueId, long categoryId, String title,
                           String description, String location) throws SQLException {
        reports.edit(reporter, issueId, categoryId, title, description, location);
    }

    public void cancelReport(User reporter, long issueId) throws SQLException {
        reports.cancel(reporter, issueId);
    }

    public void startWork(User administrator, long issueId) throws SQLException {
        reports.startWork(administrator, issueId);
    }

    public void resolveReport(User administrator, long issueId, String note) throws SQLException {
        reports.resolve(administrator, issueId, note);
    }

    public List<Category> listCategories(User administrator) throws SQLException {
        return categories.list(administrator);
    }

    public void createCategory(User administrator, String name) throws SQLException {
        categories.create(administrator, name);
    }

    public void renameCategory(User administrator, long categoryId, String name) throws SQLException {
        categories.rename(administrator, categoryId, name);
    }

    public void setCategoryActive(User administrator, long categoryId, boolean active) throws SQLException {
        categories.setActive(administrator, categoryId, active);
    }

    @Override
    public void close() throws SQLException {
        database.close();
    }
}
