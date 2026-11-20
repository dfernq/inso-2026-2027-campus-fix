package org.campusfix.android.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import org.campusfix.android.CampusFixApplication;
import org.campusfix.android.R;
import org.campusfix.android.viewmodel.ConnectionViewModel;
import org.campusfix.android.viewmodel.ConnectionViewModel.State;

public final class MainActivity extends ComponentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.screen), (view, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        var application = (CampusFixApplication) getApplication();
        var viewModel = new ViewModelProvider(this, application.connectionViewModelFactory())
                .get(ConnectionViewModel.class);

        TextView status = findViewById(R.id.connection_status);
        Button checkButton = findViewById(R.id.check_connection);
        checkButton.setOnClickListener(view -> viewModel.checkConnection());
        viewModel.state().observe(this, state -> {
            status.setText(state.message());
            checkButton.setEnabled(state != State.CHECKING);
        });
    }
}
