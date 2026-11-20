import psycopg


class DatabaseConnection:
    def __init__(
        self, url: str | None, username: str | None, password: str | None
    ) -> None:
        self._url = self._require_setting(url, "Database URL").removeprefix("jdbc:")
        self._username = self._require_setting(username, "Database username")
        self._password = self._require_setting(password, "Database password")
        self._connection: psycopg.Connection | None = None

    @staticmethod
    def _require_setting(value: str | None, name: str) -> str:
        if value is None or not value.strip():
            raise ValueError(f"{name} must be provided.")
        return value

    # shortcut: one worker owns one autocommit connection. Add transactions and
    # recovery when workflows need multiple operations.
    def get_connection(self) -> psycopg.Connection:
        if self._connection is None:
            self._connection = psycopg.connect(
                self._url,
                user=self._username,
                password=self._password,
                autocommit=True,
                connect_timeout=5,
            )
        return self._connection

    def check_connection(self) -> bool:
        with self.get_connection().cursor() as cursor:
            cursor.execute("SELECT 1")
            return cursor.fetchone() == (1,)

    def close(self) -> None:
        if self._connection is not None:
            self._connection.close()
            self._connection = None
