from dataclasses import dataclass
from enum import Enum


class Role(Enum):
    REGULAR = "REGULAR"
    ADMIN = "ADMIN"


@dataclass(frozen=True)
class User:
    id: int
    username: str
    role: Role
