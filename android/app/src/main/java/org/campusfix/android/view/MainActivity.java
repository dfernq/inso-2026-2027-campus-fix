package org.campusfix.android.view;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import org.campusfix.android.CampusFixApplication;
import org.campusfix.android.R;
import org.campusfix.android.viewmodel.CampusFixViewModel;
import org.campusfix.android.viewmodel.CampusFixViewModel.State;

public final class MainActivity extends ComponentActivity {
    private View signInForm;
    private EditText username;
    private EditText password;
    private Button signIn;
    private Button checkConnection;
    private TextView status;
    private View accountPanel;
    private TextView accountTitle;
    private TextView accountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        configureInsets();

        signInForm = findViewById(R.id.sign_in_form);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        signIn = findViewById(R.id.sign_in);
        checkConnection = findViewById(R.id.check_connection);
        status = findViewById(R.id.sign_in_status);
        accountPanel = findViewById(R.id.account_panel);
        accountTitle = findViewById(R.id.account_title);
        accountName = findViewById(R.id.account_name);

        var application = (CampusFixApplication) getApplication();
        var viewModel = new ViewModelProvider(this, application.viewModelFactory())
                .get(CampusFixViewModel.class);

        signIn.setOnClickListener(view -> {
            String enteredUsername = username.getText().toString();
            char[] enteredPassword = takePassword();
            var insets = WindowCompat.getInsetsController(getWindow(), password);
            insets.hide(WindowInsetsCompat.Type.ime());
            viewModel.signIn(enteredUsername, enteredPassword);
        });
        password.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE || !signIn.isEnabled()) {
                return false;
            }
            return signIn.performClick();
        });
        checkConnection.setOnClickListener(view -> viewModel.checkConnection());
        findViewById(R.id.sign_out).setOnClickListener(view -> {
            username.getText().clear();
            viewModel.signOut();
        });
        viewModel.state().observe(this, this::render);
    }

    private void configureInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.screen), (view, insets) -> {
            int insetTypes = WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime();
            var padding = insets.getInsets(insetTypes);
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            return insets;
        });
    }

    private char[] takePassword() {
        var text = password.getText();
        char[] value = new char[text.length()];
        text.getChars(0, text.length(), value, 0);
        text.clear();
        return value;
    }

    private void render(State state) {
        var account = state.account();
        if (account != null) {
            signInForm.setVisibility(View.GONE);
            accountTitle.setText(account.title());
            accountName.setText(getString(R.string.signed_in_as, account.username()));
            accountPanel.setVisibility(View.VISIBLE);
            return;
        }

        accountPanel.setVisibility(View.GONE);
        status.setText(state.signInStatus().message());
        boolean ready = !state.signInStatus().busy();
        username.setEnabled(ready);
        password.setEnabled(ready);
        signIn.setEnabled(ready);
        checkConnection.setEnabled(ready);
        signInForm.setVisibility(View.VISIBLE);
    }
}
