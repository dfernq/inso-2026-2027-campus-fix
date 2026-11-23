package org.campusfix.desktop.controller;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.campusfix.desktop.view.CampusFixView;
import org.campusfix.desktop.view.CampusFixView.CategoryOption;
import org.campusfix.desktop.view.CategoryPanel;
import org.campusfix.desktop.view.CategoryPanel.CategoryRow;
import org.campusfix.model.auth.AuthenticationService;
import org.campusfix.model.domain.Issue;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.DatabaseConnection;
import org.campusfix.model.reports.CategoryService;
import org.campusfix.model.reports.IssueService;

public final class CampusFixController {
    private static final Logger LOGGER = Logger.getLogger(CampusFixController.class.getName());

    private final DatabaseConnection database;
    private final AuthenticationService authentication;
    private final IssueService reports;
    private final CategoryService categories;
    private final CampusFixView view;
    private final CategoryPanel categoryPanel;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private User signedInUser;
    private ReportFormState reportForm = new NewReport();
    private boolean managingCategories;
    private boolean closing;

    public CampusFixController(DatabaseConnection database, AuthenticationService authentication,
                               IssueService reports, CategoryService categories) {
        this.database = database;
        this.authentication = authentication;
        this.reports = reports;
        this.categories = categories;
        categoryPanel = new CategoryPanel();
        view = new CampusFixView(categoryPanel);
        view.onSignIn(this::signIn);
        view.onCheck(this::checkConnection);
        view.onSignOut(this::signOut);
        view.onRefresh(this::refreshAccount);
        view.onSwitchAccountSection(this::switchAccountSection);
        categoryPanel.onAdd(() -> changeCategory(CategoryAction.ADD));
        categoryPanel.onRename(() -> changeCategory(CategoryAction.RENAME));
        categoryPanel.onToggleActive(() -> changeCategory(CategoryAction.TOGGLE_ACTIVE));
        view.onSubmitReport(this::saveReport);
        view.onEditReport(this::beginEdit);
        view.onCancelReport(this::cancelReport);
        view.onDiscardEdit(this::resetReportForm);
        view.onStartWork(() -> advanceReport(Issue.Status.IN_PROGRESS));
        view.onResolveReport(() -> advanceReport(Issue.Status.RESOLVED));
        view.onClose(this::close);
    }

    public void start() {
        view.showSignIn();
        view.show();
    }

    private void signIn() {
        String username = view.username();
        char[] password = view.takePassword();
        view.showStatus("Signing in...", true);

        worker.execute(() -> {
            try {
                User user = authentication.signIn(username, password).orElse(null);
                updateWindow(() -> completeSignIn(user));
            } catch (SQLException | IllegalStateException exception) {
                LOGGER.log(Level.WARNING, "Sign-in failed.", exception);
                updateWindow(() -> view.showStatus("Could not sign in. Check the database setup.", false));
            } finally {
                Arrays.fill(password, '\0');
            }
        });
    }

    private void completeSignIn(User user) {
        if (user == null) {
            view.showStatus("Username or password is incorrect.", false);
            return;
        }
        signedInUser = user;
        var presentation = ReportPresentation.forRole(user.role());
        view.showAccount(presentation.accountTitle(), user.username(), presentation.showsSubmissionForm());
        refreshReports();
    }

    private void signOut() {
        signedInUser = null;
        reportForm = new NewReport();
        managingCategories = false;
        categoryPanel.reset();
        view.showSignIn();
    }

    private enum CategoryAction {
        ADD, RENAME, TOGGLE_ACTIVE
    }

    private void switchAccountSection() {
        User user = signedInUser;
        if (user == null || user.role() != User.Role.ADMIN) {
            return;
        }
        managingCategories = !managingCategories;
        view.showCategorySection(managingCategories);
        refreshAccount();
    }

    private void refreshAccount() {
        if (managingCategories) {
            loadCategories(null);
        } else {
            refreshReports();
        }
    }

