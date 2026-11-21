package org.campusfix.android;

import android.app.Application;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.ViewModelInitializer;
import io.github.cdimascio.dotenv.Dotenv;
import org.campusfix.android.viewmodel.CampusFixViewModel;
import org.campusfix.model.persistence.DatabaseConnection;

public final class CampusFixApplication extends Application {
    public ViewModelProvider.Factory viewModelFactory() {
        return ViewModelProvider.Factory.from(new ViewModelInitializer<>(
                CampusFixViewModel.class,
                extras -> new CampusFixViewModel(this::createDatabaseConnection)));
    }

    private DatabaseConnection createDatabaseConnection() {
        var settings = Dotenv.configure().directory(getFilesDir().getAbsolutePath()).load();
        return new DatabaseConnection(
                settings.get("CAMPUSFIX_DB_URL"),
                settings.get("CAMPUSFIX_DB_USER"),
                settings.get("CAMPUSFIX_DB_PASSWORD"));
    }
}
