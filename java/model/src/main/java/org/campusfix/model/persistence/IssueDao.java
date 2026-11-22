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
            SELECT i.id, i.reporter_id, i.category_id, i.title, i.description,
                   i.location, i.status, i.resolution_note,
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
                INSERT INTO issues (reporter_id, category_id, title, description, location, status, resolution_note)
                SELECT ?, id, ?, ?, ?, ?, ? FROM categories WHERE id = ? AND active
                RETURNING id, reporter_id, category_id, title, description, location, status, resolution_note
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, issue.reporterId());
            statement.setString(2, issue.title());
            statement.setString(3, issue.description());
            statement.setString(4, issue.location());
            statement.setString(5, issue.status().name());
            statement.setString(6, issue.resolutionNote());
            statement.setLong(7, issue.categoryId());
            return queryIssue(statement)
                    .orElseThrow(() -> new IllegalArgumentException("Select an active category."));
        }
    }

    @Override
    public Optional<Issue> read(long id) throws SQLException {
        String sql = """
                SELECT id, reporter_id, category_id, title, description, location, status, resolution_note
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
                UPDATE issues SET category_id = ?, title = ?, description = ?, location = ?,
                    status = ?, resolution_note = ?
                WHERE id = ?
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, issue.categoryId());
            statement.setString(2, issue.title());
            statement.setString(3, issue.description());
            statement.setString(4, issue.location());
            statement.setString(5, issue.status().name());
            statement.setString(6, issue.resolutionNote());
            statement.setLong(7, issue.id());
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

    public boolean updateOpen(Issue issue) throws SQLException {
        String sql = """
                UPDATE issues
                SET category_id = category.id, title = ?, description = ?, location = ?
                FROM categories category
                WHERE issues.id = ? AND issues.reporter_id = ?
                  AND issues.status = 'OPEN'
                  AND category.id = ? AND category.active
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, issue.title());
            statement.setString(2, issue.description());
            statement.setString(3, issue.location());
            statement.setLong(4, issue.id());
            statement.setLong(5, issue.reporterId());
            statement.setLong(6, issue.categoryId());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean cancelOpen(long id, long reporterId) throws SQLException {
        String sql = """
                UPDATE issues SET status = 'CANCELLED'
                WHERE id = ? AND reporter_id = ? AND status = 'OPEN'
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, reporterId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean startWork(long id) throws SQLException {
        String sql = "UPDATE issues SET status = 'IN_PROGRESS' WHERE id = ? AND status = 'OPEN'";
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean resolve(long id, String resolutionNote) throws SQLException {
        String sql = """
                UPDATE issues SET status = 'RESOLVED', resolution_note = ?
                WHERE id = ? AND status = 'IN_PROGRESS'
                """;
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, resolutionNote);
            statement.setLong(2, id);
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
                Issue.Status.valueOf(row.getString("status")), row.getString("resolution_note"));
    }
}