    private void loadCategories(String successMessage) {
        User administrator = signedInUser;
        if (administrator == null) {
            return;
        }
        String progress = successMessage == null
                ? "Loading categories..." : String.format("%s Refreshing...", successMessage);
        showCategoryStatus(progress, true);
        worker.execute(() -> {
            try {
                var rows = categories.list(administrator).stream()
                        .map(category -> new CategoryRow(category.id(), category.name(), category.active()))
                        .toList();
                updateAccount(administrator, () -> {
                    categoryPanel.showCategories(rows);
                    String message = successMessage == null ? "Categories up to date." : successMessage;
                    showCategoryStatus(message, false);
                });
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Loading categories failed.", exception);
                String message = successMessage == null
                        ? "Could not load categories. Try Refresh."
                        : String.format("%s Could not refresh categories.", successMessage);
                updateAccount(administrator, () -> showCategoryStatus(message, false));
            }
        });
    }

    private void changeCategory(CategoryAction action) {
        User administrator = signedInUser;
        if (administrator == null) {
            return;
        }
        var selected = categoryPanel.selectedCategory();
        if (action != CategoryAction.ADD && selected == null) {
            return;
        }

        String name = action == CategoryAction.ADD ? categoryPanel.newName() : categoryPanel.selectedName();
        showCategoryStatus("Saving category...", true);
        worker.execute(() -> {
            try {
                String message = switch (action) {
                    case ADD -> {
                        categories.create(administrator, name);
                        yield "Category added.";
                    }
                    case RENAME -> {
                        categories.rename(administrator, selected.id(), name);
                        yield "Category renamed.";
                    }
                    case TOGGLE_ACTIVE -> {
                        boolean active = !selected.active();
                        categories.setActive(administrator, selected.id(), active);
                        yield active ? "Category activated." : "Category deactivated.";
                    }
                };
                updateAccount(administrator, () -> {
                    if (action == CategoryAction.ADD) {
                        categoryPanel.clearNewName();
                    }
                    loadCategories(message);
                });
            } catch (IllegalArgumentException exception) {
                updateAccount(administrator, () -> showCategoryStatus(exception.getMessage(), false));
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Saving a category failed.", exception);
                updateAccount(administrator, () -> showCategoryStatus("Could not save the category.", false));
            }
        });
    }

    private void showCategoryStatus(String message, boolean busy) {
        view.setAccountActionsEnabled(!busy);
        categoryPanel.showStatus(message, busy);
    }

    private void refreshReports() {
        loadReports("Loading reports...", null, "Could not load reports. Try Refresh.");
    }

    private void loadReports(String loadingMessage, String successMessage, String failureMessage) {
        User user = signedInUser;
        if (user == null) {
            return;
        }
        var presentation = ReportPresentation.forRole(user.role());
        view.showReportStatus(loadingMessage, true);
        worker.execute(() -> {
            try {
                var rows = reports.list(user).stream().map(presentation::toRow).toList();
                List<CategoryOption> categories = presentation.showsSubmissionForm()
                        ? reports.activeCategories().stream()
                                .map(category -> new CategoryOption(category.id(), category.name())).toList()
                        : List.of();
                updateAccount(user, () -> {
                    view.showReports(rows);
                    view.showCategories(categories);
                    String message = successMessage;
                    if (message == null) {
                        message = rows.isEmpty() ? "No reports yet." : "Reports up to date.";
                    }
                    view.showReportStatus(message, false);
                });
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Loading reports failed.", exception);
                updateAccount(user, () -> view.showReportStatus(failureMessage, false));
            }
        });
    }

    private void beginEdit() {
        var report = view.selectedReport();
        if (report == null || !report.canManage()) {
            return;
        }
        reportForm = new EditingReport(report.id());
        view.showReportForEditing(report);
    }

    private void resetReportForm() {
        reportForm = new NewReport();
        view.clearReportForm();
    }

