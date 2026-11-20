import unittest
from os import getenv
from pathlib import Path

from dotenv import load_dotenv
from psycopg.conninfo import conninfo_to_dict

from campusfix.model.persistence.database_connection import DatabaseConnection


class DatabaseConnectionIntegrationTest(unittest.TestCase):
    def test_checks_postgres_reuses_the_connection_and_closes_it(self) -> None:
        env_file = Path(__file__).resolve().parents[3] / ".env"
        load_dotenv(env_file, interpolate=False)
        url = getenv("CAMPUSFIX_TEST_DB_URL", "")
        connection_info = conninfo_to_dict(url.removeprefix("jdbc:"))
        self.assertTrue(
            connection_info.get("dbname", "").endswith("_test"),
            "Set CAMPUSFIX_TEST_DB_URL to a dedicated database ending in _test.",
        )

        database = DatabaseConnection(
            url, getenv("CAMPUSFIX_DB_USER"), getenv("CAMPUSFIX_DB_PASSWORD")
        )
        self.addCleanup(database.close)
        connection = database.get_connection()

        self.assertTrue(connection.autocommit)
        self.assertTrue(database.check_connection())
        self.assertIs(connection, database.get_connection())

        database.close()
        self.assertTrue(connection.closed)
