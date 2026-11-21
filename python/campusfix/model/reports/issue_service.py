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
        if reporter.role is not Role.REGULAR:
            raise ValueError("Only regular users can submit reports.")
        issue = Issue.open(reporter.id, category_id, title, description, location)
        return self._issues.create(issue)

    def list(self, viewer: User) -> list[IssueSummary]:
        if viewer.role is Role.ADMIN:
            return self._issues.find_all()
        return self._issues.find_by_reporter(viewer.id)
