package org.campusfix.model;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.campusfix.model.ModelChange.CategoryChanged;
import org.campusfix.model.ModelChange.ReportChanged;
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
    private static final Logger LOGGER = Logger.getLogger(CampusFix.class.getName());

    private final List<Consumer<ModelChange>> changeListeners = new CopyOnWriteArrayList<>();
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

    public void addChangeListener(Consumer<ModelChange> listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(Consumer<ModelChange> listener) {
        changeListeners.remove(listener);
    }

    private void notifyChange(ModelChange change) {
        for (var listener : changeListeners) {
            try {
                listener.accept(change);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Model change listener failed.", exception);
            }
        }
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
        Issue issue = reports.submit(reporter, categoryId, title, description, location);
        notifyChange(new ReportChanged(reporter, ReportChanged.Kind.SUBMITTED, issue.id()));
        return issue;
    }

    public void editReport(User reporter, long issueId, long categoryId, String title,
                           String description, String location) throws SQLException {
        reports.edit(reporter, issueId, categoryId, title, description, location);
        notifyChange(new ReportChanged(reporter, ReportChanged.Kind.EDITED, issueId));
    }

    public void cancelReport(User reporter, long issueId) throws SQLException {
        reports.cancel(reporter, issueId);
        notifyChange(new ReportChanged(reporter, ReportChanged.Kind.CANCELLED, issueId));
    }

    public void startWork(User administrator, long issueId) throws SQLException {
        reports.startWork(administrator, issueId);
        notifyChange(new ReportChanged(administrator, ReportChanged.Kind.WORK_STARTED, issueId));
    }

    public void resolveReport(User administrator, long issueId, String note) throws SQLException {
        reports.resolve(administrator, issueId, note);
        notifyChange(new ReportChanged(administrator, ReportChanged.Kind.RESOLVED, issueId));
    }

    public List<Category> listCategories(User administrator) throws SQLException {
        return categories.list(administrator);
    }

    public void createCategory(User administrator, String name) throws SQLException {
        categories.create(administrator, name);
        notifyChange(new CategoryChanged(administrator, CategoryChanged.Kind.ADDED));
    }

    public void renameCategory(User administrator, long categoryId, String name) throws SQLException {
        categories.rename(administrator, categoryId, name);
        notifyChange(new CategoryChanged(administrator, CategoryChanged.Kind.RENAMED));
    }

    public void setCategoryActive(User administrator, long categoryId, boolean active) throws SQLException {
        categories.setActive(administrator, categoryId, active);
        var kind = active ? CategoryChanged.Kind.ACTIVATED : CategoryChanged.Kind.DEACTIVATED;
        notifyChange(new CategoryChanged(administrator, kind));
    }

    @Override
    public void close() throws SQLException {
        database.close();
    }
}
