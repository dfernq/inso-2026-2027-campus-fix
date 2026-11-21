package org.campusfix.android.view;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;
import org.campusfix.android.CampusFixApplication;
import org.campusfix.android.R;
import org.campusfix.android.viewmodel.CampusFixViewModel;
import org.campusfix.android.viewmodel.CampusFixViewModel.AccountDisplay;
import org.campusfix.android.viewmodel.CampusFixViewModel.CategoryOption;
import org.campusfix.android.viewmodel.CampusFixViewModel.ReportDisplay;
import org.campusfix.android.viewmodel.CampusFixViewModel.ReportsState;
import org.campusfix.android.viewmodel.CampusFixViewModel.State;

public final class MainActivity extends ComponentActivity {
    private static final String DRAFT_TITLE = "draftTitle";
    private static final String DRAFT_LOCATION = "draftLocation";
    private static final String DRAFT_DESCRIPTION = "draftDescription";
    private static final String DRAFT_CATEGORY = "draftCategory";
    private static final String RENDERED_SUBMISSION = "renderedSubmission";

    private View signInForm;
    private EditText username;
    private EditText password;
    private Button signIn;
    private Button checkConnection;
    private TextView status;
    private View accountPanel;
    private TextView accountTitle;
    private TextView accountName;
    private TextView reportsTitle;
    private TextView reportStatus;
    private LinearLayout reportList;
    private Button refresh;
    private View reportForm;
    private EditText reportTitle;
    private EditText reportLocation;
    private EditText reportDescription;
    private Spinner reportCategory;
    private Button submit;
    private ArrayAdapter<String> categoryNames;
    private List<CategoryOption> displayedCategories = List.of();
    private List<ReportDisplay> displayedReports = List.of();
    private long restoredCategoryId;
    private long renderedSubmissionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        configureInsets();
        bindViews();
        restoreDraft(savedInstanceState);

