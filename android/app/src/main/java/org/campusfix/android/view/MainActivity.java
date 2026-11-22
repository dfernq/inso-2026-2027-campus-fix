package org.campusfix.android.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private static final String DRAFT_ISSUE = "draftIssue";
    private static final String RENDERED_SAVE_COUNT = "renderedSaveCount";
    private static final String RESOLUTION_ISSUE = "resolutionIssue";
    private static final String RESOLUTION_NOTE = "resolutionNote";

    private CampusFixViewModel viewModel;
    private AlertDialog cancellationDialog;
    private ScrollView screen;
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
    private TextView reportFormHeading;
    private EditText reportTitle;
    private EditText reportLocation;
    private EditText reportDescription;
    private Spinner reportCategory;
    private Button submit;
    private Button discardEdit;
    private View resolutionForm;
    private TextView resolutionHeading;
    private EditText resolutionNote;
    private Button resolveButton;
    private Button discardResolution;
    private ArrayAdapter<String> categoryNames;
    private List<CategoryOption> displayedCategories = List.of();
    private List<ReportDisplay> displayedReports = List.of();
    private long restoredCategoryId;
    private long editingIssueId;
    private long resolvingIssueId;
    private long renderedSaveCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        configureInsets();
        bindViews();
        restoreDraft(savedInstanceState);

        var application = (CampusFixApplication) getApplication();
        viewModel = new ViewModelProvider(this, application.viewModelFactory())
                .get(CampusFixViewModel.class);
        bindActions();
        viewModel.state().observe(this, this::render);
    }

    private void bindViews() {
        screen = findViewById(R.id.screen);
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
        reportFormHeading = findViewById(R.id.report_form_heading);
        reportTitle = findViewById(R.id.report_title);
        reportLocation = findViewById(R.id.report_location);
        reportDescription = findViewById(R.id.report_description);
        reportCategory = findViewById(R.id.report_category);
        submit = findViewById(R.id.submit_report);
        discardEdit = findViewById(R.id.discard_edit);
        resolutionForm = findViewById(R.id.resolution_form);
        resolutionHeading = findViewById(R.id.resolution_heading);
        resolutionNote = findViewById(R.id.resolution_note);
        resolveButton = findViewById(R.id.resolve_report);
        discardResolution = findViewById(R.id.discard_resolution);

        categoryNames = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        categoryNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryNames.add(getString(R.string.choose_category));
        reportCategory.setAdapter(categoryNames);
    }

    private void bindActions() {
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
            viewModel.saveReport(editingIssueId, selectedCategoryId(), reportTitle.getText().toString(),
                    reportDescription.getText().toString(), reportLocation.getText().toString());
        });
        discardEdit.setOnClickListener(view -> {
            hideKeyboard(reportDescription);
            clearDraft();
        });
        resolveButton.setOnClickListener(view -> {
            hideKeyboard(resolutionNote);
            viewModel.resolveReport(resolvingIssueId, resolutionNote.getText().toString());
        });
        discardResolution.setOnClickListener(view -> {
            hideKeyboard(resolutionNote);
            clearResolutionDraft();
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
        clearResolutionDraft();
        renderedSaveCount = 0;
        renderCategories(List.of());
        renderRows(List.of(), false);

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

        boolean saved = renderedSaveCount != reports.savedDraftCount();
        boolean draftWasCancelled = editingIssueId != 0 && editingIssueId == reports.cancelledIssueId();
        if (saved || draftWasCancelled) {
            renderedSaveCount = reports.savedDraftCount();
            clearDraft();
        }
        renderDraftMode();
        if (resolvingIssueId != 0 && resolvingIssueId == reports.resolvedIssueId()) {
            clearResolutionDraft();
        }
        renderResolutionForm(!reports.busy());
        renderCategories(reports.categories());
        renderRows(reports.items(), reports.busy());
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
        discardEdit.setEnabled(ready);
        refresh.setEnabled(ready);
        accountPanel.setVisibility(View.VISIBLE);
    }

    private void renderCategories(List<CategoryOption> categories) {
        if (displayedCategories.equals(categories)) {
            return;
        }
        long selectedId = selectedCategoryId();
        var names = new ArrayList<String>();
        names.add(getString(R.string.choose_category));
        for (var category : categories) {
            names.add(category.name());
        }

        displayedCategories = categories;
        categoryNames.clear();
        categoryNames.addAll(names);
        selectCategory(selectedId);
    }

    private void selectCategory(long categoryId) {
        restoredCategoryId = 0;
        for (int index = 0; index < displayedCategories.size(); index++) {
            if (displayedCategories.get(index).id() == categoryId) {
                reportCategory.setSelection(index + 1);
                return;
            }
        }
        reportCategory.setSelection(0);
    }

    private long selectedCategoryId() {
        int index = reportCategory.getSelectedItemPosition();
        return index > 0 ? displayedCategories.get(index - 1).id() : restoredCategoryId;
    }

    private void renderRows(List<ReportDisplay> reports, boolean busy) {
        if (!displayedReports.equals(reports)) {
            displayedReports = reports;
            reportList.removeAllViews();
            for (var report : reports) {
                reportList.addView(createReportRow(report));
            }
        }
        for (int index = 0; index < reportList.getChildCount(); index++) {
            View row = reportList.getChildAt(index);
            row.findViewById(R.id.edit_report).setEnabled(!busy);
            row.findViewById(R.id.cancel_report).setEnabled(!busy);
            row.findViewById(R.id.start_work).setEnabled(!busy);
            row.findViewById(R.id.open_resolution).setEnabled(!busy);
        }
    }

    private View createReportRow(ReportDisplay report) {
        View row = getLayoutInflater().inflate(R.layout.report_row, reportList, false);
        TextView title = row.findViewById(R.id.report_row_title);
        TextView details = row.findViewById(R.id.report_row_details);
        TextView description = row.findViewById(R.id.report_row_description);
        title.setText(getString(R.string.report_row_title, report.id(), report.title()));
        details.setText(getString(R.string.report_row_details, report.category(), report.location(),
                report.reporter(), report.status()));
        description.setText(report.description());

        TextView note = row.findViewById(R.id.report_row_resolution_note);
        if (report.resolutionNote() == null) {
            note.setVisibility(View.GONE);
        } else {
            note.setText(getString(R.string.report_row_resolution_note, report.resolutionNote()));
        }

        View actions = row.findViewById(R.id.report_row_actions);
        boolean adminAction = report.canStartWork() || report.canResolve();
        boolean showActions = report.canManage() || adminAction;
        actions.setVisibility(showActions ? View.VISIBLE : View.GONE);

        View edit = row.findViewById(R.id.edit_report);
        View cancel = row.findViewById(R.id.cancel_report);
        View start = row.findViewById(R.id.start_work);
        View resolve = row.findViewById(R.id.open_resolution);
        edit.setVisibility(report.canManage() ? View.VISIBLE : View.GONE);
        cancel.setVisibility(report.canManage() ? View.VISIBLE : View.GONE);
        start.setVisibility(report.canStartWork() ? View.VISIBLE : View.GONE);
        resolve.setVisibility(report.canResolve() ? View.VISIBLE : View.GONE);
        edit.setOnClickListener(view -> showReportForEditing(report));
        cancel.setOnClickListener(view -> confirmCancellation(report.id()));
        start.setOnClickListener(view -> viewModel.startWork(report.id()));
        resolve.setOnClickListener(view -> openResolution(report.id()));
        return row;
    }

    private void openResolution(long issueId) {
        if (resolvingIssueId != issueId) {
            resolutionNote.getText().clear();
            resolvingIssueId = issueId;
        }
        renderResolutionForm(true);
        screen.smoothScrollTo(0, accountPanel.getTop() + resolutionForm.getTop());
    }

    private void renderResolutionForm(boolean ready) {
        boolean editing = resolvingIssueId != 0;
        resolutionForm.setVisibility(editing ? View.VISIBLE : View.GONE);
        if (editing) {
            resolutionHeading.setText(getString(R.string.resolve_report_heading, resolvingIssueId));
        }
        resolutionNote.setEnabled(ready);
        resolveButton.setEnabled(ready);
        discardResolution.setEnabled(ready);
    }

    private void clearResolutionDraft() {
        resolvingIssueId = 0;
        resolutionNote.getText().clear();
        resolutionForm.setVisibility(View.GONE);
    }

    private void showReportForEditing(ReportDisplay report) {
        editingIssueId = report.id();
        reportTitle.setText(report.title());
        reportLocation.setText(report.location());
        reportDescription.setText(report.description());
        selectCategory(report.categoryId());
        renderDraftMode();
        screen.smoothScrollTo(0, accountPanel.getTop() + reportForm.getTop());
    }

    private void confirmCancellation(long issueId) {
        cancellationDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.cancel_report)
                .setMessage(getString(R.string.confirm_cancel_report, issueId))
                .setNegativeButton(R.string.keep_report, null)
                .setPositiveButton(R.string.cancel_report, (dialog, which) -> viewModel.cancelReport(issueId))
                .show();
    }

    private void renderDraftMode() {
        if (editingIssueId == 0) {
            reportFormHeading.setText(R.string.new_report);
            submit.setText(R.string.submit_report);
            discardEdit.setVisibility(View.GONE);
            return;
        }

        reportFormHeading.setText(getString(R.string.edit_report_heading, editingIssueId));
        submit.setText(R.string.save_report_changes);
        discardEdit.setVisibility(View.VISIBLE);
    }

    private void clearDraft() {
        editingIssueId = 0;
        reportTitle.getText().clear();
        reportLocation.getText().clear();
        reportDescription.getText().clear();
        restoredCategoryId = 0;
        reportCategory.setSelection(0);
        renderDraftMode();
    }

    private void restoreDraft(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        reportTitle.setText(savedInstanceState.getString(DRAFT_TITLE, ""));
        reportLocation.setText(savedInstanceState.getString(DRAFT_LOCATION, ""));
        reportDescription.setText(savedInstanceState.getString(DRAFT_DESCRIPTION, ""));
        restoredCategoryId = savedInstanceState.getLong(DRAFT_CATEGORY);
        editingIssueId = savedInstanceState.getLong(DRAFT_ISSUE);
        renderedSaveCount = savedInstanceState.getLong(RENDERED_SAVE_COUNT);
        resolvingIssueId = savedInstanceState.getLong(RESOLUTION_ISSUE);
        resolutionNote.setText(savedInstanceState.getString(RESOLUTION_NOTE, ""));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DRAFT_TITLE, reportTitle.getText().toString());
        outState.putString(DRAFT_LOCATION, reportLocation.getText().toString());
        outState.putString(DRAFT_DESCRIPTION, reportDescription.getText().toString());
        outState.putLong(DRAFT_CATEGORY, selectedCategoryId());
        outState.putLong(DRAFT_ISSUE, editingIssueId);
        outState.putLong(RENDERED_SAVE_COUNT, renderedSaveCount);
        outState.putLong(RESOLUTION_ISSUE, resolvingIssueId);
        outState.putString(RESOLUTION_NOTE, resolutionNote.getText().toString());
    }

    @Override
    protected void onDestroy() {
        if (cancellationDialog != null) {
            cancellationDialog.dismiss();
        }
        super.onDestroy();
    }
}
