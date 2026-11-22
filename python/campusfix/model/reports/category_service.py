from psycopg.errors import UniqueViolation

from campusfix.model.domain.category import Category
from campusfix.model.domain.user import Role, User
from campusfix.model.persistence.category_dao import CategoryDao


class CategoryService:
    def __init__(self, categories: CategoryDao) -> None:
        self._categories = categories

    def list(self, administrator: User) -> list[Category]:
        self._require_administrator(administrator)
        return self._categories.find_all()

    def create(self, administrator: User, name: str) -> None:
        self._require_administrator(administrator)
        try:
            self._categories.create(Category(0, self._require_name(name), True))
        except UniqueViolation as error:
            raise ValueError("A category with this name already exists.") from error

    def rename(self, administrator: User, category_id: int, name: str) -> None:
        self._require_administrator(administrator)
        try:
            self._require_updated(self._categories.rename(category_id, self._require_name(name)))
        except UniqueViolation as error:
            raise ValueError("A category with this name already exists.") from error

    def set_active(self, administrator: User, category_id: int, active: bool) -> None:
        self._require_administrator(administrator)
        self._require_updated(self._categories.set_active(category_id, active))

    @staticmethod
    def _require_administrator(user: User) -> None:
        if user.role is not Role.ADMIN:
            raise ValueError("Only administrators can manage categories.")

    @staticmethod
    def _require_name(name: str) -> str:
        trimmed_name = name.strip()
        if not trimmed_name:
            raise ValueError("Category name is required.")
        return trimmed_name

    @staticmethod
    def _require_updated(updated: bool) -> None:
        if not updated:
            raise ValueError("The category no longer exists. Refresh the list.")