        var application = (CampusFixApplication) getApplication();
        var viewModel = new ViewModelProvider(this, application.viewModelFactory())
                .get(CampusFixViewModel.class);
        bindActions(viewModel);
        viewModel.state().observe(this, this::render);
    }

    private void bindViews() {
        signInForm = findViewById(R.id.sign_in_form);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        signIn = findViewById(R.id.sign_in);
        checkConnection = findViewById(R.id.check_connection);
        status = findViewById(R.id.sign_in_status);
        accountPanel = findViewById(R.id.account_panel);
        accountTitle = findViewById(R.id.account_title);
        accountName = findViewById(R.id.account_name);
        reportsTitle = findViewById(R.id.reports_title);
        reportStatus = findViewById(R.id.report_status);
        reportList = findViewById(R.id.report_list);
        refresh = findViewById(R.id.refresh_reports);
        reportForm = findViewById(R.id.report_form);
        reportTitle = findViewById(R.id.report_title);
        reportLocation = findViewById(R.id.report_location);
        reportDescription = findViewById(R.id.report_description);
        reportCategory = findViewById(R.id.report_category);
        submit = findViewById(R.id.submit_report);

        categoryNames = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        categoryNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryNames.add(getString(R.string.choose_category));
        reportCategory.setAdapter(categoryNames);
    }

    private void bindActions(CampusFixViewModel viewModel) {
        signIn.setOnClickListener(view -> {
            String enteredUsername = username.getText().toString();
            char[] enteredPassword = takePassword();
            hideKeyboard(password);
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
            hideKeyboard(username);
            viewModel.signOut();
        });
        refresh.setOnClickListener(view -> viewModel.refreshReports());
        submit.setOnClickListener(view -> {
            hideKeyboard(reportDescription);
            viewModel.submitReport(selectedCategoryId(), reportTitle.getText().toString(),
                    reportDescription.getText().toString(), reportLocation.getText().toString());
        });
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

    private void hideKeyboard(View view) {
        var insets = WindowCompat.getInsetsController(getWindow(), view);
        insets.hide(WindowInsetsCompat.Type.ime());
    }

    private char[] takePassword() {
        var text = password.getText();
        char[] value = new char[text.length()];
        text.getChars(0, text.length(), value, 0);
        text.clear();
        return value;
    }

    private void render(State state) {
        if (state.account() != null) {
            renderAccount(state.account(), state.reports());
            return;
        }

        accountPanel.setVisibility(View.GONE);
        clearDraft();
        renderedSubmissionId = 0;
        renderCategories(List.of());
        renderRows(List.of());

        status.setText(state.signInStatus().message());
        boolean ready = !state.signInStatus().busy();
        username.setEnabled(ready);
        password.setEnabled(ready);
        signIn.setEnabled(ready);
        checkConnection.setEnabled(ready);
        signInForm.setVisibility(View.VISIBLE);
    }

    private void renderAccount(AccountDisplay account, ReportsState reports) {
        signInForm.setVisibility(View.GONE);
        accountTitle.setText(account.title());
        accountName.setText(getString(R.string.signed_in_as, account.username()));
        reportsTitle.setText(account.canSubmit() ? R.string.my_reports : R.string.all_reports);
        reportForm.setVisibility(account.canSubmit() ? View.VISIBLE : View.GONE);

        if (renderedSubmissionId != reports.submittedIssueId()) {
            renderedSubmissionId = reports.submittedIssueId();
            clearDraft();
        }
        renderCategories(reports.categories());
        renderRows(reports.items());
        if (reports.validationMessage() == null) {
            reportStatus.setText(reports.message());
        } else {
            reportStatus.setText(reports.validationMessage());
        }

        boolean ready = !reports.busy();
        reportTitle.setEnabled(ready);
        reportLocation.setEnabled(ready);
        reportDescription.setEnabled(ready);
        reportCategory.setEnabled(ready);
        submit.setEnabled(ready);
        refresh.setEnabled(ready);
        accountPanel.setVisibility(View.VISIBLE);
    }

    private void renderCategories(List<CategoryOption> categories) {
        if (displayedCategories.equals(categories)) {
            return;
        }
        long selectedId = selectedCategoryId();
        restoredCategoryId = 0;
        var names = new ArrayList<String>();
        names.add(getString(R.string.choose_category));
        int selectedIndex = 0;
        for (var category : categories) {
            names.add(category.name());
            if (category.id() == selectedId) {
                selectedIndex = names.size() - 1;
            }
        }

        displayedCategories = categories;
        categoryNames.clear();
        categoryNames.addAll(names);
        reportCategory.setSelection(selectedIndex);
    }

    private long selectedCategoryId() {
        int index = reportCategory.getSelectedItemPosition();
        return index > 0 ? displayedCategories.get(index - 1).id() : restoredCategoryId;
    }

    private void renderRows(List<ReportDisplay> reports) {
        if (displayedReports.equals(reports)) {
            return;
        }

        displayedReports = reports;
        reportList.removeAllViews();
        for (var report : reports) {
            View row = getLayoutInflater().inflate(R.layout.report_row, reportList, false);
            TextView title = row.findViewById(R.id.report_row_title);
            TextView details = row.findViewById(R.id.report_row_details);
            TextView description = row.findViewById(R.id.report_row_description);
            title.setText(getString(R.string.report_row_title, report.id(), report.title()));
            details.setText(getString(R.string.report_row_details, report.category(), report.location(),
                    report.reporter(), report.status()));
            description.setText(report.description());
            reportList.addView(row);
        }
    }

    private void clearDraft() {
        reportTitle.getText().clear();
        reportLocation.getText().clear();
        reportDescription.getText().clear();
        restoredCategoryId = 0;
        reportCategory.setSelection(0);
    }

    private void restoreDraft(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        reportTitle.setText(savedInstanceState.getString(DRAFT_TITLE, ""));
        reportLocation.setText(savedInstanceState.getString(DRAFT_LOCATION, ""));
        reportDescription.setText(savedInstanceState.getString(DRAFT_DESCRIPTION, ""));
        restoredCategoryId = savedInstanceState.getLong(DRAFT_CATEGORY);
        renderedSubmissionId = savedInstanceState.getLong(RENDERED_SUBMISSION);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DRAFT_TITLE, reportTitle.getText().toString());
        outState.putString(DRAFT_LOCATION, reportLocation.getText().toString());
        outState.putString(DRAFT_DESCRIPTION, reportDescription.getText().toString());
        outState.putLong(DRAFT_CATEGORY, selectedCategoryId());
        outState.putLong(RENDERED_SUBMISSION, renderedSubmissionId);
    }
}
