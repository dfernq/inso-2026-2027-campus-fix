package org.campusfix.desktop.controller;

import org.campusfix.desktop.view.CampusFixView.ReportRow;
import org.campusfix.model.domain.Issue;
import org.campusfix.model.domain.User;

enum ReportPresentation {
    REGULAR("Regular account", true) {
        @Override
        ReportActions actionsFor(Issue.Status status) {
            return new ReportActions(status == Issue.Status.OPEN, false, false);
        }
    },
    ADMINISTRATOR("Administrator account", false) {
        @Override
        ReportActions actionsFor(Issue.Status status) {
            return new ReportActions(false, status == Issue.Status.OPEN, status == Issue.Status.IN_PROGRESS);
        }
    };

    private final String accountTitle;
    private final boolean showsSubmissionForm;

    ReportPresentation(String accountTitle, boolean showsSubmissionForm) {
        this.accountTitle = accountTitle;
        this.showsSubmissionForm = showsSubmissionForm;
    }

    static ReportPresentation forRole(User.Role role) {
        return switch (role) {
            case REGULAR -> REGULAR;
            case ADMIN -> ADMINISTRATOR;
        };
    }

    String accountTitle() {
        return accountTitle;
    }

    boolean showsSubmissionForm() {
        return showsSubmissionForm;
    }

    ReportRow toRow(Issue.Summary report) {
        var issue = report.issue();
        var actions = actionsFor(issue.status());
        return new ReportRow(issue.id(), issue.title(), issue.categoryId(), report.categoryName(),
                issue.location(), report.reporterName(), issue.status().name(), issue.description(),
                issue.resolutionNote(), actions.canManage(), actions.canStartWork(), actions.canResolve());
    }

    abstract ReportActions actionsFor(Issue.Status status);

    private record ReportActions(boolean canManage, boolean canStartWork, boolean canResolve) {
    }
}
