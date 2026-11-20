# CampusFix

Campus maintenance reporting application.

## Run the Java desktop client

Use JDK 17 or newer and Docker Compose. On first setup, copy `.env.example` to
`.env` and set the database passwords. Then run these commands from the
repository root:

```sh
docker compose up -d --wait
cd java
./gradlew :desktop:run
```

See the [Java desktop guide](docs/java-desktop.md) for configuration, tests,
architecture, and changesets.
