# CampusFix

Campus maintenance reporting application.

## Run locally

On first setup, copy `.env.example` to `.env` and set the database passwords.
Start PostgreSQL from the repository root:

```sh
docker compose up -d --wait
```

For Java, use JDK 17 or newer and [initialize the accounts](docs/java-desktop.md#initialize-accounts):

```sh
cd java
./gradlew :desktop:run
```

For Python, [set up the virtual environment](docs/python-desktop.md#run-locally),
then run:

```sh
cd python
python -m campusfix
```

For Android, see the [Android guide](docs/android.md) for device setup and launch.

The [Java desktop guide](docs/java-desktop.md) and
[Python desktop guide](docs/python-desktop.md) cover configuration, tests,
architecture, and changesets.
