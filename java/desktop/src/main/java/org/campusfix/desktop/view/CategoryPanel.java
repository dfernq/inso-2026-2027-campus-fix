package org.campusfix.desktop.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public final class CategoryPanel extends JPanel {
    public record CategoryRow(long id, String name, boolean active) {
    }

    private final JTextField newName = new JTextField(22);
    private final JTextField selectedName = new JTextField(22);
    private final JButton add = new JButton("Add category");
    private final JButton rename = new JButton("Rename category");
    private final JButton toggleActive = new JButton("Deactivate category");
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Category", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<CategoryRow> rows = List.of();
    private boolean busy;

    public CategoryPanel() {
        super(new BorderLayout(0, 12));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> showSelection());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
        updateActions();
    }

    private JPanel createActions() {
        var addRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
        var newLabel = new JLabel("New category name");
        newLabel.setLabelFor(newName);
        addRow.add(newLabel);
        addRow.add(newName);
        addRow.add(add);

        var editRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
        var selectedLabel = new JLabel("Selected category name");
        selectedLabel.setLabelFor(selectedName);
        editRow.add(selectedLabel);
        editRow.add(selectedName);
        editRow.add(rename);
        editRow.add(toggleActive);

        var fields = new JPanel(new GridLayout(0, 1));
        fields.add(addRow);
        fields.add(editRow);

        var actions = new JPanel(new BorderLayout(0, 8));
        actions.add(fields, BorderLayout.CENTER);
        actions.add(status, BorderLayout.SOUTH);
        return actions;
    }

    public void onAdd(Runnable action) {
        add.addActionListener(event -> action.run());
    }

    public void onRename(Runnable action) {
        rename.addActionListener(event -> action.run());
    }

    public void onToggleActive(Runnable action) {
        toggleActive.addActionListener(event -> action.run());
    }

    public String newName() {
        return newName.getText();
    }

    public String selectedName() {
        return selectedName.getText();
    }

    public CategoryRow selectedCategory() {
        int index = table.getSelectedRow();
        return index < 0 ? null : rows.get(index);
    }

    public void showCategories(List<CategoryRow> categories) {
        var previous = selectedCategory();
        table.clearSelection();
        rows = categories;
        tableModel.setRowCount(0);
        int selection = 0;
        for (var category : categories) {
            if (previous != null && category.id() == previous.id()) {
                selection = tableModel.getRowCount();
            }
            tableModel.addRow(new Object[]{category.name(), category.active() ? "Active" : "Inactive"});
        }
        if (!categories.isEmpty()) {
            table.setRowSelectionInterval(selection, selection);
        }
        showSelection();
    }

    public void showStatus(String message, boolean busy) {
        this.busy = busy;
        status.setText(message);
        table.setEnabled(!busy);
        newName.setEnabled(!busy);
        add.setEnabled(!busy);
        updateActions();
    }

    public void clearNewName() {
        newName.setText("");
    }

    public void reset() {
        clearNewName();
        showCategories(List.of());
        showStatus(" ", false);
    }

    private void showSelection() {
        var selected = selectedCategory();
        selectedName.setText(selected == null ? "" : selected.name());
        updateActions();
    }

    private void updateActions() {
        var selected = selectedCategory();
        boolean canEdit = selected != null && !busy;
        selectedName.setEnabled(canEdit);
        rename.setEnabled(canEdit);
        toggleActive.setEnabled(canEdit);
        boolean activate = selected != null && !selected.active();
        toggleActive.setText(activate ? "Activate category" : "Deactivate category");
    }
}
