from dataclasses import dataclass
from tkinter import ttk


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


class ReportTable(ttk.Treeview):
    def __init__(self, parent: ttk.Frame) -> None:
        columns = ("ID", "Title", "Category", "Location", "Reporter", "Status")
        super().__init__(parent, columns=columns, show="headings", selectmode="browse", height=6)
        self._rows: dict[str, ReportRow] = {}
        for name, width in zip(columns, (45, 170, 110, 125, 90, 105)):
            self.heading(name, text=name)
            self.column(name, width=width, stretch=name == "Title")

    def show_rows(self, rows: list[ReportRow]) -> None:
        selection = self.selection()
        selected_id = selection[0] if selection else ""
        self.delete(*self.get_children())
        self._rows = {str(row.id): row for row in rows}
        for item_id, row in self._rows.items():
            values = (row.id, row.title, row.category, row.location, row.reporter, row.status)
            self.insert("", "end", iid=item_id, values=values)

        if rows:
            if selected_id not in self._rows:
                selected_id = str(rows[0].id)
            self.selection_set(selected_id)
            self.see(selected_id)

    def selected_report(self) -> ReportRow | None:
        selection = self.selection()
        return self._rows.get(selection[0]) if selection else None
