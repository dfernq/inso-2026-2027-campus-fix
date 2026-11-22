from dataclasses import dataclass
from enum import Enum


class Status(Enum):
    OPEN = "OPEN"
    IN_PROGRESS = "IN_PROGRESS"
    RESOLVED = "RESOLVED"
    CANCELLED = "CANCELLED"


@dataclass(frozen=True)
class Issue:
    id: int
    reporter_id: int
    category_id: int
    title: str
    description: str
    location: str
    status: Status

    @staticmethod
    def open(
        reporter_id: int,
        category_id: int,
        title: str | None,
        description: str | None,
        location: str | None,
        *,
        issue_id: int = 0,
    ) -> "Issue":
        if category_id <= 0:
            raise ValueError("Select a category.")
        return Issue(
            issue_id,
            reporter_id,
            category_id,
            _require_text(title, "Title", 120),
            _require_text(description, "Description", 2000),
            _require_text(location, "Location", 120),
            Status.OPEN,
        )


@dataclass(frozen=True)
class IssueSummary:
    issue: Issue
    reporter_name: str
    category_name: str


def _require_text(text: str | None, field: str, max_length: int) -> str:
    value = text.strip() if text is not None else ""
    if not value:
        raise ValueError(f"{field} is required.")
    if len(value) > max_length:
        raise ValueError(f"{field} must be at most {max_length} characters.")
    return value
