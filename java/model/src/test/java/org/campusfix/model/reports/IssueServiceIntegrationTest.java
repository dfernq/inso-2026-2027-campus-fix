package org.campusfix.model.reports;

import java.sql.SQLException;
import java.util.UUID;
import org.campusfix.model.TestDatabase;
import org.campusfix.model.domain.Category;
import org.campusfix.model.domain.Issue;
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
class IssueServiceIntegrationTest {
    @Test
    void submitsAReportAndListsItAccordingToTheAccountRole() throws SQLException {
        try (var database = TestDatabase.open()) {
            var users = new UserDao(database);
            var issues = new IssueDao(database);
            var service = new IssueService(issues, new CategoryDao(database));
            var alice = users.findByUsername("alice").orElseThrow().user();
            var bob = users.findByUsername("bob").orElseThrow().user();
            var admin = users.findByUsername("admin").orElseThrow().user();
            var category = service.activeCategories().stream()
                    .filter(item -> item.name().equals("Plumbing")).findFirst().orElseThrow();

            var issue = service.submit(alice, category.id(), "Leaking tap", "Water on the floor.", "Library");
            try {
                var expected = new Issue.Summary(issue, "alice", "Plumbing");
                assertTrue(service.list(alice).contains(expected));
                assertFalse(service.list(bob).contains(expected));
                assertTrue(service.list(admin).contains(expected));
                assertEquals(issue, issues.read(issue.id()).orElseThrow());
                assertThrows(IllegalArgumentException.class,
                        () -> service.submit(admin, category.id(), "Leaking tap", "Water on the floor.", "Library"));
            } finally {
                issues.delete(issue.id());
            }
        }
    }

    @Test
    void editsAndCancelsOnlyYourOwnOpenReport() throws SQLException {
        try (var database = TestDatabase.open()) {
            var users = new UserDao(database);
            var issues = new IssueDao(database);
            var service = new IssueService(issues, new CategoryDao(database));
            var alice = users.findByUsername("alice").orElseThrow().user();
            var bob = users.findByUsername("bob").orElseThrow().user();
            var admin = users.findByUsername("admin").orElseThrow().user();
            long category = service.activeCategories().get(0).id();
            var issue = service.submit(alice, category, "Broken light", "The light flickers.", "Library");

            try {
                assertThrows(IllegalArgumentException.class,
                        () -> service.edit(bob, issue.id(), category, "Other title", "Other details", "Hall"));
                assertThrows(IllegalArgumentException.class, () -> service.cancel(bob, issue.id()));
                assertThrows(IllegalArgumentException.class, () -> service.cancel(admin, issue.id()));
                assertEquals(issue, issues.read(issue.id()).orElseThrow());

                service.edit(alice, issue.id(), category, "Light has gone out", "No light in the hall.", "Hall");
                var edited = issues.read(issue.id()).orElseThrow();
                assertEquals("Light has gone out", edited.title());
                assertEquals("No light in the hall.", edited.description());
                assertEquals("Hall", edited.location());
                assertEquals(alice.id(), edited.reporterId());
                assertEquals(Issue.Status.OPEN, edited.status());

                service.cancel(alice, issue.id());
                var cancelled = issues.read(issue.id()).orElseThrow();
                assertEquals(Issue.Status.CANCELLED, cancelled.status());
                assertEquals(edited.title(), cancelled.title());
                assertTrue(service.list(alice).stream().anyMatch(row -> row.issue().equals(cancelled)));
                assertTrue(service.list(admin).stream().anyMatch(row -> row.issue().equals(cancelled)));
                assertThrows(IllegalArgumentException.class,
                        () -> service.edit(alice, issue.id(), category, "Late edit", "Details", "Hall"));
                assertThrows(IllegalArgumentException.class, () -> service.cancel(alice, issue.id()));
            } finally {
                issues.delete(issue.id());
            }
        }
    }

    @Test
    void rejectsSubmissionsAndEditsWithAnInactiveCategory() throws SQLException {
        try (var database = TestDatabase.open()) {
            var categories = new CategoryDao(database);
            var issues = new IssueDao(database);
            var service = new IssueService(issues, categories);
            var alice = new UserDao(database).findByUsername("alice").orElseThrow().user();
            var name = String.format("Inactive %s", UUID.randomUUID());
            var category = categories.create(new Category(0, name, false));
            try {
                assertFalse(service.activeCategories().contains(category));
                assertThrows(IllegalArgumentException.class,
                        () -> service.submit(alice, category.id(), "Leaking tap", "Water on the floor.", "Library"));

                long activeCategory = service.activeCategories().get(0).id();
                var issue = service.submit(alice, activeCategory, "Leaking tap", "Water on the floor.", "Library");
                try {
                    assertThrows(IllegalArgumentException.class,
                            () -> service.edit(alice, issue.id(), category.id(), "Leaking tap", "Still leaking", "Library"));
                    assertEquals(issue, issues.read(issue.id()).orElseThrow());
                } finally {
                    issues.delete(issue.id());
                }
            } finally {
                categories.delete(category.id());
            }
        }
    }
}
