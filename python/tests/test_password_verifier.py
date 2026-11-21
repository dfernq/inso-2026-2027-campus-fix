import unittest

from campusfix.model.auth.password_verifier import matches


class PasswordVerifierTest(unittest.TestCase):
    def test_verifies_the_shared_password_format(self) -> None:
        salt = bytes.fromhex("00112233445566778899aabbccddeeff")
        password_hash = bytes.fromhex(
            "339add849f6f8177bd35d90372c1d7e143c69a9109f4c3c7964175349f488d53"
        )

        self.assertTrue(matches("café-demo", salt, password_hash))
        self.assertFalse(matches("wrong-password", salt, password_hash))
