from dataclasses import dataclass, field

import psycopg

from campusfix.model.domain.user import Role, User
from campusfix.model.persistence.crud_dao import CrudDao
from campusfix.model.persistence.database_connection import DatabaseConnection


_ACCOUNT_COLUMNS = "id, username, role, password_salt, password_hash"


@dataclass(frozen=True)
class Account:
    user: User
    salt: bytes = field(repr=False)
    password_hash: bytes = field(repr=False)


class UserDao(CrudDao[Account]):
    def __init__(self, database: DatabaseConnection) -> None:
        self._database = database

    def create(self, account: Account) -> Account:
        query = f"""
            INSERT INTO users (username, role, password_salt, password_hash)
            VALUES (%s, %s, %s, %s)
            RETURNING {_ACCOUNT_COLUMNS}
        """
        created = self._query_account(query, self._account_values(account))
        if created is None:
            raise psycopg.DatabaseError("Insert returned no account.")
        return created

    def read(self, entity_id: int) -> Account | None:
        query = f"SELECT {_ACCOUNT_COLUMNS} FROM users WHERE id = %s"
        return self._query_account(query, (entity_id,))

    def update(self, account: Account) -> bool:
        query = """
            UPDATE users
            SET username = %s, role = %s, password_salt = %s, password_hash = %s
            WHERE id = %s
        """
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, (*self._account_values(account), account.user.id))
            return cursor.rowcount == 1

    def delete(self, entity_id: int) -> bool:
        with self._database.get_connection().cursor() as cursor:
            cursor.execute("DELETE FROM users WHERE id = %s", (entity_id,))
            return cursor.rowcount == 1

    def find_by_username(self, username: str) -> Account | None:
        query = f"SELECT {_ACCOUNT_COLUMNS} FROM users WHERE username = %s"
        return self._query_account(query, (username,))

    def _query_account(
        self, query: str, parameters: tuple[int | str | bytes, ...]
    ) -> Account | None:
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, parameters)
            row = cursor.fetchone()
        if row is None:
            return None
        user_id, username, role, salt, password_hash = row
        return Account(User(user_id, username, Role(role)), salt, password_hash)

    @staticmethod
    def _account_values(account: Account) -> tuple[str, str, bytes, bytes]:
        return (
            account.user.username,
            account.user.role.value,
            account.salt,
            account.password_hash,
        )
