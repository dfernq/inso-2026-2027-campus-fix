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
        if (reporter.role() != User.Role.REGULAR) {
            throw new IllegalArgumentException("Only regular users can submit reports.");
        }
        return issues.create(Issue.open(reporter.id(), categoryId, title, description, location));
    }

    public List<Issue.Summary> list(User viewer) throws SQLException {
        if (viewer.role() == User.Role.ADMIN) {
            return issues.findAll();
        }
        return issues.findByReporter(viewer.id());
    }
}
