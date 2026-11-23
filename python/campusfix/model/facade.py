import logging
from collections.abc import Callable
from threading import Lock

from campusfix.model.auth.authentication_service import AuthenticationService
from campusfix.model.domain.category import Category
from campusfix.model.domain.issue import Issue, IssueSummary
from campusfix.model.domain.user import User
from campusfix.model.model_change import (
    CategoryChanged,
    CategoryChangeKind,
    ModelChange,
    ReportChanged,
    ReportChangeKind,
)
from campusfix.model.persistence.category_dao import CategoryDao
from campusfix.model.persistence.database_connection import DatabaseConnection
from campusfix.model.persistence.issue_dao import IssueDao
from campusfix.model.persistence.user_dao import UserDao
from campusfix.model.reports.category_service import CategoryService
from campusfix.model.reports.issue_service import IssueService


logger = logging.getLogger(__name__)
ChangeListener = Callable[[ModelChange], None]


class CampusFix:
    def __init__(self, database: DatabaseConnection) -> None:
        self._database = database
        category_dao = CategoryDao(database)
        self._authentication = AuthenticationService(UserDao(database))
        self._reports = IssueService(IssueDao(database), category_dao)
        self._categories = CategoryService(category_dao)
        self._change_listeners: list[ChangeListener] = []
        self._listener_lock = Lock()

    def add_change_listener(self, listener: ChangeListener) -> None:
        with self._listener_lock:
            self._change_listeners.append(listener)

    def remove_change_listener(self, listener: ChangeListener) -> None:
        with self._listener_lock:
            if listener in self._change_listeners:
                self._change_listeners.remove(listener)

    def _notify_change(self, change: ModelChange) -> None:
        with self._listener_lock:
            listeners = self._change_listeners.copy()
        for listener in listeners:
            try:
                listener(change)
            except Exception:
                logger.exception("Model change listener failed.")

    def check_connection(self) -> bool:
        return self._database.check_connection()

    def sign_in(self, username: str, password: str) -> User | None:
        return self._authentication.sign_in(username, password)

    def list_reports(self, viewer: User) -> list[IssueSummary]:
        return self._reports.list(viewer)

    def active_categories(self) -> list[Category]:
        return self._reports.active_categories()

    def submit_report(
        self, reporter: User, category_id: int, title: str, description: str, location: str
    ) -> Issue:
        issue = self._reports.submit(reporter, category_id, title, description, location)
        self._notify_change(ReportChanged(reporter, ReportChangeKind.SUBMITTED, issue.id))
        return issue

    def edit_report(
        self, reporter: User, issue_id: int, category_id: int,
        title: str, description: str, location: str,
    ) -> None:
        self._reports.edit(reporter, issue_id, category_id, title, description, location)
        self._notify_change(ReportChanged(reporter, ReportChangeKind.EDITED, issue_id))

    def cancel_report(self, reporter: User, issue_id: int) -> None:
        self._reports.cancel(reporter, issue_id)
        self._notify_change(ReportChanged(reporter, ReportChangeKind.CANCELLED, issue_id))

    def start_work(self, administrator: User, issue_id: int) -> None:
        self._reports.start_work(administrator, issue_id)
        self._notify_change(ReportChanged(administrator, ReportChangeKind.WORK_STARTED, issue_id))

    def resolve_report(self, administrator: User, issue_id: int, note: str) -> None:
        self._reports.resolve(administrator, issue_id, note)
        self._notify_change(ReportChanged(administrator, ReportChangeKind.RESOLVED, issue_id))

    def list_categories(self, administrator: User) -> list[Category]:
        return self._categories.list(administrator)

    def create_category(self, administrator: User, name: str) -> None:
        self._categories.create(administrator, name)
        self._notify_change(CategoryChanged(administrator, CategoryChangeKind.ADDED))

    def rename_category(self, administrator: User, category_id: int, name: str) -> None:
        self._categories.rename(administrator, category_id, name)
        self._notify_change(CategoryChanged(administrator, CategoryChangeKind.RENAMED))

    def set_category_active(self, administrator: User, category_id: int, active: bool) -> None:
        self._categories.set_active(administrator, category_id, active)
        kind = CategoryChangeKind.ACTIVATED if active else CategoryChangeKind.DEACTIVATED
        self._notify_change(CategoryChanged(administrator, kind))

    def close(self) -> None:
        self._database.close()
