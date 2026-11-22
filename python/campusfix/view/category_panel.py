from collections.abc import Callable
from dataclasses import dataclass
from tkinter import StringVar, ttk


@dataclass(frozen=True)
class CategoryRow:
    id: int
    name: str
    active: bool


class CategoryPanel(ttk.Frame):
    def __init__(self, parent: ttk.Frame) -> None:
        super().__init__(parent)
        self._rows: dict[str, CategoryRow] = {}
        self._busy = False
        self._new_name = StringVar(master=self)
        self._selected_name = StringVar(master=self)

        table = ttk.Frame(self)
        table.pack(fill="both", expand=True)
        table.columnconfigure(0, weight=1)
        table.rowconfigure(0, weight=1)
        self._table = ttk.Treeview(
            table, columns=("name", "status"), show="headings", selectmode="browse", height=6
        )
        self._table.heading("name", text="Category")
        self._table.heading("status", text="Status")
        self._table.column("status", width=100, stretch=False)
        self._table.grid(row=0, column=0, sticky="nsew")
        scrollbar = ttk.Scrollbar(table, orient="vertical", command=self._table.yview)
        scrollbar.grid(row=0, column=1, sticky="ns")
        self._table.configure(yscrollcommand=scrollbar.set)
        self._table.bind("<<TreeviewSelect>>", self._show_selection)

        editor = ttk.Frame(self)
        editor.pack(fill="x", pady=(8, 0))
        editor.columnconfigure(1, weight=1)

        ttk.Label(editor, text="New category").grid(row=0, column=0, sticky="w")
        self._new_entry = ttk.Entry(editor, textvariable=self._new_name)
        self._new_entry.grid(row=0, column=1, sticky="ew", padx=8)
        self._add_button = ttk.Button(editor, text="Add category")
        self._add_button.grid(row=0, column=2, sticky="w")

        ttk.Label(editor, text="Selected category").grid(row=1, column=0, sticky="w")
        self._selected_entry = ttk.Entry(editor, textvariable=self._selected_name)
        self._selected_entry.grid(row=1, column=1, sticky="ew", padx=8, pady=(4, 0))
        self._rename_button = ttk.Button(editor, text="Rename")
        self._rename_button.grid(row=1, column=2, sticky="w", pady=(4, 0))
        self._toggle_button = ttk.Button(editor)
        self._toggle_button.grid(row=1, column=3, sticky="w", padx=(8, 0), pady=(4, 0))

        self._status = ttk.Label(self)
        self._status.pack(fill="x", pady=(8, 0))
        self._update_actions()

    def on_add(self, action: Callable[[], None]) -> None:
        self._add_button.configure(command=action)

    def on_rename(self, action: Callable[[], None]) -> None:
        self._rename_button.configure(command=action)

    def on_toggle_active(self, action: Callable[[], None]) -> None:
        self._toggle_button.configure(command=action)

    def new_name(self) -> str:
        return self._new_name.get()

    def selected_name(self) -> str:
        return self._selected_name.get()

    def selected_category(self) -> CategoryRow | None:
        selection = self._table.selection()
        return self._rows.get(selection[0]) if selection else None

    def clear_new_name(self) -> None:
        self._new_name.set("")

    def show_rows(self, rows: list[CategoryRow]) -> None:
        selected = self.selected_category()
        self._table.delete(*self._table.get_children())
        self._rows = {str(row.id): row for row in rows}
        for row in rows:
            status = "Active" if row.active else "Inactive"
            self._table.insert("", "end", iid=str(row.id), values=(row.name, status))
        if rows:
            selected_id = str(selected.id) if selected is not None else str(rows[0].id)
            if selected_id not in self._rows:
                selected_id = str(rows[0].id)
            self._table.selection_set(selected_id)
            self._table.see(selected_id)
        self._show_selection()

    def show_status(self, message: str, *, busy: bool) -> None:
        self._status.configure(text=message)
        self._busy = busy
        self._update_actions()

    def _show_selection(self, _event: object = None) -> None:
        category = self.selected_category()
        self._selected_name.set(category.name if category is not None else "")
        self._update_actions()

    def _update_actions(self) -> None:
        category = self.selected_category()
        new_state = "disabled" if self._busy else "normal"
        can_edit = category is not None and not self._busy
        selected_state = "normal" if can_edit else "disabled"
        self._new_entry.configure(state=new_state)
        self._add_button.configure(state=new_state)
        self._selected_entry.configure(state=selected_state)
        self._rename_button.configure(state=selected_state)
        active = category is not None and category.active
        label = "Deactivate" if active else "Reactivate"
        self._toggle_button.configure(text=label, state=selected_state)
