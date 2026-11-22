from campusfix import desktop_database
from campusfix.controller.campusfix_controller import CampusFixController
from campusfix.model.auth.authentication_service import AuthenticationService
from campusfix.model.persistence.category_dao import CategoryDao
from campusfix.model.persistence.issue_dao import IssueDao
from campusfix.model.persistence.user_dao import UserDao
from campusfix.model.reports.category_service import CategoryService
from campusfix.model.reports.issue_service import IssueService


def main() -> None:
    try:
        database = desktop_database.get_instance()
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
