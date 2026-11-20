import logging
from concurrent.futures import Future, ThreadPoolExecutor
from tkinter import Tk

import psycopg

from campusfix.model.persistence.database_connection import DatabaseConnection
from campusfix.view.connection_view import ConnectionView

logger = logging.getLogger(__name__)


class ConnectionController:
    def __init__(self, database: DatabaseConnection) -> None:
        self._database = database
        self._window = Tk()
        self._view = ConnectionView(self._window)
        self._worker = ThreadPoolExecutor(max_workers=1)
        self._view.on_check(self._check_connection)
        self._window.protocol("WM_DELETE_WINDOW", self._window.destroy)

    def start(self) -> None:
        with self._worker:
            try:
                self._window.mainloop()
            finally:
                self._worker.submit(self._database.close).result()

    def _check_connection(self) -> None:
        self._view.show_status("Checking PostgreSQL...", busy=True)
        result = self._worker.submit(self._database.check_connection)
        self._show_result(result)

    def _show_result(self, result: Future[bool]) -> None:
        if not result.done():
            self._window.after(50, self._show_result, result)
            return

        try:
            if result.result():
                message = "Connected to PostgreSQL."
            else:
                message = "PostgreSQL returned an unexpected result."
        except psycopg.Error:
            logger.exception("Database connection check failed.")
            message = "Connection failed. Check your settings and PostgreSQL."

        self._view.show_status(message, busy=False)
