package org.campusfix.android.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.github.cdimascio.dotenv.DotenvException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.campusfix.android.R;
import org.campusfix.model.persistence.DatabaseConnection;

public final class ConnectionViewModel extends ViewModel {
    private static final String TAG = "ConnectionViewModel";

    public enum State {
        READY(R.string.connection_ready),
        CHECKING(R.string.connection_checking),
        CONNECTED(R.string.connection_connected),
        UNEXPECTED_RESULT(R.string.connection_unexpected_result),
        CONFIGURATION_ERROR(R.string.connection_configuration_error),
        CONNECTION_FAILED(R.string.connection_failed);

        private final int message;

        State(int message) {
            this.message = message;
        }

        public int message() {
            return message;
        }
    }

    private final Supplier<DatabaseConnection> createDatabase;
    private final MutableLiveData<State> state = new MutableLiveData<>(State.READY);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private DatabaseConnection database;

    public ConnectionViewModel(Supplier<DatabaseConnection> createDatabase) {
        this.createDatabase = createDatabase;
    }

    public LiveData<State> state() {
        return state;
    }

    public void checkConnection() {
        if (state.getValue() == State.CHECKING) {
            return;
        }
        state.setValue(State.CHECKING);
        worker.execute(() -> state.postValue(checkDatabase()));
    }

    private State checkDatabase() {
        try {
            if (database == null) {
                database = createDatabase.get();
            }
            return database.checkConnection() ? State.CONNECTED : State.UNEXPECTED_RESULT;
        } catch (DotenvException | IllegalArgumentException exception) {
            return State.CONFIGURATION_ERROR;
        } catch (SQLException exception) {
            Log.w(TAG, "Database connection check failed.", exception);
            return State.CONNECTION_FAILED;
        }
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
