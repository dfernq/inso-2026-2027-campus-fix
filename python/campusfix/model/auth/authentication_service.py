from campusfix.model.auth import password_verifier
from campusfix.model.domain.user import User
from campusfix.model.persistence.user_dao import UserDao


class AuthenticationService:
    def __init__(self, users: UserDao) -> None:
        self._users = users

    def sign_in(self, username: str, password: str) -> User | None:
        username = username.strip()
        if not username or not password:
            return None

        account = self._users.find_by_username(username)
        if account is None:
            return None
        if password_verifier.matches(password, account.salt, account.password_hash):
            return account.user
        return None
