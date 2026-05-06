# AGENTS.md - EHR System

## Build & Run

- **Build**: `./mvnw clean package -DskipTests`
- **Run**: `./mvnw spring-boot:run` or `java -jar target/ehr-system-0.0.1-SNAPSHOT.jar`
- **Dev**: Uses Spring Boot DevTools (auto-restart on code change)

## Testing

- **Run tests**: `./mvnw test`
- **Run single test**: `./mvnw test -Dtest=AuthIntegrationTest`
- Integration tests require PostgreSQL running at `localhost:5432/ehr_system`

## Database

- PostgreSQL required. Connection in `src/main/resources/application.properties`
- Migrations: Flyway (`src/main/resources/db/migration/V*.sql`)
- **DDL mode**: `validate` - migrations must match schema, no auto-creation

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/api-docs`

## Important Config

- `spring.jpa.open-in-view=false` - disabled by default
- `logging.level.org.springframework.security=DEBUG` - verbose security logs
- JWT secret is hardcoded in `application.properties` (not env var)

## Project Structure

- `src/main/java/com/example/ehrsystem/` - main source
- Modules under `modules/` (auth, user, role, permission)
- Controllers in `modules/*/controller/`
- Entities in `modules/*/entity/`
- Repositories in `modules/*/repository/`