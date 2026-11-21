package org.campusfix.model.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.campusfix.model.domain.Issue;

public final class IssueDao implements CrudDao<Issue> {
    private static final String REPORT_QUERY = """
            SELECT i.id, i.reporter_id, i.category_id, i.title, i.description, i.location, i.status,
                   u.username AS reporter_name, c.name AS category_name
            FROM issues i
            JOIN users u ON u.id = i.reporter_id
            JOIN categories c ON c.id = i.category_id
            """;

    private final DatabaseConnection database;

    public IssueDao(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Issue create(Issue issue) throws SQLException {
        String sql = """
                INSERT INTO issues (reporter_id, category_id, title, description, location, status)
                SELECT ?, id, ?, ?, ?, ? FROM categories WHERE id = ? AND active
                RETURNING id, reporter_id, category_id, title, description, location, status
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, issue.reporterId());
            statement.setString(2, issue.title());
            statement.setString(3, issue.description());
            statement.setString(4, issue.location());
            statement.setString(5, issue.status().name());
            statement.setLong(6, issue.categoryId());
            return queryIssue(statement)
                    .orElseThrow(() -> new IllegalArgumentException("Select an active category."));
        }
    }

    @Override
    public Optional<Issue> read(long id) throws SQLException {
        String sql = """
                SELECT id, reporter_id, category_id, title, description, location, status
                FROM issues WHERE id = ?
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            return queryIssue(statement);
        }
    }

    @Override
    public boolean update(Issue issue) throws SQLException {
        String sql = """
                UPDATE issues SET category_id = ?, title = ?, description = ?, location = ?, status = ?
                WHERE id = ?
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, issue.categoryId());
            statement.setString(2, issue.title());
            statement.setString(3, issue.description());
            statement.setString(4, issue.location());
            statement.setString(5, issue.status().name());
            statement.setLong(6, issue.id());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        try (var statement = database.getConnection().prepareStatement("DELETE FROM issues WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Issue.Summary> findAll() throws SQLException {
        String sql = String.format("%s ORDER BY i.id DESC", REPORT_QUERY);
        try (var statement = database.getConnection().prepareStatement(sql)) {
            return queryReports(statement);
        }
    }

    public List<Issue.Summary> findByReporter(long reporterId) throws SQLException {
        String sql = String.format("%s WHERE i.reporter_id = ? ORDER BY i.id DESC", REPORT_QUERY);
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, reporterId);
            return queryReports(statement);
        }
    }

    private static Optional<Issue> queryIssue(PreparedStatement statement) throws SQLException {
        try (var rows = statement.executeQuery()) {
            return rows.next() ? Optional.of(mapIssue(rows)) : Optional.empty();
        }
    }

    private static List<Issue.Summary> queryReports(PreparedStatement statement) throws SQLException {
        try (var rows = statement.executeQuery()) {
            var issues = new ArrayList<Issue.Summary>();
            while (rows.next()) {
                issues.add(new Issue.Summary(mapIssue(rows),
                        rows.getString("reporter_name"), rows.getString("category_name")));
            }
            return issues;
        }
    }

    private static Issue mapIssue(ResultSet row) throws SQLException {
        return new Issue(row.getLong("id"), row.getLong("reporter_id"), row.getLong("category_id"),
                row.getString("title"), row.getString("description"), row.getString("location"),
                Issue.Status.valueOf(row.getString("status")));
    }
}
