from abc import ABC, abstractmethod
from typing import Generic, TypeVar, final

from psycopg.rows import DictRow, dict_row

from campusfix.model.persistence.database_connection import DatabaseConnection


T = TypeVar("T")


class RowMapper(ABC, Generic[T]):
    @final
    def query_one(
        self, database: DatabaseConnection, query: str, parameters: tuple[object, ...] = ()
    ) -> T | None:
        with database.get_connection().cursor(row_factory=dict_row) as cursor:
            cursor.execute(query, parameters)
            row = cursor.fetchone()
            return self.map_row(row) if row is not None else None

    @final
    def query_all(
        self, database: DatabaseConnection, query: str, parameters: tuple[object, ...] = ()
    ) -> list[T]:
        with database.get_connection().cursor(row_factory=dict_row) as cursor:
            cursor.execute(query, parameters)
            return [self.map_row(row) for row in cursor]

    @abstractmethod
    def map_row(self, row: DictRow) -> T:
        pass
