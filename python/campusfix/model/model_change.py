from dataclasses import dataclass
from enum import Enum, auto

from campusfix.model.domain.user import User


class ReportChangeKind(Enum):
    SUBMITTED = auto()
    EDITED = auto()
    CANCELLED = auto()
    WORK_STARTED = auto()
    RESOLVED = auto()


@dataclass(frozen=True)
class ReportChanged:
    actor: User
    kind: ReportChangeKind
    report_id: int


class CategoryChangeKind(Enum):
    ADDED = auto()
    RENAMED = auto()
    ACTIVATED = auto()
    DEACTIVATED = auto()


@dataclass(frozen=True)
class CategoryChanged:
    actor: User
    kind: CategoryChangeKind


ModelChange = ReportChanged | CategoryChanged
