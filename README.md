# CampusFix

Campus maintenance reporting application.

## Run locally

On first setup, copy `.env.example` to `.env` and set the database passwords.

For Java, start Docker and use JDK 17 or newer. Run the launcher from the
repository root to start PostgreSQL, apply the migrations, and open the client.

```sh
bash java/run.sh
```

For Python, [apply the migrations and set up the virtual environment](docs/python-desktop.md#run-locally).
Then run the desktop client.

```sh
cd python
python -m campusfix
```

For Android, see the [Android guide](docs/android.md) for device setup and launch.

The [Java desktop guide](docs/java-desktop.md) and
[Python desktop guide](docs/python-desktop.md) cover configuration, tests,
architecture, and changesets.
