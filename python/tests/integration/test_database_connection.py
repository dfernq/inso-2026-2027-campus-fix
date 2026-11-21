import unittest

from database_fixture import open_test_database


class DatabaseConnectionIntegrationTest(unittest.TestCase):
    def test_checks_postgres_reuses_the_connection_and_closes_it(self) -> None:
        database = open_test_database(self)
        connection = database.get_connection()

        self.assertTrue(connection.autocommit)
        self.assertTrue(database.check_connection())
        self.assertIs(connection, database.get_connection())

        database.close()
        self.assertTrue(connection.closed)
