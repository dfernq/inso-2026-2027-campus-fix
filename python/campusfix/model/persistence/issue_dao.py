from psycopg.rows import DictRow, dict_row

from campusfix.model.domain.issue import Issue, IssueSummary, Status
from campusfix.model.persistence.crud_dao import CrudDao
from campusfix.model.persistence.database_connection import DatabaseConnection


_REPORT_QUERY = """
    SELECT i.id, i.reporter_id, i.category_id, i.title, i.description,
           i.location, i.status, i.resolution_note,
           u.username AS reporter_name, c.name AS category_name
    FROM issues i
    JOIN users u ON u.id = i.reporter_id
    JOIN categories c ON c.id = i.category_id
"""


class IssueDao(CrudDao[Issue]):
    def __init__(self, database: DatabaseConnection) -> None:
        self._database = database

    def create(self, issue: Issue) -> Issue:
        query = """
            INSERT INTO issues
                (reporter_id, category_id, title, description, location, status, resolution_note)
            SELECT %s, id, %s, %s, %s, %s, %s FROM categories WHERE id = %s AND active
            RETURNING id, reporter_id, category_id, title, description, location, status, resolution_note
        """
        parameters = (
            issue.reporter_id,
            issue.title,
            issue.description,
            issue.location,
            issue.status.value,
            issue.resolution_note,
            issue.category_id,
        )
        created = self._query_issue(query, parameters)
        if created is None:
            raise ValueError("Select an active category.")
        return created

    def read(self, entity_id: int) -> Issue | None:
        query = """
            SELECT id, reporter_id, category_id, title, description, location, status, resolution_note
            FROM issues WHERE id = %s
        """
        return self._query_issue(query, (entity_id,))

    def update(self, issue: Issue) -> bool:
        query = """
            UPDATE issues SET category_id = %s, title = %s, description = %s,
                location = %s, status = %s, resolution_note = %s
            WHERE id = %s
        """
        parameters = (
            issue.category_id,
            issue.title,
            issue.description,
            issue.location,
            issue.status.value,
            issue.resolution_note,
            issue.id,
        )
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, parameters)
            return cursor.rowcount == 1

    def delete(self, entity_id: int) -> bool:
        with self._database.get_connection().cursor() as cursor:
            cursor.execute("DELETE FROM issues WHERE id = %s", (entity_id,))
            return cursor.rowcount == 1

    def update_open(self, issue: Issue) -> bool:
        query = """
            UPDATE issues
            SET category_id = category.id, title = %s, description = %s, location = %s
            FROM categories category
            WHERE issues.id = %s AND issues.reporter_id = %s
              AND issues.status = 'OPEN'
              AND category.id = %s AND category.active
        """
        parameters = (
            issue.title,
            issue.description,
            issue.location,
            issue.id,
            issue.reporter_id,
            issue.category_id,
        )
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, parameters)
            return cursor.rowcount == 1

    def cancel_open(self, issue_id: int, reporter_id: int) -> bool:
        query = """
            UPDATE issues SET status = 'CANCELLED'
            WHERE id = %s AND reporter_id = %s AND status = 'OPEN'
        """
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, (issue_id, reporter_id))
            return cursor.rowcount == 1

    def start_work(self, issue_id: int) -> bool:
        query = """
            UPDATE issues SET status = 'IN_PROGRESS' WHERE id = %s AND status = 'OPEN'
        """
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, (issue_id,))
            return cursor.rowcount == 1

    def resolve(self, issue_id: int, resolution_note: str) -> bool:
        query = """
            UPDATE issues SET status = 'RESOLVED', resolution_note = %s
            WHERE id = %s AND status = 'IN_PROGRESS'
        """
        with self._database.get_connection().cursor() as cursor:
            cursor.execute(query, (resolution_note, issue_id))
            return cursor.rowcount == 1

    def find_all(self) -> list[IssueSummary]:
        return self._query_reports(f"{_REPORT_QUERY} ORDER BY i.id DESC", ())

    def find_by_reporter(self, reporter_id: int) -> list[IssueSummary]:
        query = f"{_REPORT_QUERY} WHERE i.reporter_id = %s ORDER BY i.id DESC"
        return self._query_reports(query, (reporter_id,))

    def _query_issue(
        self, query: str, parameters: tuple[int | str | None, ...]
    ) -> Issue | None:
        with self._database.get_connection().cursor(row_factory=dict_row) as cursor:
            cursor.execute(query, parameters)
            row = cursor.fetchone()
        return self._map_issue(row) if row is not None else None

    def _query_reports(
        self, query: str, parameters: tuple[int, ...]
    ) -> list[IssueSummary]:
        with self._database.get_connection().cursor(row_factory=dict_row) as cursor:
            cursor.execute(query, parameters)
            return [
                IssueSummary(
                    self._map_issue(row), row["reporter_name"], row["category_name"]
                )
                for row in cursor
            ]

    @staticmethod
    def _map_issue(row: DictRow) -> Issue:
        return Issue(
            row["id"],
            row["reporter_id"],
            row["category_id"],
            row["title"],
            row["description"],
            row["location"],
            Status(row["status"]),
            row["resolution_note"],
        )
