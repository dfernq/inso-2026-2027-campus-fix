from os import getenv
from pathlib import Path
from unittest import TestCase

from dotenv import load_dotenv
from psycopg.conninfo import conninfo_to_dict

from campusfix.model.persistence.database_connection import DatabaseConnection


def open_test_database(test: TestCase) -> DatabaseConnection:
    env_file = Path(__file__).resolve().parents[3] / ".env"
    load_dotenv(env_file, interpolate=False)
    url = getenv("CAMPUSFIX_TEST_DB_URL", "")
    connection_info = conninfo_to_dict(url.removeprefix("jdbc:"))
    test.assertTrue(
        connection_info.get("dbname", "").endswith("_test"),
        "Set CAMPUSFIX_TEST_DB_URL to a dedicated database ending in _test.",
    )

    database = DatabaseConnection(
        url, getenv("CAMPUSFIX_DB_USER"), getenv("CAMPUSFIX_DB_PASSWORD")
    )
    test.addCleanup(database.close)
    test.assertTrue(
        database.get_connection().info.dbname.endswith("_test"),
        "Integration tests require a connection to a dedicated test database.",
    )
    return database
