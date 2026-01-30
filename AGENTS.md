# Repository Guidelines

## Project Structure & Module Organization
This is a multi-module Maven monorepo for Spring Cloud microservices. Top-level modules include `ww-api-gateway`, `ww-auth`, `ww-mall`, `ww-im`, `ww-framework`, and others. Each service or submodule follows standard Maven layout: `src/main/java` for code and `src/main/resources` for configs/assets. Tests live in `src/test/java`. Database scripts are under `script/sql`.

## Build, Test, and Development Commands
- `mvn clean package -DskipTests`: Build all modules without running tests.
- `mvn clean package -pl ww-api-gateway -am -DskipTests`: Build a single module plus its dependencies.
- `mvn test`: Run tests across the repo.
- `mvn -pl ww-api-gateway test`: Run tests for one module.
- `java -jar ww-api-gateway/target/ww-api-gateway.jar`: Run the packaged gateway service.

## Coding Style & Naming Conventions
- Java: follow Alibaba Java style (UpperCamelCase classes, lowerCamelCase methods/vars, UPPER_SNAKE_CASE constants).
- Javadoc is expected on classes, fields, and methods.
- Frontend (if working in UI modules): Vue components in PascalCase, CSS uses BEM, JS uses camelCase.
- Formatters/linters are not centrally configured here; keep code consistent with nearby files.

## Testing Guidelines
- Tests use Spring Boot’s `spring-boot-starter-test` (JUnit-based). Place unit tests in `src/test/java`.
- Prefer naming tests as `*Test` or `*Tests` and keep them near the module they cover.
- No explicit coverage thresholds are defined; keep coverage meaningful for new logic.

## Commit & Pull Request Guidelines
- Git history is not available in this environment; follow the documented Angular-style format:
  `<type>(<scope>): <subject>` with optional body/footer (e.g., `feat(ww-auth): add token refresh`).
- PRs should include: summary, testing notes (commands run), and screenshots for UI changes.

## Configuration & Security Tips
- Service config typically lives in `application.yml`/`bootstrap.yml` and Nacos.
- Do not commit secrets; use local overrides or environment variables.
- Database setup is under `script/sql`; keep schema changes versioned and documented.

## Concurrency & Distributed System Rules

- Any code touching:
  - Redis
  - MQ consumers/producers
  - scheduled jobs
  - distributed locks
  - retry mechanisms

  must explicitly consider:
  - idempotency
  - duplicate execution
  - failure and retry scenarios
  - cluster / multi-instance behavior

- Avoid global locks; scope locks by business key
- Consumers must be idempotent by design
- Assume at-least-once delivery semantics