    private void saveReport() {
        User user = signedInUser;
        if (user == null) {
            return;
        }
        reportForm.save(user, view.reportCategoryId(), view.reportTitle(),
                view.reportDescription(), view.reportLocation());
    }

    private interface ReportFormState {
        void save(User user, long categoryId, String title, String description, String location);

        default boolean edits(long reportId) {
            return false;
        }
    }

    private final class NewReport implements ReportFormState {
        @Override
        public void save(User user, long categoryId, String title, String description, String location) {
            executeReportCommand(user, () -> reports.submit(user, categoryId, title, description, location),
                    "Submitting report...", "Report submitted.", "Could not save the report.",
                    CampusFixController.this::resetReportForm);
        }
    }

    private final class EditingReport implements ReportFormState {
        private final long reportId;

        private EditingReport(long reportId) {
            this.reportId = reportId;
        }

        @Override
        public void save(User user, long categoryId, String title, String description, String location) {
            executeReportCommand(user, () -> reports.edit(user, reportId, categoryId, title, description, location),
                    "Saving report...", "Report updated.", "Could not save the report.",
                    CampusFixController.this::resetReportForm);
        }

        @Override
        public boolean edits(long reportId) {
            return this.reportId == reportId;
        }
    }

    private void cancelReport() {
        User user = signedInUser;
        var report = view.selectedReport();
        if (user == null || report == null) {
            return;
        }
        if (!report.canManage() || !view.confirmCancellation(report.id())) {
            return;
        }

        executeReportCommand(user, () -> reports.cancel(user, report.id()),
                "Cancelling report...", "Report cancelled.", "Could not cancel the report.", () -> {
                    if (reportForm.edits(report.id())) {
                        resetReportForm();
                    }
                });
    }

    private void advanceReport(Issue.Status targetStatus) {
        User user = signedInUser;
        var report = view.selectedReport();
        if (user == null || report == null) {
            return;
        }
        boolean resolving = targetStatus == Issue.Status.RESOLVED;
        boolean allowed = resolving ? report.canResolve() : report.canStartWork();
        if (!allowed) {
            return;
        }

        if (resolving) {
            String note = view.resolutionNote();
            executeReportCommand(user, () -> reports.resolve(user, report.id(), note),
                    "Resolving report...", "Report resolved.", "Could not resolve the report.",
                    () -> view.clearResolutionNote(report.id()));
        } else {
            executeReportCommand(user, () -> reports.startWork(user, report.id()),
                    "Starting work...", "Work started.", "Could not start work.", null);
        }
    }

    @FunctionalInterface
    private interface ReportCommand {
        void execute() throws SQLException;
    }

    private void executeReportCommand(User user, ReportCommand command, String progressMessage,
                                      String successMessage, String failureMessage, Runnable afterSuccess) {
        view.showReportStatus(progressMessage, true);
        worker.execute(() -> {
            try {
                command.execute();
                updateAccount(user, () -> {
                    if (afterSuccess != null) {
                        afterSuccess.run();
                    }
                    refreshAfterChange(successMessage);
                });
            } catch (IllegalArgumentException exception) {
                updateAccount(user, () -> view.showReportStatus(exception.getMessage(), false));
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, failureMessage, exception);
                updateAccount(user, () -> view.showReportStatus(failureMessage, false));
            }
        });
    }

    private void refreshAfterChange(String message) {
        loadReports(String.format("%s Refreshing...", message), message,
                String.format("%s Could not refresh reports.", message));
    }

    private void updateAccount(User user, Runnable update) {
        updateWindow(() -> {
            if (signedInUser == user) {
                update.run();
            }
        });
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
            updateWindow(() -> view.showStatus(result, false));
        });
    }

    private void updateWindow(Runnable update) {
        SwingUtilities.invokeLater(() -> {
            if (!closing) {
                update.run();
            }
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
