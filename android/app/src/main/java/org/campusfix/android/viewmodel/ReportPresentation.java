package org.campusfix.android.viewmodel;

import org.campusfix.android.R;
import org.campusfix.android.viewmodel.CampusFixViewModel.ReportDisplay;
import org.campusfix.model.domain.Issue;
import org.campusfix.model.domain.User;

enum ReportPresentation {
    REGULAR(R.string.regular_account, true) {
        @Override
        ReportActions actionsFor(Issue.Status status) {
            return new ReportActions(status == Issue.Status.OPEN, false, false);
        }
    },
    ADMINISTRATOR(R.string.admin_account, false) {
        @Override
        ReportActions actionsFor(Issue.Status status) {
            return new ReportActions(false, status == Issue.Status.OPEN, status == Issue.Status.IN_PROGRESS);
        }
    };

    private final int accountTitle;
    private final boolean showsSubmissionForm;

    ReportPresentation(int accountTitle, boolean showsSubmissionForm) {
        this.accountTitle = accountTitle;
        this.showsSubmissionForm = showsSubmissionForm;
    }

    static ReportPresentation forRole(User.Role role) {
        return switch (role) {
            case REGULAR -> REGULAR;
            case ADMIN -> ADMINISTRATOR;
        };
    }

    int accountTitle() {
        return accountTitle;
    }

    boolean showsSubmissionForm() {
        return showsSubmissionForm;
    }

    ReportDisplay toDisplay(Issue.Summary report) {
        var issue = report.issue();
        var actions = actionsFor(issue.status());
        return new ReportDisplay(issue.id(), issue.title(), issue.categoryId(), report.categoryName(),
                issue.location(), report.reporterName(), issue.status().name(), issue.description(),
                issue.resolutionNote(), actions.canManage(), actions.canStartWork(), actions.canResolve());
    }

    abstract ReportActions actionsFor(Issue.Status status);

    private record ReportActions(boolean canManage, boolean canStartWork, boolean canResolve) {
    }
}
