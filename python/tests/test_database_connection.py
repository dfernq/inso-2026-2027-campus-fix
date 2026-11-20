import unittest

import psycopg

from campusfix.model.persistence.database_connection import DatabaseConnection


class DatabaseConnectionTest(unittest.TestCase):
    def test_requires_connection_settings(self) -> None:
        with self.assertRaises(ValueError):
            DatabaseConnection("", "user", "password")
        with self.assertRaises(ValueError):
            DatabaseConnection("jdbc:postgresql://localhost/campusfix", None, "password")
        with self.assertRaises(ValueError):
            DatabaseConnection("jdbc:postgresql://localhost/campusfix", "user", " ")

    def test_reports_connection_failure(self) -> None:
        database = DatabaseConnection("jdbc:unsupported:campusfix", "user", "password")
        self.addCleanup(database.close)

        with self.assertRaises(psycopg.ProgrammingError):
            database.check_connection()
