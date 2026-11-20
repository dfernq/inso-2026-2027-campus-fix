from os import getenv
from pathlib import Path

from dotenv import load_dotenv

from campusfix.controller.connection_controller import ConnectionController
from campusfix.model.persistence.database_connection import DatabaseConnection


def main() -> None:
    env_file = Path(__file__).resolve().parents[2] / ".env"
    if not env_file.is_file():
        raise SystemExit("Create .env in the repository root before starting CampusFix.")
    load_dotenv(env_file, interpolate=False)

    try:
        database = DatabaseConnection(
            getenv("CAMPUSFIX_DB_URL"),
            getenv("CAMPUSFIX_DB_USER"),
            getenv("CAMPUSFIX_DB_PASSWORD"),
        )
    except ValueError as error:
        raise SystemExit(str(error)) from None

    ConnectionController(database).start()


if __name__ == "__main__":
    main()
