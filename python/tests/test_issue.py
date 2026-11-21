import unittest

from campusfix.model.domain.issue import Issue, Status


class IssueTest(unittest.TestCase):
    def test_opens_a_report_and_trims_its_fields(self) -> None:
        issue = Issue.open(7, 2, "  Leaking tap ", " Water on the floor. ", " Library ")

        self.assertIs(Status.OPEN, issue.status)
        self.assertEqual("Leaking tap", issue.title)
        self.assertEqual("Water on the floor.", issue.description)
        self.assertEqual("Library", issue.location)

    def test_rejects_incomplete_or_oversized_reports(self) -> None:
        cases = [
            (0, "Leaking tap", "Water on the floor.", "Library", "Select a category."),
            (2, " ", "Water on the floor.", "Library", "Title is required."),
            (2, "Leaking tap", "Water on the floor.", "", "Location is required."),
            (2, "Leaking tap", "x" * 2001, "Library", "at most 2000 characters"),
        ]
        for category, title, description, location, error in cases:
            with self.subTest(error=error):
                with self.assertRaisesRegex(ValueError, error):
                    Issue.open(7, category, title, description, location)
