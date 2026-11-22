from campusfix.model.domain.category import Category
from campusfix.model.domain.issue import Issue, IssueSummary
from campusfix.model.domain.user import Role, User
from campusfix.model.persistence.category_dao import CategoryDao
from campusfix.model.persistence.issue_dao import IssueDao


class IssueService:
    def __init__(self, issues: IssueDao, categories: CategoryDao) -> None:
        self._issues = issues
        self._categories = categories

    def active_categories(self) -> list[Category]:
        return self._categories.find_active()

    def submit(
        self,
        reporter: User,
        category_id: int,
        title: str,
        description: str,
        location: str,
    ) -> Issue:
        self._require_regular_user(reporter)
        issue = Issue.open(reporter.id, category_id, title, description, location)
        return self._issues.create(issue)

    def edit(
        self,
        reporter: User,
        issue_id: int,
        category_id: int,
        title: str,
        description: str,
        location: str,
    ) -> None:
        self._require_regular_user(reporter)
        issue = Issue.open(
            reporter.id, category_id, title, description, location, issue_id=issue_id
        )
        if not self._issues.update_open(issue):
            raise ValueError("Edit an open report you submitted and select an active category.")

    def cancel(self, reporter: User, issue_id: int) -> None:
        self._require_regular_user(reporter)
        if not self._issues.cancel_open(issue_id, reporter.id):
            raise ValueError("Only your open reports can be cancelled.")

    def start_work(self, administrator: User, issue_id: int) -> None:
        self._require_administrator(administrator)
        if not self._issues.start_work(issue_id):
            raise ValueError("Only open reports can be started.")

    def resolve(self, administrator: User, issue_id: int, resolution_note: str) -> None:
        self._require_administrator(administrator)
        note = Issue.require_resolution_note(resolution_note)
        if not self._issues.resolve(issue_id, note):
            raise ValueError("Only reports in progress can be resolved.")

    def list(self, viewer: User) -> list[IssueSummary]:
        if viewer.role is Role.ADMIN:
            return self._issues.find_all()
        return self._issues.find_by_reporter(viewer.id)

    @staticmethod
    def _require_administrator(user: User) -> None:
        if user.role is not Role.ADMIN:
            raise ValueError("Only administrators can start work or resolve reports.")

    @staticmethod
    def _require_regular_user(user: User) -> None:
        if user.role is not Role.REGULAR:
            raise ValueError("Only regular users can submit, edit, or cancel reports.")
