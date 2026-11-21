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
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.CategoryDao;
import org.campusfix.model.persistence.DatabaseConnection;
import org.campusfix.model.persistence.IssueDao;
import org.campusfix.model.persistence.UserDao;
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

    public record ReportDisplay(long id, String title, String category, String location,
                                String reporter, String status, String description) {
    }

    public record ReportsState(List<ReportDisplay> items, List<CategoryOption> categories,
                               int message, String validationMessage, boolean busy,
                               long submittedIssueId) {
        private ReportsState withStatus(int message, boolean busy) {
            return new ReportsState(items, categories, message, null, busy, submittedIssueId);
        }

        private ReportsState withValidationMessage(String validationMessage) {
            return new ReportsState(items, categories, 0, validationMessage, false, submittedIssueId);
        }
    }

    public record State(Status signInStatus, AccountDisplay account, ReportsState reports) {
        private State(Status signInStatus) {
            this(signInStatus, null, null);
        }
    }

    private final Supplier<DatabaseConnection> createDatabase;
    // See https://developer.android.com/topic/libraries/architecture/livedata#create_livedata_objects
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.READY));
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private DatabaseConnection database;
    private AuthenticationService authentication;
    private IssueService issues;
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
        var initialReports = new ReportsState(List.of(), List.of(), R.string.reports_loading, null, true, 0);
        state.setValue(new State(Status.READY, account, initialReports));
        loadReports(user, initialReports);
    }

    public void refreshReports() {
        var reports = state.getValue().reports();
        if (reports == null || reports.busy()) {
            return;
        }
        showReports(reports.withStatus(R.string.reports_loading, true));
        loadReports(signedInUser, reports);
    }

    public void submitReport(long categoryId, String title, String description, String location) {
        var reports = state.getValue().reports();
        if (reports == null || reports.busy()) {
            return;
        }

        var reporter = signedInUser;
        showReports(reports.withStatus(R.string.report_submitting, true));
        worker.execute(() -> {
            try {
                var issue = issues().submit(reporter, categoryId, title, description, location);
                publishReports(reporter, readReports(reporter, reports, issue.id()));
            } catch (IllegalArgumentException exception) {
                publishReports(reporter, reports.withValidationMessage(exception.getMessage()));
            } catch (SQLException exception) {
                Log.w(TAG, "Submitting the report failed.", exception);
                publishReports(reporter, reports.withStatus(R.string.report_submit_failed, false));
            }
        });
    }

    private void loadReports(User viewer, ReportsState previous) {
        worker.execute(() -> publishReports(viewer, readReports(viewer, previous, 0)));
    }

    private ReportsState readReports(User viewer, ReportsState previous, long newIssueId) {
        boolean submitted = newIssueId != 0;
        long submittedIssueId = submitted ? newIssueId : previous.submittedIssueId();
        try {
            var reports = new ArrayList<ReportDisplay>();
            for (var summary : issues().list(viewer)) {
                var issue = summary.issue();
                reports.add(new ReportDisplay(issue.id(), issue.title(), summary.categoryName(),
                        issue.location(), summary.reporterName(), issue.status().name(), issue.description()));
            }

            var categories = new ArrayList<CategoryOption>();
            if (viewer.role() == User.Role.REGULAR) {
                for (var category : issues().activeCategories()) {
                    categories.add(new CategoryOption(category.id(), category.name()));
                }
            }

            int message = reports.isEmpty() ? R.string.reports_empty : R.string.reports_loaded;
            if (submitted) {
                message = R.string.report_submitted;
            }
            return new ReportsState(List.copyOf(reports), List.copyOf(categories),
                    message, null, false, submittedIssueId);
        } catch (SQLException exception) {
            Log.w(TAG, "Loading reports failed.", exception);
            int message = submitted ? R.string.report_submitted_refresh_failed : R.string.reports_load_failed;
            return new ReportsState(previous.items(), previous.categories(),
                    message, null, false, submittedIssueId);
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
        state.setValue(new State(Status.READY, state.getValue().account(), reports));
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
