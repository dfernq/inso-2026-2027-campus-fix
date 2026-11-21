package org.campusfix.model.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IssueTest {
    @Test
    void opensAReportAndTrimsItsFields() {
        var issue = Issue.open(7, 2, "  Leaking tap ", " Water on the floor. ", " Library ");

        assertEquals(Issue.Status.OPEN, issue.status());
        assertEquals("Leaking tap", issue.title());
        assertEquals("Water on the floor.", issue.description());
        assertEquals("Library", issue.location());
    }

    @Test
    void rejectsIncompleteOrOversizedReports() {
        assertThrows(IllegalArgumentException.class,
                () -> Issue.open(7, 0, "Leaking tap", "Water on the floor.", "Library"));
        assertThrows(IllegalArgumentException.class,
                () -> Issue.open(7, 2, " ", "Water on the floor.", "Library"));
        assertThrows(IllegalArgumentException.class,
                () -> Issue.open(7, 2, "Leaking tap", "Water on the floor.", ""));
        assertThrows(IllegalArgumentException.class,
                () -> Issue.open(7, 2, "Leaking tap", "x".repeat(2001), "Library"));
    }
}
