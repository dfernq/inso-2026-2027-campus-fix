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
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public final class CampusFixView {
    public record CategoryOption(long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    public record ReportRow(long id, String title, String category, String location,
                            String reporter, String status, String description) {
    }

    private final JFrame frame = new JFrame("CampusFix");
    private final CardLayout layout = new CardLayout();
    private final JPanel cards = new JPanel(layout);
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JButton signIn = new JButton("Sign in");
    private final JButton check = new JButton("Check database connection");
    private final JLabel status = new JLabel();
    private final JLabel accountTitle = new JLabel();
    private final JLabel accountName = new JLabel();
    private final JButton signOut = new JButton("Sign out");
    private final JButton refresh = new JButton("Refresh");
    private final JLabel reportListTitle = new JLabel();
    private final JLabel reportStatus = new JLabel(" ");
    private final JTextField reportTitle = new JTextField();
    private final JTextField reportLocation = new JTextField();
    private final JComboBox<CategoryOption> reportCategory = new JComboBox<>();
    private final JTextArea reportDescription = new JTextArea(3, 28);
    private final JButton submit = new JButton("Submit report");
    private final JPanel reportForm;
    private final JTextArea selectedDescription = new JTextArea(3, 28);
    private final DefaultTableModel reportTable = new DefaultTableModel(
            new String[]{"ID", "Title", "Category", "Location", "Reporter", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable reports = new JTable(reportTable);
    private List<ReportRow> displayedReports = List.of();

    public CampusFixView() {
        reportForm = createReportForm();
        cards.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        cards.add(createSignInPanel(), "sign-in");
        cards.add(createAccountPanel(), "account");
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

    private JPanel createAccountPanel() {
        var heading = new JPanel(new GridLayout(0, 1, 0, 6));
        heading.add(accountTitle);
        heading.add(accountName);

        var actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.add(refresh);
        actions.add(signOut);

        var header = new JPanel(new BorderLayout());
        header.add(heading, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        var footer = new JPanel(new BorderLayout(0, 8));
        footer.add(reportForm, BorderLayout.CENTER);
        footer.add(reportStatus, BorderLayout.SOUTH);

        var panel = new JPanel(new BorderLayout(0, 12));
        panel.add(header, BorderLayout.NORTH);
        panel.add(createReportList(), BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createReportList() {
        reports.setFillsViewportHeight(true);
        reports.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reports.getColumnModel().getColumn(0).setMaxWidth(60);
        reports.getSelectionModel().addListSelectionListener(event -> showSelectedDescription());
        selectedDescription.setEditable(false);
        selectedDescription.setLineWrap(true);
        selectedDescription.setWrapStyleWord(true);
        var details = new JScrollPane(selectedDescription);
        details.setBorder(BorderFactory.createTitledBorder("Report description"));

        var panel = new JPanel(new BorderLayout(0, 8));
        panel.add(reportListTitle, BorderLayout.NORTH);
        panel.add(new JScrollPane(reports), BorderLayout.CENTER);
        panel.add(details, BorderLayout.SOUTH);
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

        var actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        actions.add(submit);
        var form = new JPanel(new BorderLayout(0, 8));
        form.setBorder(BorderFactory.createTitledBorder("New maintenance report"));
        form.add(fields, BorderLayout.NORTH);
        form.add(description, BorderLayout.CENTER);
        form.add(actions, BorderLayout.SOUTH);
        return form;
    }

    private static void addField(JPanel fields, String name, JComponent field) {
        var label = new JLabel(name);
        label.setLabelFor(field);
        fields.add(label);
        fields.add(field);
    }

    private void showSelectedDescription() {
        int index = reports.getSelectedRow();
        String description = index < 0 ? "" : displayedReports.get(index).description();
        selectedDescription.setText(description);
        selectedDescription.setCaretPosition(0);
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

    public void onSubmitReport(Runnable action) {
        submit.addActionListener(event -> action.run());
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
        reportStatus.setText(message);
        refresh.setEnabled(!busy);
        submit.setEnabled(!busy);
        reportTitle.setEnabled(!busy);
        reportLocation.setEnabled(!busy);
        reportDescription.setEnabled(!busy);
        reportCategory.setEnabled(!busy);
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
        reports.clearSelection();
        displayedReports = rows;
        reportTable.setRowCount(0);
        for (var row : rows) {
            reportTable.addRow(new Object[]{
                    row.id(), row.title(), row.category(), row.location(), row.reporter(), row.status()
            });
        }
        if (!rows.isEmpty()) {
            reports.setRowSelectionInterval(0, 0);
        }
    }

    public void clearReportForm() {
        reportTitle.setText("");
        reportLocation.setText("");
        reportDescription.setText("");
        reportCategory.setSelectedItem(null);
    }

    public void showSignIn() {
        username.setText("");
        password.setText("");
        clearReportForm();
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
