package org.campusfix.android;

import android.app.Application;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.ViewModelInitializer;
import org.campusfix.android.viewmodel.CampusFixViewModel;

public final class CampusFixApplication extends Application {
    public ViewModelProvider.Factory viewModelFactory() {
        var database = DatabaseAccess.getInstance();
        return ViewModelProvider.Factory.from(new ViewModelInitializer<>(
                CampusFixViewModel.class,
                extras -> new CampusFixViewModel(
                        () -> database.connection(getFilesDir()), database.worker())));
    }
}
