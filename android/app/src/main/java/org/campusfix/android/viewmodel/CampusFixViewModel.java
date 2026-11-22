package org.campusfix.android.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.github.cdimascio.dotenv.DotenvException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.campusfix.android.R;
import org.campusfix.model.auth.AuthenticationService;
import org.campusfix.model.domain.Issue;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.CategoryDao;
import org.campusfix.model.persistence.DatabaseConnection;
import org.campusfix.model.persistence.IssueDao;
import org.campusfix.model.persistence.UserDao;
import org.campusfix.model.reports.CategoryService;
import org.campusfix.model.reports.IssueService;

public final class CampusFixViewModel extends ViewModel {
    private static final String TAG = "CampusFixViewModel";

    public enum Status {
        READY(R.string.sign_in_ready),
        SIGNING_IN(R.string.signing_in),
        INVALID_CREDENTIALS(R.string.invalid_credentials),
        SIGN_IN_FAILED(R.string.sign_in_failed),
        CHECKING(R.string.connection_checking),
        CONNECTED(R.string.connection_connected),
        UNEXPECTED_RESULT(R.string.connection_unexpected_result),
        CONFIGURATION_ERROR(R.string.connection_configuration_error),
        CONNECTION_FAILED(R.string.connection_failed);

        private final int message;

        Status(int message) {
            this.message = message;
        }

        public int message() {
            return message;
        }

        public boolean busy() {
            return this == SIGNING_IN || this == CHECKING;
        }
    }

    public record AccountDisplay(String username, int title, boolean canSubmit) {
    }

    public record CategoryOption(long id, String name) {
    }

    public record ReportDisplay(long id, String title, long categoryId, String category,
                                String location, String reporter, String status,
                                String description, String resolutionNote,
                                boolean canManage, boolean canStartWork, boolean canResolve) {
    }

    public record ReportsState(List<ReportDisplay> items, List<CategoryOption> categories,
                               int message, String validationMessage, boolean busy,
                               long savedDraftCount, long cancelledIssueId, long resolvedIssueId) {
        private ReportsState withStatus(int message, boolean busy) {
            return new ReportsState(items, categories, message, null, busy,
                    savedDraftCount, cancelledIssueId, resolvedIssueId);
        }

        private ReportsState withValidationMessage(String validationMessage) {
            return new ReportsState(items, categories, 0, validationMessage, false,
                    savedDraftCount, cancelledIssueId, resolvedIssueId);
        }

        private ReportsState afterSave() {
            return new ReportsState(items, categories, message, null, busy,
                    savedDraftCount + 1, cancelledIssueId, resolvedIssueId);
        }

        private ReportsState afterCancellation(long issueId) {
            return new ReportsState(items, categories, message, null, busy,
                    savedDraftCount, issueId, resolvedIssueId);
        }

        private ReportsState afterResolution(long issueId) {
            return new ReportsState(items, categories, message, null, busy,
                    savedDraftCount, cancelledIssueId, issueId);
        }
    }

    public record CategoryDisplay(long id, String name, boolean active) {
    }

    public record CategoriesState(List<CategoryDisplay> items, int message, String validationMessage,
                                  boolean busy, long savedNameCount) {
        private CategoriesState withStatus(int message, boolean busy) {
            return new CategoriesState(items, message, null, busy, savedNameCount);
        }
    }

    public record State(Status signInStatus, AccountDisplay account, ReportsState reports,
                        CategoriesState categories) {
        private State(Status signInStatus) {
            this(signInStatus, null, null, null);
        }
    }

    @FunctionalInterface
    private interface CategoryChange {
        void apply(User administrator) throws SQLException;
    }

