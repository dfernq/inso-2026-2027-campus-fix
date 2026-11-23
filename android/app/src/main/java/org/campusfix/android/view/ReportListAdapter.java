package org.campusfix.android.view;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.campusfix.android.R;
import org.campusfix.android.viewmodel.CampusFixViewModel.ReportDisplay;

final class ReportListAdapter {
    private final LinearLayout reportList;
    private final LayoutInflater inflater;
    private final Consumer<ReportDisplay> editReport;
    private final LongConsumer cancelReport;
    private final LongConsumer startWork;
    private final LongConsumer resolveReport;
    private List<ReportDisplay> displayedReports = List.of();

    ReportListAdapter(LinearLayout reportList, Consumer<ReportDisplay> editReport,
                      LongConsumer cancelReport, LongConsumer startWork, LongConsumer resolveReport) {
        this.reportList = reportList;
        this.inflater = LayoutInflater.from(reportList.getContext());
        this.editReport = editReport;
        this.cancelReport = cancelReport;
        this.startWork = startWork;
        this.resolveReport = resolveReport;
    }

    void render(List<ReportDisplay> reports, boolean busy) {
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
        View row = inflater.inflate(R.layout.report_row, reportList, false);
        var resources = row.getResources();
        TextView title = row.findViewById(R.id.report_row_title);
        TextView details = row.findViewById(R.id.report_row_details);
        TextView description = row.findViewById(R.id.report_row_description);
        title.setText(resources.getString(R.string.report_row_title, report.id(), report.title()));
        details.setText(resources.getString(R.string.report_row_details, report.category(), report.location(),
                report.reporter(), report.status()));
        description.setText(report.description());

        TextView note = row.findViewById(R.id.report_row_resolution_note);
        if (report.resolutionNote() == null) {
            note.setVisibility(View.GONE);
        } else {
            note.setText(resources.getString(R.string.report_row_resolution_note, report.resolutionNote()));
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
        edit.setOnClickListener(view -> editReport.accept(report));
        cancel.setOnClickListener(view -> cancelReport.accept(report.id()));
        start.setOnClickListener(view -> startWork.accept(report.id()));
        resolve.setOnClickListener(view -> resolveReport.accept(report.id()));
        return row;
    }
}
