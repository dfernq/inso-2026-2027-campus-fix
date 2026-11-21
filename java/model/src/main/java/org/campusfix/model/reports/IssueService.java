package org.campusfix.model.reports;

import java.sql.SQLException;
import java.util.List;
import org.campusfix.model.domain.Category;
import org.campusfix.model.domain.Issue;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.CategoryDao;
import org.campusfix.model.persistence.IssueDao;

public final class IssueService {
    private final IssueDao issues;
    private final CategoryDao categories;

    public IssueService(IssueDao issues, CategoryDao categories) {
        this.issues = issues;
        this.categories = categories;
    }

    public List<Category> activeCategories() throws SQLException {
        return categories.findActive();
    }

    public Issue submit(User reporter, long categoryId, String title,
                        String description, String location) throws SQLException {
        requireRegularUser(reporter);
        return issues.create(Issue.open(reporter.id(), categoryId, title, description, location));
    }

    public void edit(User reporter, long issueId, long categoryId, String title,
                     String description, String location) throws SQLException {
        requireRegularUser(reporter);
        var issue = Issue.open(issueId, reporter.id(), categoryId, title, description, location);
        if (!issues.updateOpen(issue)) {
            throw new IllegalArgumentException(
                    "Edit an open report you submitted and select an active category.");
        }
    }

    public void cancel(User reporter, long issueId) throws SQLException {
        requireRegularUser(reporter);
        if (!issues.cancelOpen(issueId, reporter.id())) {
            throw new IllegalArgumentException("Only your open reports can be cancelled.");
        }
    }

    public List<Issue.Summary> list(User viewer) throws SQLException {
        if (viewer.role() == User.Role.ADMIN) {
            return issues.findAll();
        }
        return issues.findByReporter(viewer.id());
    }

    private static void requireRegularUser(User user) {
        if (user.role() != User.Role.REGULAR) {
            throw new IllegalArgumentException("Only regular users can submit, edit, or cancel reports.");
        }
    }
}
