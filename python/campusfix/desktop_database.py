from os import getenv
from pathlib import Path
from threading import Lock

from dotenv import load_dotenv

from campusfix.model.persistence.database_connection import DatabaseConnection


_instance: DatabaseConnection | None = None
_instance_lock = Lock()


def get_instance() -> DatabaseConnection:
    global _instance
    with _instance_lock:
        if _instance is None:
            _instance = _create_connection()
        return _instance


def _create_connection() -> DatabaseConnection:
    env_file = Path(__file__).resolve().parents[2] / ".env"
    if not env_file.is_file():
        raise SystemExit("Create .env in the repository root before starting CampusFix.")
    load_dotenv(env_file, interpolate=False)

    return DatabaseConnection(
        getenv("CAMPUSFIX_DB_URL"),
        getenv("CAMPUSFIX_DB_USER"),
        getenv("CAMPUSFIX_DB_PASSWORD"),
    )
