import unittest
from uuid import uuid4

from campusfix.model.domain.user import Role, User
from campusfix.model.persistence.user_dao import Account, UserDao
from database_fixture import open_test_database


class UserDaoIntegrationTest(unittest.TestCase):
    def test_creates_reads_updates_and_deletes_an_account(self) -> None:
        accounts = UserDao(open_test_database(self))
        username = f"crud-{uuid4()}"
        salt = bytes(16)
        password_hash = bytes(32)
        regular = Account(User(0, username, Role.REGULAR), salt, password_hash)
        created = accounts.create(regular)
        user_id = created.user.id
        self.addCleanup(accounts.delete, user_id)

        self.assertGreater(user_id, 0)
        saved = Account(User(user_id, username, Role.REGULAR), salt, password_hash)
        self.assertEqual(saved, accounts.read(user_id))

        admin = User(user_id, f"{username}-admin", Role.ADMIN)
        updated = Account(admin, salt, bytes([1]) * 32)
        self.assertTrue(accounts.update(updated))
        self.assertEqual(updated, accounts.read(user_id))

        self.assertTrue(accounts.delete(user_id))
        self.assertIsNone(accounts.read(user_id))
        self.assertFalse(accounts.update(updated))
        self.assertFalse(accounts.delete(user_id))
