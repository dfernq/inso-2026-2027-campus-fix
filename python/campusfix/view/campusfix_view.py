from collections.abc import Callable
from dataclasses import dataclass
from tkinter import Text, Tk, messagebox, ttk

from campusfix.view.category_panel import CategoryPanel


@dataclass(frozen=True)
class CategoryOption:
    id: int
    name: str


@dataclass(frozen=True)
class ReportRow:
    id: int
    title: str
    category_id: int
    category: str
    location: str
    reporter: str
    status: str
    description: str
    resolution_note: str | None
    can_manage: bool
    can_start_work: bool
    can_resolve: bool


class CampusFixView:
    def __init__(self, window: Tk) -> None:
        self._window = window
        window.title("CampusFix")
        self._categories: list[CategoryOption] = []
        self._displayed_reports: dict[str, ReportRow] = {}
        self._report_busy = False
        self._resolution_report_id: int | None = None

        content = ttk.Frame(window, padding=24)
        content.pack(fill="both", expand=True)

        self._sign_in_panel = ttk.Frame(content)
        self._sign_in_panel.columnconfigure(1, weight=1)
        ttk.Label(self._sign_in_panel, text="Username").grid(row=0, column=0, sticky="w")
        self._username = ttk.Entry(self._sign_in_panel)
        self._username.grid(row=0, column=1, sticky="ew", padx=(12, 0))

        ttk.Label(self._sign_in_panel, text="Password").grid(row=1, column=0, sticky="w")
        self._password = ttk.Entry(self._sign_in_panel, show="*")
        self._password.grid(row=1, column=1, sticky="ew", padx=(12, 0), pady=(8, 0))

        actions = ttk.Frame(self._sign_in_panel)
        actions.grid(row=2, column=0, columnspan=2, sticky="w", pady=(12, 0))
        self._sign_in_button = ttk.Button(actions, text="Sign in")
        self._sign_in_button.pack(side="left")
        self._check_button = ttk.Button(actions, text="Check database connection")
        self._check_button.pack(side="left", padx=(8, 0))
        self._status = ttk.Label(self._sign_in_panel)
        self._status.grid(row=3, column=0, columnspan=2, sticky="w", pady=(12, 0))

        self._create_account_panel(content)

    def _create_account_panel(self, content: ttk.Frame) -> None:
        self._account_panel = ttk.Frame(content)
        header = ttk.Frame(self._account_panel)
        header.pack(fill="x", pady=(0, 12))
        heading = ttk.Frame(header)
        heading.pack(side="left")
        self._account_title = ttk.Label(heading)
        self._account_title.pack(anchor="w")
        self._account_name = ttk.Label(heading)
        self._account_name.pack(anchor="w", pady=(4, 0))

        self._sign_out_button = ttk.Button(header, text="Sign out")
        self._sign_out_button.pack(side="right")
        self._refresh_button = ttk.Button(header, text="Refresh")
        self._refresh_button.pack(side="right", padx=(0, 8))

        self._section_button = ttk.Button(header, text="Categories")
        body = ttk.Frame(self._account_panel)
        body.pack(fill="both", expand=True)
        self._report_panel = ttk.Frame(body)
        self.category_panel = CategoryPanel(body)

        self._create_report_list()
        self._create_report_form()
        self._create_resolution_form()
        self._report_status = ttk.Label(self._report_panel)
        self._report_status.pack(fill="x", pady=(8, 0))

    def _create_report_list(self) -> None:
        self._report_list_title = ttk.Label(self._report_panel)
        self._report_list_title.pack(anchor="w", pady=(0, 4))

        table = ttk.Frame(self._report_panel)
        table.pack(fill="both", expand=True)
        table.columnconfigure(0, weight=1)
        table.rowconfigure(0, weight=1)
        columns = ("ID", "Title", "Category", "Location", "Reporter", "Status")
        self._reports = ttk.Treeview(
            table, columns=columns, show="headings", selectmode="browse", height=6
        )
        for name, width in zip(columns, (45, 170, 110, 125, 90, 105)):
            self._reports.heading(name, text=name)
            self._reports.column(name, width=width, stretch=name == "Title")
        self._reports.grid(row=0, column=0, sticky="nsew")
        scrollbar = ttk.Scrollbar(table, orient="vertical", command=self._reports.yview)
        scrollbar.grid(row=0, column=1, sticky="ns")
        self._reports.configure(yscrollcommand=scrollbar.set)
        self._reports.bind("<<TreeviewSelect>>", lambda _event: self._show_selected_report())

        details = ttk.LabelFrame(self._report_panel, text="Report description", padding=8)
        details.pack(fill="x", pady=(8, 0))
        self._selected_description = Text(
            details, height=3, wrap="word", state="disabled"
        )
        self._selected_description.pack(fill="x")

        self._report_actions = ttk.Frame(self._report_panel)
        self._edit_button = ttk.Button(self._report_actions, text="Edit report", state="disabled")
        self._edit_button.pack(side="left")
        self._cancel_report_button = ttk.Button(
            self._report_actions, text="Cancel report", state="disabled"
        )
        self._cancel_report_button.pack(side="left", padx=(8, 0))

        self._admin_actions = ttk.Frame(self._report_panel)
        self._start_work_button = ttk.Button(
            self._admin_actions, text="Start work", state="disabled"
        )
        self._start_work_button.pack(side="left")

    def _create_report_form(self) -> None:
        self._report_form = ttk.LabelFrame(
            self._report_panel, text="New maintenance report", padding=8
        )
        self._report_form.columnconfigure(1, weight=1)
        self._report_title = ttk.Entry(self._report_form)
        self._report_location = ttk.Entry(self._report_form)
        self._report_category = ttk.Combobox(self._report_form, state="readonly")
        self._report_description = Text(self._report_form, height=3, wrap="word")

        fields = (
            ("Title", self._report_title),
            ("Location", self._report_location),
            ("Category", self._report_category),
            ("Description", self._report_description),
        )
        for row, (label, field) in enumerate(fields):
            ttk.Label(self._report_form, text=label).grid(row=row, column=0, sticky="nw")
            field.grid(row=row, column=1, sticky="ew", padx=(12, 0), pady=(0, 4))

        actions = ttk.Frame(self._report_form)
        actions.grid(row=4, column=0, columnspan=2, sticky="w")
        self._submit_button = ttk.Button(actions, text="Submit report")
        self._submit_button.pack(side="left")
        self._discard_edit_button = ttk.Button(actions, text="Discard edit")

    def _create_resolution_form(self) -> None:
        self._resolution_form = ttk.LabelFrame(
            self._report_panel, text="Resolution note", padding=8
        )
        self._resolution_note = Text(self._resolution_form, height=3, wrap="word")
        self._resolution_note.pack(fill="x")
        self._resolve_report_button = ttk.Button(self._resolution_form, text="Resolve report")
        self._resolve_report_button.pack(anchor="w", pady=(6, 0))

    def on_sign_in(self, action: Callable[[], None]) -> None:
        self._sign_in_button.configure(command=action)
        self._username.bind("<Return>", lambda _event: self._sign_in_button.invoke())
        self._password.bind("<Return>", lambda _event: self._sign_in_button.invoke())

    def on_check(self, action: Callable[[], None]) -> None:
        self._check_button.configure(command=action)

    def on_sign_out(self, action: Callable[[], None]) -> None:
        self._sign_out_button.configure(command=action)

    def on_refresh(self, action: Callable[[], None]) -> None:
        self._refresh_button.configure(command=action)

    def on_switch_section(self, action: Callable[[], None]) -> None:
        self._section_button.configure(command=action)

    def on_submit_report(self, action: Callable[[], None]) -> None:
        self._submit_button.configure(command=action)

    def on_edit_report(self, action: Callable[[], None]) -> None:
        self._edit_button.configure(command=action)

    def on_cancel_report(self, action: Callable[[], None]) -> None:
        self._cancel_report_button.configure(command=action)

    def on_discard_edit(self, action: Callable[[], None]) -> None:
        self._discard_edit_button.configure(command=action)

    def on_start_work(self, action: Callable[[], None]) -> None:
        self._start_work_button.configure(command=action)

    def on_resolve_report(self, action: Callable[[], None]) -> None:
        self._resolve_report_button.configure(command=action)

    def username(self) -> str:
        return self._username.get()

    def take_password(self) -> str:
        value = self._password.get()
        self._password.delete(0, "end")
        return value

    def show_status(self, message: str, *, busy: bool) -> None:
        self._status.configure(text=message)
        state = "disabled" if busy else "normal"
        self._username.configure(state=state)
        self._password.configure(state=state)
        self._sign_in_button.configure(state=state)
        self._check_button.configure(state=state)

    def report_title(self) -> str:
        return self._report_title.get()

    def report_location(self) -> str:
        return self._report_location.get()

    def report_description(self) -> str:
        return self._report_description.get("1.0", "end-1c")

    def resolution_note(self) -> str:
        return self._resolution_note.get("1.0", "end-1c")

    def clear_resolution_note(self, issue_id: int) -> None:
        if self._resolution_report_id == issue_id:
            self._clear_resolution_draft()

    def _clear_resolution_draft(self) -> None:
        self._resolution_note.configure(state="normal")
        self._resolution_note.delete("1.0", "end")
        if self._report_busy:
            self._resolution_note.configure(state="disabled")

    def report_category_id(self) -> int:
        index = self._report_category.current()
        return self._categories[index].id if index >= 0 else 0

    def show_report_status(self, message: str, *, busy: bool) -> None:
        self._report_busy = busy
        self._report_status.configure(text=message)
        state = "disabled" if busy else "normal"
        self.set_navigation_busy(busy)
        self._submit_button.configure(state=state)
        self._report_title.configure(state=state)
        self._report_location.configure(state=state)
        self._report_description.configure(state=state)
        self._report_category.configure(state="disabled" if busy else "readonly")
        self._discard_edit_button.configure(state=state)
        self._update_report_actions()

    def set_navigation_busy(self, busy: bool) -> None:
        state = "disabled" if busy else "normal"
        self._refresh_button.configure(state=state)
        self._section_button.configure(state=state)

    def show_category_section(self, visible: bool) -> None:
        if visible:
            self._report_panel.pack_forget()
            self.category_panel.pack(fill="both", expand=True)
            self._section_button.configure(text="Reports")
        else:
            self.category_panel.pack_forget()
            self._report_panel.pack(fill="both", expand=True)
            self._section_button.configure(text="Categories")

    def show_categories(self, categories: list[CategoryOption]) -> None:
        selected_id = self.report_category_id()
        self._categories = categories
        names = [category.name for category in categories]
        self._report_category.configure(values=names)
        self._report_category.set("")
        for index, category in enumerate(categories):
            if category.id == selected_id:
                self._report_category.current(index)
                break

    def show_reports(self, rows: list[ReportRow]) -> None:
        selection = self._reports.selection()
        selected_id = selection[0] if selection else ""
        self._reports.delete(*self._reports.get_children())
        self._displayed_reports = {str(row.id): row for row in rows}
        for row in rows:
            values = (
                row.id, row.title, row.category, row.location, row.reporter, row.status
            )
            self._reports.insert("", "end", iid=str(row.id), values=values)
        if rows:
            if selected_id not in self._displayed_reports:
                selected_id = str(rows[0].id)
            self._reports.selection_set(selected_id)
            self._reports.see(selected_id)
        self._show_selected_report()

    def selected_report(self) -> ReportRow | None:
        selection = self._reports.selection()
        return self._displayed_reports.get(selection[0]) if selection else None

    def _show_selected_report(self) -> None:
        report = self.selected_report()
        if report is not None and report.id != self._resolution_report_id:
            self._resolution_report_id = report.id
            self._clear_resolution_draft()

        description = report.description if report is not None else ""
        if report is not None and report.resolution_note is not None:
            description = f"{description}\n\nResolution note\n{report.resolution_note}"
        self._selected_description.configure(state="normal")
        self._selected_description.delete("1.0", "end")
        self._selected_description.insert("1.0", description)
        self._selected_description.configure(state="disabled")
        self._update_report_actions()

    def _update_report_actions(self) -> None:
        report = self.selected_report()
        manageable = report is not None and report.can_manage
        enabled = manageable and not self._report_busy
        state = "normal" if enabled else "disabled"
        self._edit_button.configure(state=state)
        self._cancel_report_button.configure(state=state)

        can_start_work = report is not None and report.can_start_work
        can_resolve = report is not None and report.can_resolve
        start_enabled = can_start_work and not self._report_busy
        resolve_enabled = can_resolve and not self._report_busy
        self._start_work_button.configure(state="normal" if start_enabled else "disabled")
        self._resolve_report_button.configure(state="normal" if resolve_enabled else "disabled")
        self._resolution_note.configure(state="disabled" if self._report_busy else "normal")
        if can_resolve:
            self._resolution_form.pack(fill="x", pady=(8, 0), before=self._report_status)
        else:
            self._resolution_form.pack_forget()

    def show_report_for_editing(self, report: ReportRow) -> None:
        self.clear_report_form()
        self._report_form.configure(text=f"Edit report #{report.id}")
        self._submit_button.configure(text="Save changes")
        self._discard_edit_button.pack(side="left", padx=(8, 0))
        self._report_title.insert(0, report.title)
        self._report_location.insert(0, report.location)
        self._report_description.insert("1.0", report.description)
        for index, category in enumerate(self._categories):
            if category.id == report.category_id:
                self._report_category.current(index)
                break
        self._report_title.focus_set()

    def confirm_cancellation(self, issue_id: int) -> bool:
        return messagebox.askyesno(
            "Cancel report", f"Cancel report #{issue_id}?",
            default=messagebox.NO, parent=self._window,
        )

    def clear_report_form(self) -> None:
        self._report_form.configure(text="New maintenance report")
        self._submit_button.configure(text="Submit report")
        self._discard_edit_button.pack_forget()
        self._report_title.delete(0, "end")
        self._report_location.delete(0, "end")
        self._report_description.delete("1.0", "end")
        self._report_category.set("")

    def show_sign_in(self) -> None:
        self._account_panel.pack_forget()
        self.show_report_status("", busy=False)
        self.clear_report_form()
        self._resolution_report_id = None
        self._clear_resolution_draft()
        self.show_reports([])
        self.show_categories([])
        self.category_panel.show_status("", busy=False)
        self.category_panel.show_rows([])
        self.category_panel.clear_new_name()
        self._sign_in_panel.pack(fill="both", expand=True)
        self._window.minsize(450, 190)
        self._window.geometry("450x190")
        self.show_status("Sign in to CampusFix.", busy=False)
        self._username.delete(0, "end")
        self._password.delete(0, "end")
        self._username.focus_set()

    def show_account(self, title: str, username: str, *, can_submit: bool) -> None:
        self._sign_in_panel.pack_forget()
        self._account_title.configure(text=title)
        self._account_name.configure(text=f"Signed in as {username}")
        list_title = "My reports" if can_submit else "All reports"
        self._report_list_title.configure(text=list_title)
        self.show_category_section(False)
        if can_submit:
            self._section_button.pack_forget()
            self._admin_actions.pack_forget()
            self._report_actions.pack(fill="x", pady=(8, 0), before=self._report_status)
            self._report_form.pack(fill="x", pady=(8, 0), before=self._report_status)
        else:
            self._section_button.pack(side="right", padx=(0, 8))
            self._report_actions.pack_forget()
            self._report_form.pack_forget()
            self._admin_actions.pack(fill="x", pady=(8, 0), before=self._report_status)
        self._account_panel.pack(fill="both", expand=True)
        self._window.minsize(660, 560)
        self._window.geometry("800x620")
        self._sign_out_button.focus_set()
