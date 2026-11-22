package org.campusfix.model.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.campusfix.model.domain.Category;

public final class CategoryDao implements CrudDao<Category> {
    private final DatabaseConnection database;

    public CategoryDao(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public Category create(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, active) VALUES (?, ?) RETURNING id, name, active";
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, category.name());
            statement.setBoolean(2, category.active());
            return queryCategory(statement).orElseThrow(() -> new SQLException("Insert returned no category."));
        }
    }

    @Override
    public Optional<Category> read(long id) throws SQLException {
        String sql = "SELECT id, name, active FROM categories WHERE id = ?";
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            return queryCategory(statement);
        }
    }

    @Override
    public boolean update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, active = ? WHERE id = ?";
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, category.name());
            statement.setBoolean(2, category.active());
            statement.setLong(3, category.id());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(long id) throws SQLException {
        try (var statement = database.getConnection().prepareStatement("DELETE FROM categories WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean rename(long id, String name) throws SQLException {
        String sql = "UPDATE categories SET name = ? WHERE id = ?";
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean setActive(long id, boolean active) throws SQLException {
        String sql = "UPDATE categories SET active = ? WHERE id = ?";
        try (var statement = database.getConnection().prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Category> findAll() throws SQLException {
        return queryCategories("SELECT id, name, active FROM categories ORDER BY name");
    }

    public List<Category> findActive() throws SQLException {
        return queryCategories("SELECT id, name, active FROM categories WHERE active ORDER BY name");
    }

    private List<Category> queryCategories(String sql) throws SQLException {
        try (var statement = database.getConnection().prepareStatement(sql);
             var rows = statement.executeQuery()) {
            var categories = new ArrayList<Category>();
            while (rows.next()) {
                categories.add(mapCategory(rows));
            }
            return categories;
        }
    }

    private static Optional<Category> queryCategory(PreparedStatement statement) throws SQLException {
        try (var rows = statement.executeQuery()) {
            return rows.next() ? Optional.of(mapCategory(rows)) : Optional.empty();
        }
    }

    private static Category mapCategory(ResultSet row) throws SQLException {
        return new Category(row.getLong("id"), row.getString("name"), row.getBoolean("active"));
    }
}
