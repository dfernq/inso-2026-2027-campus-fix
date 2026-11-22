package org.campusfix.model.reports;

import java.sql.SQLException;
import java.util.UUID;
import org.campusfix.model.TestDatabase;
import org.campusfix.model.persistence.CategoryDao;
import org.campusfix.model.persistence.IssueDao;
import org.campusfix.model.persistence.UserDao;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class CategoryServiceIntegrationTest {
    @Test
    void restrictsCategoryManagementAndRejectsBlankOrDuplicateNames() throws SQLException {
        try (var database = TestDatabase.open()) {
            var users = new UserDao(database);
            var service = new CategoryService(new CategoryDao(database));
            var alice = users.findByUsername("alice").orElseThrow().user();
            var admin = users.findByUsername("admin").orElseThrow().user();
            var seed = service.list(admin).get(0);

            assertThrows(IllegalArgumentException.class, () -> service.list(alice));
            assertThrows(IllegalArgumentException.class, () -> service.create(alice, "Other category"));
            assertThrows(IllegalArgumentException.class, () -> service.rename(alice, seed.id(), "New name"));
            assertThrows(IllegalArgumentException.class, () -> service.setActive(alice, seed.id(), false));
            assertThrows(IllegalArgumentException.class, () -> service.create(admin, "  "));
            assertThrows(IllegalArgumentException.class,
                    () -> service.create(admin, String.format(" %s ", seed.name())));
            assertTrue(service.list(admin).contains(seed));
        }
    }

    @Test
    void renamesAndDeactivatesCategoryWithoutLosingItsReports() throws SQLException {
        try (var database = TestDatabase.open()) {
            var users = new UserDao(database);
            var categories = new CategoryDao(database);
            var issues = new IssueDao(database);
            var service = new CategoryService(categories);
            var reporting = new IssueService(issues, categories);
            var alice = users.findByUsername("alice").orElseThrow().user();
            var admin = users.findByUsername("admin").orElseThrow().user();
            String name = String.format("Outdoor maintenance %s", UUID.randomUUID());

            service.create(admin, String.format(" %s ", name));
            var category = service.list(admin).stream()
                    .filter(item -> item.name().equals(name)).findFirst().orElseThrow();
            long id = category.id();
            try {
                assertTrue(category.active());
                var issue = reporting.submit(alice, id, "Damaged bench", "A slat is loose.", "Courtyard");
                try {
                    String renamed = String.format("%s updated", name);
                    service.rename(admin, id, renamed);
                    service.setActive(admin, id, false);

                    var stored = categories.read(id).orElseThrow();
                    assertFalse(stored.active());
                    assertTrue(service.list(admin).contains(stored));
                    assertFalse(reporting.activeCategories().stream().anyMatch(item -> item.id() == id));
                    assertThrows(IllegalArgumentException.class,
                            () -> reporting.submit(alice, id, "Another bench", "Damage", "Courtyard"));
                    var listed = reporting.list(alice).stream()
                            .filter(row -> row.issue().id() == issue.id()).findFirst().orElseThrow();
                    assertEquals(id, listed.issue().categoryId());
                    assertEquals(renamed, listed.categoryName());

                    service.setActive(admin, id, true);
                    assertTrue(reporting.activeCategories().stream().anyMatch(item -> item.id() == id));
                } finally {
                    issues.delete(issue.id());
                }
            } finally {
                categories.delete(id);
            }
        }
    }
}