    private final Supplier<DatabaseConnection> createDatabase;
    // See https://developer.android.com/topic/libraries/architecture/livedata#create_livedata_objects
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.READY));
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private DatabaseConnection database;
    private AuthenticationService authentication;
    private IssueService issues;
    private CategoryService categories;
    private User signedInUser;

    public CampusFixViewModel(Supplier<DatabaseConnection> createDatabase) {
        this.createDatabase = createDatabase;
    }

    public LiveData<State> state() {
        return state;
    }

    public void signIn(String username, char[] password) {
        if (!signInFormAvailable()) {
            Arrays.fill(password, '\0');
            return;
        }
        state.setValue(new State(Status.SIGNING_IN));
        worker.execute(() -> authenticate(username, password));
    }

    public void signOut() {
        if (signedInUser != null) {
            signedInUser = null;
            state.setValue(new State(Status.READY));
        }
    }

    public void checkConnection() {
        if (!signInFormAvailable()) {
            return;
        }
        state.setValue(new State(Status.CHECKING));
        worker.execute(() -> state.postValue(new State(checkDatabase())));
    }

    private boolean signInFormAvailable() {
        var current = state.getValue();
        return current.account() == null && !current.signInStatus().busy();
    }

    private void authenticate(String username, char[] password) {
        try {
            var user = authentication().signIn(username, password).orElse(null);
            if (user == null) {
                state.postValue(new State(Status.INVALID_CREDENTIALS));
                return;
            }
            mainThread.post(() -> completeSignIn(user));
        } catch (DotenvException | IllegalArgumentException exception) {
            state.postValue(new State(Status.CONFIGURATION_ERROR));
        } catch (SQLException | IllegalStateException exception) {
            Log.w(TAG, "Sign-in failed.", exception);
            state.postValue(new State(Status.SIGN_IN_FAILED));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void completeSignIn(User user) {
        if (worker.isShutdown()) {
            return;
        }
        signedInUser = user;
        boolean regular = user.role() == User.Role.REGULAR;
        int title = regular ? R.string.regular_account : R.string.admin_account;
        var account = new AccountDisplay(user.username(), title, regular);
        var initialReports = new ReportsState(List.of(), List.of(),
                R.string.reports_loading, null, true, 0, 0, 0);
        state.setValue(new State(Status.READY, account, initialReports, null));
        loadReports(user, initialReports);
    }

    public void switchSection() {
        var current = state.getValue();
        var user = signedInUser;
        if (user == null || user.role() != User.Role.ADMIN) {
            return;
        }
        var categoryPanel = current.categories();
        if (categoryPanel == null) {
            if (!current.reports().busy()) {
                loadCategories(user, new CategoriesState(List.of(), 0, null, false, 0));
            }
        } else if (!categoryPanel.busy()) {
            showReports(current.reports().withStatus(R.string.reports_loading, true));
            loadReports(user, current.reports());
        }
    }

    public void refresh() {
        var categoryPanel = state.getValue().categories();
        if (categoryPanel == null) {
            refreshReports();
        } else if (!categoryPanel.busy()) {
            loadCategories(signedInUser, categoryPanel);
        }
    }

    public void saveCategory(long categoryId, String name) {
        if (categoryId == 0) {
            changeCategory(user -> categories().create(user, name), R.string.category_added, true);
        } else {
            changeCategory(user -> categories().rename(user, categoryId, name), R.string.category_renamed, true);
        }
    }

    public void setCategoryActive(long categoryId, boolean active) {
        int message = active ? R.string.category_reactivated : R.string.category_deactivated;
        changeCategory(user -> categories().setActive(user, categoryId, active), message, false);
    }

    private void loadCategories(User administrator, CategoriesState previous) {
        showCategories(previous.withStatus(R.string.categories_loading, true));
        worker.execute(() -> publishCategories(administrator, readCategories(administrator, previous, 0)));
    }

    private void changeCategory(CategoryChange change, int successMessage, boolean nameSaved) {
        var previous = state.getValue().categories();
        if (previous == null || previous.busy()) {
            return;
        }

        var administrator = signedInUser;
        showCategories(previous.withStatus(R.string.category_saving, true));
        worker.execute(() -> {
            try {
                change.apply(administrator);
            } catch (IllegalArgumentException exception) {
                publishCategories(administrator, new CategoriesState(previous.items(), 0,
                        exception.getMessage(), false, previous.savedNameCount()));
                return;
            } catch (SQLException exception) {
                Log.w(TAG, "Saving the category failed.", exception);
                publishCategories(administrator, previous.withStatus(R.string.category_save_failed, false));
                return;
            }

            long savedNameCount = previous.savedNameCount();
            if (nameSaved) {
                savedNameCount++;
            }
            var saved = new CategoriesState(previous.items(), successMessage, null, true, savedNameCount);
            publishCategories(administrator, readCategories(administrator, saved, successMessage));
        });
    }

    private CategoriesState readCategories(User administrator, CategoriesState previous, int successMessage) {
        try {
            var rows = new ArrayList<CategoryDisplay>();
            for (var category : categories().list(administrator)) {
                rows.add(new CategoryDisplay(category.id(), category.name(), category.active()));
            }
            int message = rows.isEmpty() ? R.string.categories_empty : R.string.categories_loaded;
            if (successMessage != 0) {
                message = successMessage;
            }
            return new CategoriesState(List.copyOf(rows), message, null, false, previous.savedNameCount());
        } catch (SQLException exception) {
            Log.w(TAG, "Loading categories failed.", exception);
            int message = successMessage == 0 ? R.string.categories_load_failed : R.string.category_saved_refresh_failed;
            return previous.withStatus(message, false);
        }
    }

    private void publishCategories(User administrator, CategoriesState categories) {
        mainThread.post(() -> {
            if (signedInUser == administrator) {
                showCategories(categories);
            }
        });
    }

    private void showCategories(CategoriesState categories) {
        var current = state.getValue();
        state.setValue(new State(Status.READY, current.account(), current.reports(), categories));
    }

    private ReportsState availableReports() {
        var current = state.getValue();
        if (current.categories() != null || current.reports() == null) {
            return null;
        }
        return current.reports().busy() ? null : current.reports();
    }

    private void refreshReports() {
        var reports = availableReports();
        if (reports == null) {
            return;
        }
        showReports(reports.withStatus(R.string.reports_loading, true));
        loadReports(signedInUser, reports);
    }

    public void saveReport(long issueId, long categoryId, String title, String description, String location) {
        var reports = availableReports();
        if (reports == null) {
            return;
        }

        var reporter = signedInUser;
        boolean editing = issueId != 0;
        int progressMessage = editing ? R.string.report_saving : R.string.report_submitting;
        showReports(reports.withStatus(progressMessage, true));
        worker.execute(() -> {
            try {
                if (editing) {
                    issues().edit(reporter, issueId, categoryId, title, description, location);
                } else {
                    issues().submit(reporter, categoryId, title, description, location);
                }
                int success = editing ? R.string.report_updated : R.string.report_submitted;
                int refreshFailure = editing
                        ? R.string.report_updated_refresh_failed : R.string.report_submitted_refresh_failed;
                publishReports(reporter, readReports(reporter, reports.afterSave(), success, refreshFailure));
            } catch (IllegalArgumentException exception) {
                publishReports(reporter, reports.withValidationMessage(exception.getMessage()));
            } catch (SQLException exception) {
                Log.w(TAG, "Saving the report failed.", exception);
                publishReports(reporter, reports.withStatus(R.string.report_save_failed, false));
            }
        });
    }

    public void cancelReport(long issueId) {
        var reports = availableReports();
        if (reports == null) {
            return;
        }

        var reporter = signedInUser;
        showReports(reports.withStatus(R.string.report_cancelling, true));
        worker.execute(() -> {
            try {
                issues().cancel(reporter, issueId);
                var cancelled = reports.afterCancellation(issueId);
                publishReports(reporter, readReports(reporter, cancelled,
                        R.string.report_cancelled, R.string.report_cancelled_refresh_failed));
            } catch (IllegalArgumentException exception) {
                publishReports(reporter, reports.withValidationMessage(exception.getMessage()));
            } catch (SQLException exception) {
                Log.w(TAG, "Cancelling the report failed.", exception);
                publishReports(reporter, reports.withStatus(R.string.report_cancel_failed, false));
            }
        });
    }

    public void startWork(long issueId) {
        advanceReport(issueId, Issue.Status.IN_PROGRESS, null);
    }

    public void resolveReport(long issueId, String note) {
        advanceReport(issueId, Issue.Status.RESOLVED, note);
    }

    private void advanceReport(long issueId, Issue.Status targetStatus, String note) {
        var reports = availableReports();
        if (reports == null) {
            return;
        }

        var administrator = signedInUser;
        boolean resolving = targetStatus == Issue.Status.RESOLVED;
        int progressMessage = resolving ? R.string.report_resolving : R.string.report_starting_work;
        showReports(reports.withStatus(progressMessage, true));
        worker.execute(() -> {
            try {
                var changed = reports;
                if (resolving) {
                    issues().resolve(administrator, issueId, note);
                    changed = reports.afterResolution(issueId);
                } else {
                    issues().startWork(administrator, issueId);
                }
                int success = resolving ? R.string.report_resolved : R.string.report_work_started;
                int refreshFailure = resolving
                        ? R.string.report_resolved_refresh_failed : R.string.report_work_started_refresh_failed;
                publishReports(administrator, readReports(administrator, changed, success, refreshFailure));
            } catch (IllegalArgumentException exception) {
                publishReports(administrator, reports.withValidationMessage(exception.getMessage()));
            } catch (SQLException exception) {
                Log.w(TAG, "Updating the report status failed.", exception);
                int message = resolving ? R.string.report_resolve_failed : R.string.report_start_work_failed;
                publishReports(administrator, reports.withStatus(message, false));
            }
        });
    }

    private void loadReports(User viewer, ReportsState previous) {
        worker.execute(() -> publishReports(viewer, readReports(viewer, previous, 0, R.string.reports_load_failed)));
    }

    private ReportsState readReports(User viewer, ReportsState previous, int successMessage, int failureMessage) {
        try {
            var reports = new ArrayList<ReportDisplay>();
            boolean regular = viewer.role() == User.Role.REGULAR;
            boolean admin = viewer.role() == User.Role.ADMIN;
            for (var summary : issues().list(viewer)) {
                var issue = summary.issue();
                boolean open = issue.status() == Issue.Status.OPEN;
                boolean canManage = regular && open;
                boolean canStartWork = admin && open;
                boolean canResolve = admin && issue.status() == Issue.Status.IN_PROGRESS;
                reports.add(new ReportDisplay(issue.id(), issue.title(), issue.categoryId(), summary.categoryName(),
                        issue.location(), summary.reporterName(), issue.status().name(), issue.description(),
                        issue.resolutionNote(), canManage, canStartWork, canResolve));
            }

            var categories = new ArrayList<CategoryOption>();
            if (regular) {
                for (var category : issues().activeCategories()) {
                    categories.add(new CategoryOption(category.id(), category.name()));
                }
            }

            int message = reports.isEmpty() ? R.string.reports_empty : R.string.reports_loaded;
            if (successMessage != 0) {
                message = successMessage;
            }
            return new ReportsState(List.copyOf(reports), List.copyOf(categories), message, null, false,
                    previous.savedDraftCount(), previous.cancelledIssueId(), previous.resolvedIssueId());
        } catch (SQLException exception) {
            Log.w(TAG, "Loading reports failed.", exception);
            return previous.withStatus(failureMessage, false);
        }
    }

    private void publishReports(User viewer, ReportsState reports) {
        mainThread.post(() -> {
            if (signedInUser == viewer) {
                showReports(reports);
            }
        });
    }

    private void showReports(ReportsState reports) {
        state.setValue(new State(Status.READY, state.getValue().account(), reports, null));
    }

    private Status checkDatabase() {
        try {
            return database().checkConnection() ? Status.CONNECTED : Status.UNEXPECTED_RESULT;
        } catch (DotenvException | IllegalArgumentException exception) {
            return Status.CONFIGURATION_ERROR;
        } catch (SQLException exception) {
            Log.w(TAG, "Database connection check failed.", exception);
            return Status.CONNECTION_FAILED;
        }
    }

    private DatabaseConnection database() {
        if (database == null) {
            database = createDatabase.get();
        }
        return database;
    }

    private AuthenticationService authentication() {
        if (authentication == null) {
            authentication = new AuthenticationService(new UserDao(database()));
        }
        return authentication;
    }

    private IssueService issues() {
        if (issues == null) {
            var connection = database();
            issues = new IssueService(new IssueDao(connection), new CategoryDao(connection));
        }
        return issues;
    }

    private CategoryService categories() {
        if (categories == null) {
            categories = new CategoryService(new CategoryDao(database()));
        }
        return categories;
    }

    @Override
    protected void onCleared() {
        signedInUser = null;
        worker.execute(this::closeDatabase);
        worker.shutdown();
    }

    private void closeDatabase() {
        if (database == null) {
            return;
        }
        try {
            database.close();
        } catch (SQLException exception) {
            Log.w(TAG, "Database connection could not be closed.", exception);
        }
    }
}
