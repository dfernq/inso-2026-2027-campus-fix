from os import getenv
from pathlib import Path

from dotenv import load_dotenv

from campusfix.controller.campusfix_controller import CampusFixController
from campusfix.model.auth.authentication_service import AuthenticationService
from campusfix.model.persistence.category_dao import CategoryDao
from campusfix.model.persistence.database_connection import DatabaseConnection
from campusfix.model.persistence.issue_dao import IssueDao
from campusfix.model.persistence.user_dao import UserDao
from campusfix.model.reports.category_service import CategoryService
from campusfix.model.reports.issue_service import IssueService


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

    users = UserDao(database)
    authentication = AuthenticationService(users)
    category_dao = CategoryDao(database)
    reports = IssueService(IssueDao(database), category_dao)
    categories = CategoryService(category_dao)
    CampusFixController(database, authentication, reports, categories).start()


if __name__ == "__main__":
    main()
