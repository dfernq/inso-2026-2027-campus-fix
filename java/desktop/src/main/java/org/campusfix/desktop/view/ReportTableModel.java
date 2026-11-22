package org.campusfix.desktop.view;

import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.campusfix.desktop.view.CampusFixView.ReportRow;

final class ReportTableModel extends AbstractTableModel {
    private static final List<String> COLUMNS = List.of("ID", "Title", "Category", "Location", "Reporter", "Status");

    private List<ReportRow> rows = List.of();

    void setRows(List<ReportRow> rows) {
        this.rows = List.copyOf(rows);
        fireTableDataChanged();
    }

    ReportRow rowAt(int index) {
        return rows.get(index);
    }

    int indexOfReport(long reportId) {
        for (int index = 0; index < rows.size(); index++) {
            if (rowAt(index).id() == reportId) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.size();
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS.get(column);
    }

    @Override
    public Object getValueAt(int row, int column) {
        var report = rowAt(row);
        return switch (column) {
            case 0 -> report.id();
            case 1 -> report.title();
            case 2 -> report.category();
            case 3 -> report.location();
            case 4 -> report.reporter();
            case 5 -> report.status();
            default -> throw new IndexOutOfBoundsException(column);
        };
    }
}
