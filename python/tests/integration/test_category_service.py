import unittest
from uuid import uuid4

from campusfix.model.domain.issue import IssueSummary
from campusfix.model.persistence.category_dao import CategoryDao
from campusfix.model.persistence.issue_dao import IssueDao
from campusfix.model.persistence.user_dao import UserDao
from campusfix.model.reports.category_service import CategoryService
from campusfix.model.reports.issue_service import IssueService
from database_fixture import open_test_database


class CategoryServiceIntegrationTest(unittest.TestCase):
    def test_restricts_category_management_and_rejects_invalid_names(self) -> None:
        database = open_test_database(self)
        users = UserDao(database)
        service = CategoryService(CategoryDao(database))
        alice = users.find_by_username("alice").user
        admin = users.find_by_username("admin").user
        category = service.list(admin)[0]

        with self.assertRaisesRegex(ValueError, "Only administrators"):
            service.list(alice)
        with self.assertRaisesRegex(ValueError, "Only administrators"):
            service.create(alice, "Outdoor repairs")
        with self.assertRaisesRegex(ValueError, "Only administrators"):
            service.rename(alice, category.id, "New name")
        with self.assertRaisesRegex(ValueError, "Only administrators"):
            service.set_active(alice, category.id, False)
        with self.assertRaisesRegex(ValueError, "Category name is required"):
            service.create(admin, "  ")
        with self.assertRaisesRegex(ValueError, "already exists"):
            service.create(admin, f" {category.name} ")
        self.assertIn(category, service.list(admin))

    def test_renames_and_deactivates_category_without_losing_its_reports(self) -> None:
        database = open_test_database(self)
        users = UserDao(database)
        categories = CategoryDao(database)
        issues = IssueDao(database)
        service = CategoryService(categories)
        reporting = IssueService(issues, categories)
        alice = users.find_by_username("alice").user
        admin = users.find_by_username("admin").user
        name = f"Outdoor maintenance {uuid4()}"
        renamed = f"{name} updated"

        service.create(admin, f" {name} ")
        category = next(item for item in service.list(admin) if item.name == name)
        self.addCleanup(categories.delete, category.id)
        issue = reporting.submit(
            alice, category.id, "Damaged bench", "A slat is loose.", "Courtyard"
        )
        self.addCleanup(issues.delete, issue.id)

        service.rename(admin, category.id, renamed)
        service.set_active(admin, category.id, False)

        stored = next(item for item in service.list(admin) if item.id == category.id)
        self.assertFalse(stored.active)
        self.assertNotIn(category.id, [item.id for item in reporting.active_categories()])
        with self.assertRaisesRegex(ValueError, "Select an active category"):
            reporting.submit(alice, category.id, "Another bench", "Damage", "Courtyard")
        self.assertIn(IssueSummary(issue, alice.username, renamed), reporting.list(alice))

        service.set_active(admin, category.id, True)
        self.assertIn(category.id, [item.id for item in reporting.active_categories()])
