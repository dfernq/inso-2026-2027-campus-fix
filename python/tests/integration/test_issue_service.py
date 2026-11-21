import unittest
from uuid import uuid4

from campusfix.model.domain.category import Category
from campusfix.model.domain.issue import IssueSummary
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

    def test_rejects_submission_to_an_inactive_category(self) -> None:
        database = open_test_database(self)
        categories = CategoryDao(database)
        service = IssueService(IssueDao(database), categories)
        alice = UserDao(database).find_by_username("alice").user
        category = categories.create(Category(0, f"Inactive {uuid4()}", False))
        self.addCleanup(categories.delete, category.id)

        self.assertNotIn(category, service.active_categories())
        with self.assertRaisesRegex(ValueError, "Select an active category"):
            service.submit(alice, category.id, "Leaking tap", "Water on the floor.", "Library")
