# Android client

The Android client lets regular users submit maintenance reports and review
their own reports. They can edit or cancel a report while it is open.
Administrators can review reports from all users, start work, record resolutions,
and manage report categories.

## Changesets

### Foundation

The Android client checks its connection to PostgreSQL when the user taps
**Check database connection**. It reads the connection settings from a private
`.env` file on the device and queries `SELECT 1` through JDBC.

### Account sign-in

The sign-in form accepts a username and password. After sign-in, the account
screen shows the username and role. Sign-out returns to the form.

### Submit and list maintenance reports

Regular users submit a title, location, category, and description. New reports
start in `OPEN`. The ViewModel loads active categories for the report form and
publishes the report list for the signed-in account. **Refresh** loads changes
from other sessions.

### Edit and cancel open reports

Regular users can edit or cancel their own open reports. **Edit report** loads
the report into the form. **Discard edit** returns to a blank form. Cancelling
a report keeps it visible with status `CANCELLED`.

`IssueService` validates edited fields and checks the account role. The
update statements check the reporter and current status in PostgreSQL, so
changes from a stale screen are rejected after the report is cancelled.

### Start work and resolve reports

Administrators move an `OPEN` report to `IN_PROGRESS` with **Start work**.
On a report in progress, **Resolve report** opens the resolution form. A
nonblank note of up to 2,000 characters is required to mark it `RESOLVED`.
The note appears on the report for the reporter and administrators.

`IssueService` validates the role and note. The Model checks the current
status in the same database statement that advances it. Resolution saves
the new status and note together.

### Manage report categories

Administrators open **Categories** to add a category, rename it, deactivate it,
or reactivate it. The ViewModel publishes the current section, category list,
and operation status. The Activity renders that state, and `CategoryService`
checks permission and validates changes before the DAO writes them.

Report forms offer active categories. Existing reports keep their category ID
and display the current category name even after deactivation.

## Architecture

```mermaid
classDiagram
    direction LR
    class CrudDao {
        <<interface>>
    }
    CampusFixApplication ..> CampusFixViewModel : creates
    CampusFixApplication ..> DatabaseConnection : creates
    MainActivity ..> CampusFixViewModel : observes and calls
    MainActivity *-- CategoryPanel : category editor
    CategoryPanel ..> CampusFixViewModel : calls
    CampusFixViewModel --> DatabaseConnection : database
    CampusFixViewModel --> AuthenticationService : authentication
    CampusFixViewModel --> IssueService : reports
    CampusFixViewModel --> CategoryService : categories
    AuthenticationService --> UserDao : users
    IssueService --> IssueDao : issues
    IssueService --> CategoryDao : categories
    CategoryService --> CategoryDao : categories
    UserDao --> DatabaseConnection : database
    IssueDao --> DatabaseConnection : database
    CategoryDao --> DatabaseConnection : database
    CrudDao <|.. UserDao
    CrudDao <|.. IssueDao
    CrudDao <|.. CategoryDao
```

- **View.** `MainActivity` reads the forms, forwards actions, and renders the
  ViewModel's state. It preserves unfinished report edits, resolution notes,
  and category drafts with their IDs across rotation. A successful save or
  sign-out clears the corresponding draft. `ViewModelProvider` supplies the
  ViewModel through the application factory.
- **ViewModel.** `CampusFixViewModel` owns the account, reports, category lists,
  current section, and operation status. Its `state()` method exposes
  `LiveData<State>` for the Activity to observe. Updates go through a private `MutableLiveData`. The same
  ViewModel serves the recreated Activity after rotation. One worker runs
  database operations and closes the connection when the ViewModel is cleared.
- **Model.** `AuthenticationService` verifies credentials, and `IssueService`
  validates report changes and selects reports by role. `CategoryService`
  restricts category management to administrators. The DAOs implement
  `CrudDao` with parameterized JDBC queries. `DatabaseConnection` opens and
  reuses an autocommit connection.

## Run locally

Use JDK 17 or newer. In [Android Studio's SDK Manager](https://developer.android.com/studio/intro/update#sdk-manager),
install Android SDK Platform 35, Build Tools 35.0.0, and Platform Tools (`adb`).
For a virtual device, install the Android Emulator and an Android 15 or newer
system image. The build uses SDK Platform 35 regardless of the device image.
Android Studio can open the `android/` directory.

Android Studio configures the SDK path in `android/local.properties`.
For terminal-only setup, set `sdk.dir` there once or use `ANDROID_HOME`.
For example, for the default macOS SDK location, add the following line
with your actual account name.

```properties
sdk.dir=/Users/yourname/Library/Android/sdk
```

Create an emulator in Device Manager or connect a debug device running
Android 9 or newer. Start Docker and create the root `.env` from `.env.example`
if needed. Use the local `jdbc:postgresql://localhost:5432/campusfix` URL.

From the repository root, run the launcher in Bash or Zsh. On Windows,
use Git Bash.

```sh
bash android/run.sh
```

The launcher reuses a connected device or starts the only configured emulator.
If you have several emulator profiles, start the one you want first or set
`campusfix.avd` to its name in `android/local.properties`.

Each run starts or reuses PostgreSQL and applies the migrations through
`database/005-report-resolution.sql`.
The scripts preserve existing accounts, reports, and categories. The launcher
installs the app, forwards the database port, and replaces the app's private connection
settings before opening it. A failed step returns you to your terminal.
Emulator startup output goes to `android/build/emulator.log`.

| Username | Password | Role |
| --- | --- | --- |
| `alice` | `alice-demo` | Regular |
| `bob` | `bob-demo` | Regular |
| `admin` | `admin-demo` | Administrator |

These sample accounts are for local development databases. As `admin`, tap
**Categories** to open the editor and **Reports** to return to the report list.
**Refresh** reloads the current list.

## Verification

Submit a report as `alice`. Open it with **Edit report**, change the title, and
rotate the device. Check that the edit still targets the same report, then
save and cancel it. Confirm that the list shows `CANCELLED`. Sign in as `bob`
to confirm the report is absent, then as `admin` to find it under **All reports**.
Use **Check database connection** on the sign-in screen to verify PostgreSQL
access.

Submit another report as `alice`, then sign in as `admin` and tap **Start work**.
Open **Resolve report**, enter a note, and rotate the device to verify that
the note and report ID survive. Save the note and sign back in as the reporter
to confirm the report shows `RESOLVED` and displays the note.

As `admin`, add a category and rename one that has a report. Deactivate that
category and check the existing report still displays its new name. As `alice`,
check the category picker omits it. Reactivate it as `admin`, then sign in as
`alice` again to verify it is available for new reports.

The debug build keeps credentials out of the APK and Git. Use this client with
a trusted local development database. An established connection is not recovered
after a network failure. Restart the app to reconnect.
