# AGENTS.md

## Project
Spring Boot 3.5.3 / Java 21 / Maven, single-module scaffold for a "picture-backend" service.
- Package root: `com.example.picturebackend` (entrypoint `PictureBackendApplication`)
- Lombok is on the classpath; install/enable the Lombok plugin in the IDE or compilation will fail on getters/setters.
- `HELP.md` is generated Spring Boot boilerplate and is gitignored — safe to ignore.

## Commands
Use the Maven wrapper, not a system `mvn`:
- Build: `./mvnw clean package`
- Run app: `./mvnw spring-boot:run`
- Tests: `./mvnw test` (see "Test prerequisites" below)
- On Windows use `./mvnw.cmd` if `mvnw` isn't executable.

## Infrastructure
`compose.yaml` provisions **only MySQL** (`my-dev-mysql`, port 3306, db `mydb`, root password `123456`). Credentials in `src/main/resources/application.yml` match this.

The pom also pulls in `spring-boot-starter-data-redis` and `spring-boot-starter-amqp`, but **no Redis or RabbitMQ services are defined in `compose.yaml`** and there is no connection config in `application.yml`. Default autoconfig targets `localhost:6379` (Redis) and `localhost:5672` (RabbitMQ); startup will hang/fail unless those are running locally or the autoconfigs are excluded.

Start MySQL before running the app or tests:
```
docker compose up -d mysql
```

## Test prerequisites
The only test is `PictureBackendApplicationTests.contextLoads`, a `@SpringBootTest`. It boots the full context, so it requires MySQL reachable at `localhost:3306` (matching `application.yml`). Without MySQL up, `./mvnw test` will fail. Redis/RabbitMQ autoconfig may also need to be excluded (e.g. via test properties) until those services exist locally.

## Conventions
- Configuration lives in `src/main/resources/application.yml`; no extra profiles or `application-*.yml` files exist yet.
- No controllers, services, entities, or repositories exist yet — this is a fresh scaffold. New code goes under `com.example.picturebackend.*`.
