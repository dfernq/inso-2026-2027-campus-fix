package org.campusfix.android;

import android.app.Application;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.ViewModelInitializer;
import io.github.cdimascio.dotenv.Dotenv;
import org.campusfix.android.viewmodel.ConnectionViewModel;
import org.campusfix.model.persistence.DatabaseConnection;

public final class CampusFixApplication extends Application {
    public ViewModelProvider.Factory connectionViewModelFactory() {
        return ViewModelProvider.Factory.from(new ViewModelInitializer<>(
                ConnectionViewModel.class,
                extras -> new ConnectionViewModel(this::createDatabaseConnection)));
    }

    private DatabaseConnection createDatabaseConnection() {
        var settings = Dotenv.configure().directory(getFilesDir().getAbsolutePath()).load();
        return new DatabaseConnection(
                settings.get("CAMPUSFIX_DB_URL"),
                settings.get("CAMPUSFIX_DB_USER"),
                settings.get("CAMPUSFIX_DB_PASSWORD"));
    }
}
