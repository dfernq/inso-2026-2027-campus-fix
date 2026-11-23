from collections.abc import Callable
from dataclasses import dataclass
from functools import partial
from typing import Protocol

from campusfix.model.domain.user import User
from campusfix.model.reports.issue_service import IssueService


ReportCommand = Callable[[], object]


@dataclass(frozen=True)
class ReportSave:
    command: ReportCommand
    progress_message: str
    success_message: str


class ReportFormState(Protocol):
    def prepare_save(
        self, reports: IssueService, user: User, category_id: int,
        title: str, description: str, location: str,
    ) -> ReportSave: ...

    def edits(self, report_id: int) -> bool: ...


class NewReport:
    def prepare_save(
        self, reports: IssueService, user: User, category_id: int,
        title: str, description: str, location: str,
    ) -> ReportSave:
        return ReportSave(
            partial(reports.submit, user, category_id, title, description, location),
            "Submitting report...",
            "Report submitted.",
        )

    def edits(self, report_id: int) -> bool:
        return False


@dataclass(frozen=True)
class EditingReport:
    report_id: int

    def prepare_save(
        self, reports: IssueService, user: User, category_id: int,
        title: str, description: str, location: str,
    ) -> ReportSave:
        return ReportSave(
            partial(reports.edit, user, self.report_id, category_id, title, description, location),
            "Saving report...",
            "Report updated.",
        )

    def edits(self, report_id: int) -> bool:
        return self.report_id == report_id
