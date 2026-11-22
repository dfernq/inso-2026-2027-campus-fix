import psycopg

from campusfix.model.domain.category import Category
from campusfix.model.persistence.crud_dao import CrudDao
from campusfix.model.persistence.database_connection import DatabaseConnection


class CategoryDao(CrudDao[Category]):
    def __init__(self, database: DatabaseConnection) -> None:
        self._database = database

    def create(self, category: Category) -> Category:
        query = """
            INSERT INTO categories (name, active) VALUES (%s, %s)
            RETURNING id, name, active
        """
        created = self._query_category(query, (category.name, category.active))
        if created is None:
            raise psycopg.DatabaseError("Insert returned no category.")
        return created

    def read(self, entity_id: int) -> Category | None:
        query = "SELECT id, name, active FROM categories WHERE id = %s"
        return self._query_category(query, (entity_id,))

    def update(self, category: Category) -> bool:
        query = "UPDATE categories SET name = %s, active = %s WHERE id = %s"
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, (category.name, category.active, category.id))
            return cursor.rowcount == 1

    def rename(self, category_id: int, name: str) -> bool:
        query = "UPDATE categories SET name = %s WHERE id = %s"
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, (name, category_id))
            return cursor.rowcount == 1

    def set_active(self, category_id: int, active: bool) -> bool:
        query = "UPDATE categories SET active = %s WHERE id = %s"
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, (active, category_id))
            return cursor.rowcount == 1

    def delete(self, entity_id: int) -> bool:
        with self._database.get_connection().cursor() as cursor:
            cursor.execute("DELETE FROM categories WHERE id = %s", (entity_id,))
            return cursor.rowcount == 1

    def find_all(self) -> list[Category]:
        return self._query_categories("SELECT id, name, active FROM categories ORDER BY name")

    def find_active(self) -> list[Category]:
        return self._query_categories(
            "SELECT id, name, active FROM categories WHERE active ORDER BY name"
        )

    def _query_categories(self, query: str) -> list[Category]:
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query)
            return [Category(*row) for row in cursor]

    def _query_category(
        self, query: str, parameters: tuple[int | str | bool, ...]
    ) -> Category | None:
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, parameters)
            row = cursor.fetchone()
        return Category(*row) if row is not None else None
