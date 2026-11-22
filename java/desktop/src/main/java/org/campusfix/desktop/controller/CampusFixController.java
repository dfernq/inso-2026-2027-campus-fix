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
import org.campusfix.desktop.view.CampusFixView.ReportRow;
import org.campusfix.model.auth.AuthenticationService;
import org.campusfix.model.domain.Issue;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.DatabaseConnection;
import org.campusfix.model.reports.IssueService;

public final class CampusFixController {
    private static final Logger LOGGER = Logger.getLogger(CampusFixController.class.getName());

    private final DatabaseConnection database;
    private final AuthenticationService authentication;
    private final IssueService reports;
    private final CampusFixView view;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private User signedInUser;
    private long editingReportId;
    private boolean closing;

    public CampusFixController(DatabaseConnection database, AuthenticationService authentication,
                               IssueService reports) {
        this.database = database;
        this.authentication = authentication;
        this.reports = reports;
        view = new CampusFixView();
        view.onSignIn(this::signIn);
        view.onCheck(this::checkConnection);
        view.onSignOut(this::signOut);
        view.onRefresh(this::refreshReports);
        view.onSubmitReport(this::saveReport);
        view.onEditReport(this::beginEdit);
        view.onCancelReport(this::cancelReport);
        view.onDiscardEdit(this::discardEdit);
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
        boolean regular = user.role() == User.Role.REGULAR;
        String title = regular ? "Regular account" : "Administrator account";
        view.showAccount(title, user.username(), regular);
        refreshReports();
    }

    private void signOut() {
        signedInUser = null;
        editingReportId = 0;
        view.showSignIn();
    }

    private void refreshReports() {
        loadReports("Loading reports...", null, "Could not load reports. Try Refresh.");
    }

    private void loadReports(String loadingMessage, String successMessage, String failureMessage) {
        User user = signedInUser;
        if (user == null) {
            return;
        }
        view.showReportStatus(loadingMessage, true);
        worker.execute(() -> {
            try {
                var rows = reports.list(user).stream().map(report -> reportRow(user, report)).toList();
                List<CategoryOption> categories = user.role() == User.Role.REGULAR
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
        editingReportId = report.id();
        view.showReportForEditing(report);
    }

    private void discardEdit() {
        editingReportId = 0;
        view.clearReportForm();
    }

    private void saveReport() {
        User user = signedInUser;
        if (user == null) {
            return;
        }
        long reportId = editingReportId;
        boolean editing = reportId != 0;
        long category = view.reportCategoryId();
        String title = view.reportTitle();
        String location = view.reportLocation();
        String description = view.reportDescription();
        view.showReportStatus(editing ? "Saving report..." : "Submitting report...", true);

        worker.execute(() -> {
            try {
                if (editing) {
                    reports.edit(user, reportId, category, title, description, location);
                } else {
                    reports.submit(user, category, title, description, location);
                }
                updateAccount(user, () -> {
                    discardEdit();
                    refreshAfterChange(editing ? "Report updated." : "Report submitted.");
                });
            } catch (IllegalArgumentException exception) {
                updateAccount(user, () -> view.showReportStatus(exception.getMessage(), false));
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Saving the report failed.", exception);
                updateAccount(user, () -> view.showReportStatus("Could not save the report.", false));
            }
        });
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

        view.showReportStatus("Cancelling report...", true);
        worker.execute(() -> {
            try {
                reports.cancel(user, report.id());
                updateAccount(user, () -> {
                    if (editingReportId == report.id()) {
                        discardEdit();
                    }
                    refreshAfterChange("Report cancelled.");
                });
            } catch (IllegalArgumentException exception) {
                updateAccount(user, () -> view.showReportStatus(exception.getMessage(), false));
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Cancelling the report failed.", exception);
                updateAccount(user, () -> view.showReportStatus("Could not cancel the report.", false));
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

        String note = resolving ? view.resolutionNote() : null;
        view.showReportStatus(resolving ? "Resolving report..." : "Starting work...", true);
        worker.execute(() -> {
            try {
                if (resolving) {
                    reports.resolve(user, report.id(), note);
                } else {
                    reports.startWork(user, report.id());
                }
                updateAccount(user, () -> {
                    if (resolving) {
                        view.clearResolutionNote(report.id());
                    }
                    refreshAfterChange(resolving ? "Report resolved." : "Work started.");
                });
            } catch (IllegalArgumentException exception) {
                updateAccount(user, () -> view.showReportStatus(exception.getMessage(), false));
            } catch (SQLException exception) {
                LOGGER.log(Level.WARNING, "Updating the report status failed.", exception);
                String message = resolving ? "Could not resolve the report." : "Could not start work.";
                updateAccount(user, () -> view.showReportStatus(message, false));
            }
        });
    }

    private void refreshAfterChange(String message) {
        loadReports(String.format("%s Refreshing...", message), message,
                String.format("%s Could not refresh reports.", message));
    }

    private static ReportRow reportRow(User viewer, Issue.Summary report) {
        var issue = report.issue();
        boolean admin = viewer.role() == User.Role.ADMIN;
        boolean open = issue.status() == Issue.Status.OPEN;
        boolean canManage = viewer.role() == User.Role.REGULAR && open;
        boolean canStartWork = admin && open;
        boolean canResolve = admin && issue.status() == Issue.Status.IN_PROGRESS;
        return new ReportRow(issue.id(), issue.title(), issue.categoryId(), report.categoryName(),
                issue.location(), report.reporterName(), issue.status().name(),
                issue.description(), issue.resolutionNote(), canManage, canStartWork, canResolve);
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
