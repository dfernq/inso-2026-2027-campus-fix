package org.campusfix.desktop.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public final class CampusFixView {
    public record CategoryOption(long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    public record ReportRow(long id, String title, long categoryId, String category,
                            String location, String reporter, String status,
                            String description, String resolutionNote,
                            boolean canManage, boolean canStartWork, boolean canResolve) {
    }

    private final JFrame frame = new JFrame("CampusFix");
    private final CardLayout layout = new CardLayout();
    private final JPanel cards = new JPanel(layout);
    private final CardLayout accountLayout = new CardLayout();
    private final JPanel accountBody = new JPanel(accountLayout);
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JButton signIn = new JButton("Sign in");
    private final JButton check = new JButton("Check database connection");
    private final JLabel status = new JLabel();
    private final JLabel accountTitle = new JLabel();
    private final JLabel accountName = new JLabel();
    private final JButton signOut = new JButton("Sign out");
    private final JButton refresh = new JButton("Refresh");
    private final JButton categorySection = new JButton("Categories");
    private final JLabel reportListTitle = new JLabel();
    private final JLabel reportStatus = new JLabel(" ");
    private final JTextField reportTitle = new JTextField();
    private final JTextField reportLocation = new JTextField();
    private final JComboBox<CategoryOption> reportCategory = new JComboBox<>();
    private final JTextArea reportDescription = new JTextArea(3, 28);
    private final JButton submit = new JButton("Submit report");
    private final JButton discardEdit = new JButton("Discard edit");
    private final JButton edit = new JButton("Edit report");
    private final JButton cancelReport = new JButton("Cancel report");
    private final JButton startWork = new JButton("Start work");
    private final JButton resolveReport = new JButton("Resolve report");
    private final JTextArea resolutionNote = new JTextArea(3, 28);
    private final JPanel reportActions = new JPanel(new FlowLayout(FlowLayout.LEADING));
    private final JPanel reportForm;
    private final JPanel resolutionForm;
    private final JTextArea selectedDescription = new JTextArea(3, 28);
    private final ReportTableModel reportTable = new ReportTableModel();
    private final JTable reports = new JTable(reportTable);
    private boolean reportBusy;
    private long resolutionReportId;

    public CampusFixView(CategoryPanel categories) {
        reportForm = createReportForm();
        resolutionForm = createResolutionForm();
        cards.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        cards.add(createSignInPanel(), "sign-in");
        cards.add(createAccountPanel(categories), "account");
        frame.setContentPane(cards);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private JPanel createSignInPanel() {
        var fields = new JPanel(new GridLayout(0, 2, 8, 8));
        addField(fields, "Username", username);
        addField(fields, "Password", password);

        var actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
        actions.add(signIn);
        actions.add(check);

        var panel = new JPanel(new BorderLayout(0, 12));
        panel.add(fields, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAccountPanel(CategoryPanel categories) {
        var heading = new JPanel(new GridLayout(0, 1, 0, 6));
        heading.add(accountTitle);
        heading.add(accountName);

        var actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.add(categorySection);
        actions.add(refresh);
        actions.add(signOut);

        var header = new JPanel(new BorderLayout());
        header.add(heading, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        var footer = new JPanel(new BorderLayout(0, 8));
        footer.add(resolutionForm, BorderLayout.NORTH);
        footer.add(reportForm, BorderLayout.CENTER);
        footer.add(reportStatus, BorderLayout.SOUTH);

        var reportsPanel = new JPanel(new BorderLayout(0, 12));
        reportsPanel.add(createReportList(), BorderLayout.CENTER);
        reportsPanel.add(footer, BorderLayout.SOUTH);
        accountBody.add(reportsPanel, "reports");
        accountBody.add(categories, "categories");

        var panel = new JPanel(new BorderLayout(0, 12));
        panel.add(header, BorderLayout.NORTH);
        panel.add(accountBody, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReportList() {
        reports.setFillsViewportHeight(true);
        reports.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reports.getColumnModel().getColumn(0).setMaxWidth(60);
        reports.getSelectionModel().addListSelectionListener(event -> showSelectedReport());
        selectedDescription.setEditable(false);
        selectedDescription.setLineWrap(true);
        selectedDescription.setWrapStyleWord(true);
        var details = new JScrollPane(selectedDescription);
        details.setBorder(BorderFactory.createTitledBorder("Report description"));

        reportActions.add(edit);
        reportActions.add(cancelReport);
        reportActions.add(startWork);
        var reportDetails = new JPanel(new BorderLayout());
        reportDetails.add(details, BorderLayout.CENTER);
        reportDetails.add(reportActions, BorderLayout.SOUTH);

        var panel = new JPanel(new BorderLayout(0, 8));
        panel.add(reportListTitle, BorderLayout.NORTH);
        panel.add(new JScrollPane(reports), BorderLayout.CENTER);
        panel.add(reportDetails, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createReportForm() {
        var fields = new JPanel(new GridLayout(0, 2, 8, 6));
        addField(fields, "Title", reportTitle);
        addField(fields, "Location", reportLocation);
        addField(fields, "Category", reportCategory);

        reportDescription.setLineWrap(true);
        reportDescription.setWrapStyleWord(true);
        var descriptionLabel = new JLabel("Description");
        descriptionLabel.setLabelFor(reportDescription);
        var description = new JPanel(new BorderLayout(0, 4));
        description.add(descriptionLabel, BorderLayout.NORTH);
        description.add(new JScrollPane(reportDescription), BorderLayout.CENTER);

        var actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
        actions.add(submit);
        actions.add(discardEdit);
        var form = new JPanel(new BorderLayout(0, 8));
        form.setBorder(BorderFactory.createTitledBorder("New maintenance report"));
        form.add(fields, BorderLayout.NORTH);
        form.add(description, BorderLayout.CENTER);
        form.add(actions, BorderLayout.SOUTH);
        return form;
    }

    private JPanel createResolutionForm() {
        resolutionNote.setLineWrap(true);
        resolutionNote.setWrapStyleWord(true);
        var heading = new JLabel("Resolution note");
        heading.setLabelFor(resolutionNote);

        var actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        actions.add(resolveReport);
        var form = new JPanel(new BorderLayout(0, 4));
        form.add(heading, BorderLayout.NORTH);
        form.add(new JScrollPane(resolutionNote), BorderLayout.CENTER);
        form.add(actions, BorderLayout.SOUTH);
        form.setVisible(false);
        return form;
    }

    private static void addField(JPanel fields, String name, JComponent field) {
        var label = new JLabel(name);
        label.setLabelFor(field);
        fields.add(label);
        fields.add(field);
    }

    public ReportRow selectedReport() {
        int index = reports.getSelectedRow();
        return index < 0 ? null : reportTable.rowAt(index);
    }

    private void showSelectedReport() {
        var report = selectedReport();
        if (report != null && report.id() != resolutionReportId) {
            resolutionReportId = report.id();
            resolutionNote.setText("");
        }

        String description = report == null ? "" : report.description();
        if (report != null && report.resolutionNote() != null) {
            description = String.format("%s%n%nResolution note%n%s", description, report.resolutionNote());
        }
        selectedDescription.setText(description);
        selectedDescription.setCaretPosition(0);
        updateReportActions();
    }

    private void updateReportActions() {
        var report = selectedReport();
        boolean manageable = report != null && report.canManage();
        boolean enabled = !reportBusy && manageable;
        edit.setEnabled(enabled);
        cancelReport.setEnabled(enabled);

        boolean canStartWork = report != null && report.canStartWork();
        boolean canResolve = report != null && report.canResolve();
        startWork.setEnabled(!reportBusy && canStartWork);
        resolveReport.setEnabled(!reportBusy && canResolve);
        resolutionNote.setEnabled(!reportBusy);
        resolutionForm.setVisible(canResolve);
    }

    public void onSignIn(Runnable action) {
        signIn.addActionListener(event -> action.run());
    }

    public void onCheck(Runnable action) {
        check.addActionListener(event -> action.run());
    }

    public void onSignOut(Runnable action) {
        signOut.addActionListener(event -> action.run());
    }

    public void onRefresh(Runnable action) {
        refresh.addActionListener(event -> action.run());
    }

    public void onSwitchAccountSection(Runnable action) {
        categorySection.addActionListener(event -> action.run());
    }

    public void onSubmitReport(Runnable action) {
        submit.addActionListener(event -> action.run());
    }

    public void onEditReport(Runnable action) {
        edit.addActionListener(event -> action.run());
    }

    public void onCancelReport(Runnable action) {
        cancelReport.addActionListener(event -> action.run());
    }

    public void onDiscardEdit(Runnable action) {
        discardEdit.addActionListener(event -> action.run());
    }

    public void onStartWork(Runnable action) {
        startWork.addActionListener(event -> action.run());
    }

    public void onResolveReport(Runnable action) {
        resolveReport.addActionListener(event -> action.run());
    }

    public void onClose(Runnable action) {
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                action.run();
            }
        });
    }

    public String username() {
        return username.getText();
    }

    public char[] takePassword() {
        char[] value = password.getPassword();
        password.setText("");
        return value;
    }

    public String reportTitle() {
        return reportTitle.getText();
    }

    public String reportLocation() {
        return reportLocation.getText();
    }

    public String reportDescription() {
        return reportDescription.getText();
    }

    public String resolutionNote() {
        return resolutionNote.getText();
    }

    public void clearResolutionNote(long id) {
        if (resolutionReportId == id) {
            resolutionNote.setText("");
        }
    }

    public long reportCategoryId() {
        var selected = (CategoryOption) reportCategory.getSelectedItem();
        return selected == null ? 0 : selected.id();
    }

    public void showStatus(String message, boolean busy) {
        status.setText(message);
        username.setEnabled(!busy);
        password.setEnabled(!busy);
        signIn.setEnabled(!busy);
        check.setEnabled(!busy);
    }

    public void showReportStatus(String message, boolean busy) {
        reportBusy = busy;
        reportStatus.setText(message);
        setAccountActionsEnabled(!busy);
        submit.setEnabled(!busy);
        reportTitle.setEnabled(!busy);
        reportLocation.setEnabled(!busy);
        reportDescription.setEnabled(!busy);
        reportCategory.setEnabled(!busy);
        discardEdit.setEnabled(!busy);
        updateReportActions();
    }

    public void setAccountActionsEnabled(boolean enabled) {
        refresh.setEnabled(enabled);
        categorySection.setEnabled(enabled);
    }

    public void showCategorySection(boolean managingCategories) {
        accountLayout.show(accountBody, managingCategories ? "categories" : "reports");
        categorySection.setText(managingCategories ? "Reports" : "Categories");
    }

    public void showCategories(List<CategoryOption> categories) {
        long selectedId = reportCategoryId();
        reportCategory.removeAllItems();
        CategoryOption selected = null;
        for (var category : categories) {
            reportCategory.addItem(category);
            if (category.id() == selectedId) {
                selected = category;
            }
        }
        reportCategory.setSelectedItem(selected);
    }

    public void showReports(List<ReportRow> rows) {
        var previousSelection = selectedReport();
        reports.clearSelection();
        reportTable.setRows(rows);
        if (!rows.isEmpty()) {
            int selectedIndex = previousSelection == null ? 0 : reportTable.indexOfReport(previousSelection.id());
            selectedIndex = Math.max(0, selectedIndex);
            reports.setRowSelectionInterval(selectedIndex, selectedIndex);
        }
        updateReportActions();
    }

    public void showReportForEditing(ReportRow report) {
        String title = String.format("Edit report #%d", report.id());
        reportForm.setBorder(BorderFactory.createTitledBorder(title));
        submit.setText("Save changes");
        discardEdit.setVisible(true);
        reportTitle.setText(report.title());
        reportLocation.setText(report.location());
        reportDescription.setText(report.description());
        reportCategory.setSelectedItem(null);
        for (int index = 0; index < reportCategory.getItemCount(); index++) {
            if (reportCategory.getItemAt(index).id() == report.categoryId()) {
                reportCategory.setSelectedIndex(index);
                break;
            }
        }
        reportTitle.requestFocusInWindow();
    }

    public boolean confirmCancellation(long id) {
        String message = String.format("Cancel report #%d?", id);
        String[] options = {"Cancel report", "Keep report"};
        int choice = JOptionPane.showOptionDialog(frame, message, "Cancel report",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return choice == 0;
    }

    public void clearReportForm() {
        reportForm.setBorder(BorderFactory.createTitledBorder("New maintenance report"));
        submit.setText("Submit report");
        discardEdit.setVisible(false);
        reportTitle.setText("");
        reportLocation.setText("");
        reportDescription.setText("");
        reportCategory.setSelectedItem(null);
    }

    public void showSignIn() {
        username.setText("");
        password.setText("");
        clearReportForm();
        resolutionReportId = 0;
        resolutionNote.setText("");
        showReports(List.of());
        showCategories(List.of());
        showStatus("Sign in to CampusFix.", false);
        layout.show(cards, "sign-in");
        frame.getRootPane().setDefaultButton(signIn);
        frame.setSize(570, 215);
        frame.setLocationRelativeTo(null);
    }

    public void showAccount(String title, String name, boolean canSubmit) {
        accountTitle.setText(title);
        accountName.setText(String.format("Signed in as %s", name));
        reportListTitle.setText(canSubmit ? "My reports" : "All reports");
        reportForm.setVisible(canSubmit);
        edit.setVisible(canSubmit);
        cancelReport.setVisible(canSubmit);
        startWork.setVisible(!canSubmit);
        categorySection.setVisible(!canSubmit);
        showCategorySection(false);
        layout.show(cards, "account");
        frame.getRootPane().setDefaultButton(canSubmit ? submit : null);
        frame.setSize(800, 620);
        frame.setLocationRelativeTo(null);
    }

    public void show() {
        frame.setVisible(true);
    }

    public void close() {
        frame.dispose();
    }
}
