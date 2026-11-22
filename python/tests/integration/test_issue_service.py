import unittest
from uuid import uuid4

from campusfix.model.domain.category import Category
from campusfix.model.domain.issue import IssueSummary, Status
from campusfix.model.persistence.category_dao import CategoryDao
from campusfix.model.persistence.issue_dao import IssueDao
from campusfix.model.persistence.user_dao import UserDao
from campusfix.model.reports.issue_service import IssueService
from database_fixture import open_test_database


class IssueServiceIntegrationTest(unittest.TestCase):
    def test_submits_a_report_and_lists_it_according_to_the_role(self) -> None:
        database = open_test_database(self)
        users = UserDao(database)
        issues = IssueDao(database)
        service = IssueService(issues, CategoryDao(database))
        alice = users.find_by_username("alice").user
        bob = users.find_by_username("bob").user
        admin = users.find_by_username("admin").user
        category = next(item for item in service.active_categories() if item.name == "Plumbing")

        issue = service.submit(alice, category.id, "Leaking tap", "Water on the floor.", "Library")
        self.addCleanup(issues.delete, issue.id)

        expected = IssueSummary(issue, "alice", "Plumbing")
        self.assertIn(expected, service.list(alice))
        self.assertNotIn(expected, service.list(bob))
        self.assertIn(expected, service.list(admin))
        self.assertEqual(issue, issues.read(issue.id))
        with self.assertRaisesRegex(ValueError, "Only regular users"):
            service.submit(admin, category.id, "Leaking tap", "Water on the floor.", "Library")

    def test_edits_and_cancels_only_your_own_open_report(self) -> None:
        database = open_test_database(self)
        users = UserDao(database)
        issues = IssueDao(database)
        service = IssueService(issues, CategoryDao(database))
        alice = users.find_by_username("alice").user
        bob = users.find_by_username("bob").user
        admin = users.find_by_username("admin").user
        category_id = service.active_categories()[0].id
        issue = service.submit(alice, category_id, "Broken light", "It flickers.", "Library")
        self.addCleanup(issues.delete, issue.id)

        with self.assertRaises(ValueError):
            service.edit(bob, issue.id, category_id, "Other title", "Other details", "Hall")
        with self.assertRaises(ValueError):
            service.cancel(bob, issue.id)
        with self.assertRaises(ValueError):
            service.cancel(admin, issue.id)
        self.assertEqual(issue, issues.read(issue.id))

        service.edit(
            alice, issue.id, category_id, "Light has gone out", "No light in the hall.", "Hall"
        )
        edited = issues.read(issue.id)
        self.assertEqual("Light has gone out", edited.title)
        self.assertEqual("No light in the hall.", edited.description)
        self.assertEqual("Hall", edited.location)
        self.assertEqual(alice.id, edited.reporter_id)
        self.assertIs(Status.OPEN, edited.status)

        service.cancel(alice, issue.id)
        cancelled = issues.read(issue.id)
        self.assertIs(Status.CANCELLED, cancelled.status)
        self.assertEqual(edited.title, cancelled.title)
        self.assertIn(cancelled, [row.issue for row in service.list(alice)])
        self.assertIn(cancelled, [row.issue for row in service.list(admin)])
        with self.assertRaises(ValueError):
            service.edit(alice, issue.id, category_id, "Late edit", "Details", "Hall")
        with self.assertRaises(ValueError):
            service.cancel(alice, issue.id)

    def test_rejects_submissions_and_edits_with_an_inactive_category(self) -> None:
        database = open_test_database(self)
        categories = CategoryDao(database)
        issues = IssueDao(database)
        service = IssueService(issues, categories)
        alice = UserDao(database).find_by_username("alice").user
        category = categories.create(Category(0, f"Inactive {uuid4()}", False))
        self.addCleanup(categories.delete, category.id)

        self.assertNotIn(category, service.active_categories())
        with self.assertRaisesRegex(ValueError, "Select an active category"):
            service.submit(alice, category.id, "Leaking tap", "Water on the floor.", "Library")

        active_category_id = service.active_categories()[0].id
        issue = service.submit(alice, active_category_id, "Leaking tap", "Wet floor.", "Library")
        self.addCleanup(issues.delete, issue.id)
        with self.assertRaises(ValueError):
            service.edit(alice, issue.id, category.id, "Leaking tap", "Still leaking.", "Library")
        self.assertEqual(issue, issues.read(issue.id))
