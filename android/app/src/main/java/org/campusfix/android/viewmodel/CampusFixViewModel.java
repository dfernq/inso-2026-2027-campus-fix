package org.campusfix.android.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.github.cdimascio.dotenv.DotenvException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.campusfix.android.R;
import org.campusfix.model.auth.AuthenticationService;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.DatabaseConnection;
import org.campusfix.model.persistence.UserDao;

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

    public record AccountDisplay(String username, int title) {
    }

    public record State(Status signInStatus, AccountDisplay account) {
        private State(Status signInStatus) {
            this(signInStatus, null);
        }
    }

    private final Supplier<DatabaseConnection> createDatabase;
    // See https://developer.android.com/topic/libraries/architecture/livedata#create_livedata_objects
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.READY));
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private DatabaseConnection database;
    private AuthenticationService authentication;

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
        worker.execute(() -> state.postValue(authenticate(username, password)));
    }

    public void signOut() {
        if (state.getValue().account() != null) {
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

    private State authenticate(String username, char[] password) {
        try {
            var user = authentication().signIn(username, password).orElse(null);
            if (user == null) {
                return new State(Status.INVALID_CREDENTIALS);
            }
            int title = user.role() == User.Role.ADMIN
                    ? R.string.admin_account : R.string.regular_account;
            return new State(Status.READY, new AccountDisplay(user.username(), title));
        } catch (DotenvException | IllegalArgumentException exception) {
            return new State(Status.CONFIGURATION_ERROR);
        } catch (SQLException | IllegalStateException exception) {
            Log.w(TAG, "Sign-in failed.", exception);
            return new State(Status.SIGN_IN_FAILED);
        } finally {
            Arrays.fill(password, '\0');
        }
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

    @Override
    protected void onCleared() {
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
