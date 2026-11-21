from collections.abc import Callable
from dataclasses import dataclass
from tkinter import Text, Tk, ttk


@dataclass(frozen=True)
class CategoryOption:
    id: int
    name: str


@dataclass(frozen=True)
class ReportRow:
    id: int
    title: str
    category: str
    location: str
    reporter: str
    status: str
    description: str


class CampusFixView:
    def __init__(self, window: Tk) -> None:
        self._window = window
        window.title("CampusFix")
        self._categories: list[CategoryOption] = []
        self._report_descriptions: dict[str, str] = {}

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

        self._create_report_list()
        self._create_report_form()
        self._report_status = ttk.Label(self._account_panel)
        self._report_status.pack(fill="x", pady=(8, 0))

    def _create_report_list(self) -> None:
        self._report_list_title = ttk.Label(self._account_panel)
        self._report_list_title.pack(anchor="w", pady=(0, 4))

        table = ttk.Frame(self._account_panel)
        table.pack(fill="both", expand=True)
        table.columnconfigure(0, weight=1)
        table.rowconfigure(0, weight=1)
        columns = ("ID", "Title", "Category", "Location", "Reporter", "Status")
        self._reports = ttk.Treeview(
            table, columns=columns, show="headings", selectmode="browse", height=6
        )
        for name, width in zip(columns, (45, 190, 110, 125, 90, 70)):
            self._reports.heading(name, text=name)
            self._reports.column(name, width=width, stretch=name == "Title")
        self._reports.grid(row=0, column=0, sticky="nsew")
        scrollbar = ttk.Scrollbar(table, orient="vertical", command=self._reports.yview)
        scrollbar.grid(row=0, column=1, sticky="ns")
        self._reports.configure(yscrollcommand=scrollbar.set)
        self._reports.bind("<<TreeviewSelect>>", lambda _event: self._show_description())

        details = ttk.LabelFrame(self._account_panel, text="Report description", padding=8)
        details.pack(fill="x", pady=(8, 0))
        self._selected_description = Text(
            details, height=3, wrap="word", state="disabled"
        )
        self._selected_description.pack(fill="x")

    def _create_report_form(self) -> None:
        self._report_form = ttk.LabelFrame(
            self._account_panel, text="New maintenance report", padding=8
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

        self._submit_button = ttk.Button(self._report_form, text="Submit report")
        self._submit_button.grid(row=4, column=0, columnspan=2, sticky="w")

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

    def on_submit_report(self, action: Callable[[], None]) -> None:
        self._submit_button.configure(command=action)

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

    def report_category_id(self) -> int:
        index = self._report_category.current()
        return self._categories[index].id if index >= 0 else 0

    def show_report_status(self, message: str, *, busy: bool) -> None:
        self._report_status.configure(text=message)
        state = "disabled" if busy else "normal"
        self._refresh_button.configure(state=state)
        self._submit_button.configure(state=state)
        self._report_title.configure(state=state)
        self._report_location.configure(state=state)
        self._report_description.configure(state=state)
        self._report_category.configure(state="disabled" if busy else "readonly")

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
        self._reports.delete(*self._reports.get_children())
        self._report_descriptions = {str(row.id): row.description for row in rows}
        for row in rows:
            values = (
                row.id, row.title, row.category, row.location, row.reporter, row.status
            )
            self._reports.insert("", "end", iid=str(row.id), values=values)
        if rows:
            self._reports.selection_set(str(rows[0].id))
        self._show_description()

    def _show_description(self) -> None:
        selection = self._reports.selection()
        description = self._report_descriptions.get(selection[0], "") if selection else ""
        self._selected_description.configure(state="normal")
        self._selected_description.delete("1.0", "end")
        self._selected_description.insert("1.0", description)
        self._selected_description.configure(state="disabled")

    def clear_report_form(self) -> None:
        self._report_title.delete(0, "end")
        self._report_location.delete(0, "end")
        self._report_description.delete("1.0", "end")
        self._report_category.set("")

    def show_sign_in(self) -> None:
        self._account_panel.pack_forget()
        self.show_report_status("", busy=False)
        self.clear_report_form()
        self.show_reports([])
        self.show_categories([])
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
        if can_submit:
            self._report_form.pack(fill="x", pady=(8, 0), before=self._report_status)
        else:
            self._report_form.pack_forget()
        self._account_panel.pack(fill="both", expand=True)
        self._window.minsize(660, 560)
        self._window.geometry("800x620")
        self._sign_out_button.focus_set()
