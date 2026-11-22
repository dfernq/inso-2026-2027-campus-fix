package org.campusfix.model.reports;

import java.sql.SQLException;
import java.util.List;
import org.campusfix.model.domain.Category;
import org.campusfix.model.domain.User;
import org.campusfix.model.persistence.CategoryDao;

public final class CategoryService {
    private static final String UNIQUE_VIOLATION = "23505";

    private final CategoryDao categories;

    public CategoryService(CategoryDao categories) {
        this.categories = categories;
    }

    public List<Category> list(User administrator) throws SQLException {
        requireAdministrator(administrator);
        return categories.findAll();
    }

    public void create(User administrator, String name) throws SQLException {
        requireAdministrator(administrator);
        try {
            categories.create(new Category(0, requireName(name), true));
        } catch (SQLException exception) {
            rethrowNameError(exception);
        }
    }

    public void rename(User administrator, long categoryId, String name) throws SQLException {
        requireAdministrator(administrator);
        try {
            requireUpdated(categories.rename(categoryId, requireName(name)));
        } catch (SQLException exception) {
            rethrowNameError(exception);
        }
    }

    public void setActive(User administrator, long categoryId, boolean active) throws SQLException {
        requireAdministrator(administrator);
        requireUpdated(categories.setActive(categoryId, active));
    }

    private static void requireAdministrator(User user) {
        if (user.role() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only administrators can manage categories.");
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        return name.strip();
    }

    private static void requireUpdated(boolean updated) {
        if (!updated) {
            throw new IllegalArgumentException("The category no longer exists. Refresh the list.");
        }
    }

    private static void rethrowNameError(SQLException exception) throws SQLException {
        if (UNIQUE_VIOLATION.equals(exception.getSQLState())) {
            throw new IllegalArgumentException("A category with this name already exists.", exception);
        }
        throw exception;
    }
}
