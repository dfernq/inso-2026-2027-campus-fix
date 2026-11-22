import logging
from collections.abc import Callable
from concurrent.futures import Future, ThreadPoolExecutor
from tkinter import Tk
from typing import TypeVar

import psycopg

from campusfix.model.auth.authentication_service import AuthenticationService
from campusfix.model.domain.issue import Status
from campusfix.model.domain.user import Role, User
from campusfix.model.persistence.database_connection import DatabaseConnection
from campusfix.model.reports.issue_service import IssueService
from campusfix.view.campusfix_view import CampusFixView, CategoryOption, ReportRow


logger = logging.getLogger(__name__)
T = TypeVar("T")


class CampusFixController:
    def __init__(
        self,
        database: DatabaseConnection,
        authentication: AuthenticationService,
        reports: IssueService,
    ) -> None:
        self._database = database
        self._authentication = authentication
        self._reports = reports
        self._window = Tk()
        self._view = CampusFixView(self._window)
        self._worker = ThreadPoolExecutor(max_workers=1)
        self._signed_in_user: User | None = None
        self._editing_report_id: int | None = None
        self._pending_callback: str | None = None
        self._view.on_sign_in(self._sign_in)
        self._view.on_check(self._check_connection)
        self._view.on_sign_out(self._sign_out)
        self._view.on_refresh(self._load_reports)
        self._view.on_submit_report(self._save_report)
        self._view.on_edit_report(self._begin_edit)
        self._view.on_cancel_report(self._cancel_report)
        self._view.on_discard_edit(self._discard_edit)
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
        regular = user.role is Role.REGULAR
        title = "Regular account" if regular else "Administrator account"
        self._view.show_account(title, user.username, can_submit=regular)
        self._load_reports()

    def _sign_out(self) -> None:
        self._cancel_pending_callback()
        self._signed_in_user = None
        self._editing_report_id = None
        self._view.show_sign_in()

    def _load_reports(self, *, change_message: str | None = None) -> None:
        user = self._signed_in_user
        if user is None:
            return
        message = f"{change_message} Refreshing..." if change_message else "Loading reports..."
        self._view.show_report_status(message, busy=True)
        result = self._worker.submit(self._report_data, user)
        self._when_ready(
            result,
            lambda completed: self._complete_report_load(completed, change_message),
        )

    def _report_data(self, user: User) -> tuple[list[ReportRow], list[CategoryOption]]:
        rows = []
        for report in self._reports.list(user):
            issue = report.issue
            can_manage = user.role is Role.REGULAR and issue.status is Status.OPEN
            rows.append(
                ReportRow(
                    issue.id,
                    issue.title,
                    issue.category_id,
                    report.category_name,
                    issue.location,
                    report.reporter_name,
                    issue.status.value,
                    issue.description,
                    can_manage,
                )
            )

        categories = []
        if user.role is Role.REGULAR:
            categories = [
                CategoryOption(category.id, category.name)
                for category in self._reports.active_categories()
            ]
        return rows, categories

    def _complete_report_load(
        self,
        result: Future[tuple[list[ReportRow], list[CategoryOption]]],
        change_message: str | None,
    ) -> None:
        try:
            rows, categories = result.result()
        except psycopg.Error:
            logger.exception("Loading reports failed.")
            message = (
                f"{change_message} Could not refresh reports."
                if change_message
                else "Could not load reports. Try Refresh."
            )
        else:
            self._view.show_reports(rows)
            self._view.show_categories(categories)
            message = change_message or ("Reports up to date." if rows else "No reports yet.")
        self._view.show_report_status(message, busy=False)

    def _begin_edit(self) -> None:
        report = self._view.selected_report()
        if report is None or not report.can_manage:
            return
        self._editing_report_id = report.id
        self._view.show_report_for_editing(report)

    def _discard_edit(self) -> None:
        self._editing_report_id = None
        self._view.clear_report_form()

    def _save_report(self) -> None:
        user = self._signed_in_user
        if user is None:
            return
        issue_id = self._editing_report_id
        category_id = self._view.report_category_id()
        title = self._view.report_title()
        description = self._view.report_description()
        location = self._view.report_location()

        if issue_id is None:
            self._view.show_report_status("Submitting report...", busy=True)
            result = self._worker.submit(
                self._reports.submit, user, category_id, title, description, location
            )
            message = "Report submitted."
        else:
            self._view.show_report_status("Saving report...", busy=True)
            result = self._worker.submit(
                self._reports.edit, user, issue_id, category_id, title, description, location
            )
            message = "Report updated."

        self._when_ready(
            result,
            lambda completed: self._complete_report_change(
                completed, message, "Could not save the report.", clear_form=True
            ),
        )

    def _cancel_report(self) -> None:
        user = self._signed_in_user
        report = self._view.selected_report()
        if user is None or report is None:
            return
        if not report.can_manage or not self._view.confirm_cancellation(report.id):
            return

        self._view.show_report_status("Cancelling report...", busy=True)
        result = self._worker.submit(self._reports.cancel, user, report.id)
        self._when_ready(
            result,
            lambda completed: self._complete_report_change(
                completed,
                "Report cancelled.",
                "Could not cancel the report.",
                clear_form=self._editing_report_id == report.id,
            ),
        )

    def _complete_report_change(
        self, result: Future[T], message: str, failure_message: str, *, clear_form: bool
    ) -> None:
        try:
            result.result()
        except ValueError as error:
            self._view.show_report_status(str(error), busy=False)
            return
        except psycopg.Error:
            logger.exception(failure_message)
            self._view.show_report_status(failure_message, busy=False)
            return

        self._view.show_report_status(message, busy=False)
        if clear_form:
            self._discard_edit()
        self._load_reports(change_message=message)

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

    def _cancel_pending_callback(self) -> None:
        if self._pending_callback is not None:
            self._window.after_cancel(self._pending_callback)
            self._pending_callback = None

    def _close_window(self) -> None:
        self._cancel_pending_callback()
        self._window.destroy()
