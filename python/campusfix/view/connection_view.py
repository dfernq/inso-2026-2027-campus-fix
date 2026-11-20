from collections.abc import Callable
from tkinter import Tk, ttk


class ConnectionView:
    def __init__(self, window: Tk) -> None:
        window.title("CampusFix")
        window.minsize(450, 120)

        content = ttk.Frame(window, padding=24)
        content.pack(fill="both", expand=True)

        self._status = ttk.Label(content, text="Ready to check the database.")
        self._status.pack(anchor="w", pady=(0, 16))

        self._check_button = ttk.Button(content, text="Check database connection")
        self._check_button.pack(anchor="w")

    def on_check(self, action: Callable[[], None]) -> None:
        self._check_button.configure(command=action)

    def show_status(self, message: str, *, busy: bool) -> None:
        self._status.configure(text=message)
        self._check_button.configure(state="disabled" if busy else "normal")
