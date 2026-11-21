import unittest

from campusfix.model.auth.authentication_service import AuthenticationService
from campusfix.model.domain.user import Role
from campusfix.model.persistence.user_dao import UserDao
from database_fixture import open_test_database


class AuthenticationServiceIntegrationTest(unittest.TestCase):
    def test_signs_in_seeded_accounts_and_rejects_invalid_credentials(self) -> None:
        authentication = AuthenticationService(UserDao(open_test_database(self)))

        cases = [
            (" alice ", "alice-demo", Role.REGULAR),
            ("bob", "bob-demo", Role.REGULAR),
            ("admin", "admin-demo", Role.ADMIN),
        ]
        for username, password, role in cases:
            with self.subTest(username=username):
                user = authentication.sign_in(username, password)
                self.assertIsNotNone(user)
                self.assertEqual(username.strip(), user.username)
                self.assertEqual(role, user.role)

        self.assertIsNone(authentication.sign_in("admin", "wrong-password"))
        self.assertIsNone(authentication.sign_in("unknown", "wrong-password"))
        self.assertIsNone(authentication.sign_in(" ", "alice-demo"))
        self.assertIsNone(authentication.sign_in("alice", ""))
