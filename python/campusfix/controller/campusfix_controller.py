import logging
from collections.abc import Callable
from concurrent.futures import Future, ThreadPoolExecutor
from tkinter import Tk
from typing import TypeVar

import psycopg

from campusfix.model.auth.authentication_service import AuthenticationService
from campusfix.model.domain.user import Role, User
from campusfix.model.persistence.database_connection import DatabaseConnection
from campusfix.view.campusfix_view import CampusFixView


logger = logging.getLogger(__name__)
T = TypeVar("T")


class CampusFixController:
    def __init__(
        self, database: DatabaseConnection, authentication: AuthenticationService
    ) -> None:
        self._database = database
        self._authentication = authentication
        self._window = Tk()
        self._view = CampusFixView(self._window)
        self._worker = ThreadPoolExecutor(max_workers=1)
        self._signed_in_user: User | None = None
        self._pending_callback: str | None = None
        self._view.on_sign_in(self._sign_in)
        self._view.on_check(self._check_connection)
        self._view.on_sign_out(self._sign_out)
        self._window.protocol("WM_DELETE_WINDOW", self._close_window)

    def start(self) -> None:
        self._view.show_sign_in()
        with self._worker:
            try:
                self._window.mainloop()
            finally:
                self._worker.submit(self._database.close).result()

    def _sign_in(self) -> None:
        username = self._view.username()
        password = self._view.take_password()
        self._view.show_status("Signing in...", busy=True)
        result = self._worker.submit(self._authentication.sign_in, username, password)
        self._when_ready(result, self._complete_sign_in)

    def _complete_sign_in(self, result: Future[User | None]) -> None:
        try:
            user = result.result()
        except psycopg.Error:
            logger.exception("Sign-in failed.")
            self._view.show_status("Could not sign in. Check the database setup.", busy=False)
            return

        if user is None:
            self._view.show_status("Username or password is incorrect.", busy=False)
            return

        self._signed_in_user = user
        title = (
            "Administrator account"
            if self._signed_in_user.role is Role.ADMIN
            else "Regular account"
        )
        self._view.show_account(title, self._signed_in_user.username)

    def _sign_out(self) -> None:
        self._signed_in_user = None
        self._view.show_sign_in()

    def _check_connection(self) -> None:
        self._view.show_status("Checking PostgreSQL...", busy=True)
        result = self._worker.submit(self._database.check_connection)
        self._when_ready(result, self._complete_connection_check)

    def _complete_connection_check(self, result: Future[bool]) -> None:
        try:
            if result.result():
                message = "Connected to PostgreSQL."
            else:
                message = "PostgreSQL returned an unexpected result."
        except psycopg.Error:
            logger.exception("Database connection check failed.")
            message = "Connection failed. Check your settings and PostgreSQL."

        self._view.show_status(message, busy=False)

    def _when_ready(
        self, result: Future[T], on_complete: Callable[[Future[T]], None]
    ) -> None:
        if result.done():
            self._pending_callback = None
            on_complete(result)
        else:
            self._pending_callback = self._window.after(
                50, self._when_ready, result, on_complete
            )

    def _close_window(self) -> None:
        if self._pending_callback is not None:
            self._window.after_cancel(self._pending_callback)
        self._window.destroy()
