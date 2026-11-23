from abc import ABC, abstractmethod
from dataclasses import dataclass

from campusfix.model.domain.issue import IssueSummary, Status
from campusfix.model.domain.user import Role
from campusfix.view.report_table import ReportRow


@dataclass(frozen=True)
class _ReportActions:
    can_manage: bool = False
    can_start_work: bool = False
    can_resolve: bool = False


class ReportPresentation(ABC):
    account_title: str
    shows_submission_form: bool

    def to_row(self, report: IssueSummary) -> ReportRow:
        issue = report.issue
        actions = self._actions_for(issue.status)
        return ReportRow(
            issue.id,
            issue.title,
            issue.category_id,
            report.category_name,
            issue.location,
            report.reporter_name,
            issue.status.value,
            issue.description,
            issue.resolution_note,
            actions.can_manage,
            actions.can_start_work,
            actions.can_resolve,
        )

    @abstractmethod
    def _actions_for(self, status: Status) -> _ReportActions:
        pass


class _RegularPresentation(ReportPresentation):
    account_title = "Regular account"
    shows_submission_form = True

    def _actions_for(self, status: Status) -> _ReportActions:
        return _ReportActions(can_manage=status is Status.OPEN)


class _AdministratorPresentation(ReportPresentation):
    account_title = "Administrator account"
    shows_submission_form = False

    def _actions_for(self, status: Status) -> _ReportActions:
        return _ReportActions(
            can_start_work=status is Status.OPEN,
            can_resolve=status is Status.IN_PROGRESS,
        )


_PRESENTATIONS: dict[Role, ReportPresentation] = {
    Role.REGULAR: _RegularPresentation(),
    Role.ADMIN: _AdministratorPresentation(),
}


def presentation_for(role: Role) -> ReportPresentation:
    return _PRESENTATIONS[role]
