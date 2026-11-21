from collections.abc import Callable
from tkinter import Tk, ttk


class CampusFixView:
    def __init__(self, window: Tk) -> None:
        window.title("CampusFix")
        window.minsize(450, 190)

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

        self._account_panel = ttk.Frame(content)
        self._account_title = ttk.Label(self._account_panel)
        self._account_title.pack(anchor="w")
        self._account_name = ttk.Label(self._account_panel)
        self._account_name.pack(anchor="w", pady=12)
        self._sign_out_button = ttk.Button(self._account_panel, text="Sign out")
        self._sign_out_button.pack(anchor="w")

    def on_sign_in(self, action: Callable[[], None]) -> None:
        self._sign_in_button.configure(command=action)
        self._username.bind("<Return>", lambda _event: self._sign_in_button.invoke())
        self._password.bind("<Return>", lambda _event: self._sign_in_button.invoke())

    def on_check(self, action: Callable[[], None]) -> None:
        self._check_button.configure(command=action)

    def on_sign_out(self, action: Callable[[], None]) -> None:
        self._sign_out_button.configure(command=action)

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

    def show_sign_in(self) -> None:
        self._account_panel.pack_forget()
        self._sign_in_panel.pack(fill="both", expand=True)
        self.show_status("Sign in to CampusFix.", busy=False)
        self._username.delete(0, "end")
        self._password.delete(0, "end")
        self._username.focus_set()

    def show_account(self, title: str, username: str) -> None:
        self._sign_in_panel.pack_forget()
        self._account_title.configure(text=title)
        self._account_name.configure(text=f"Signed in as {username}")
        self._account_panel.pack(fill="both", expand=True)
        self._sign_out_button.focus_set()
