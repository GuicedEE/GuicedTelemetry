# Guiced Telemetry

An OpenTelemetry addon for GuicedEE. This repository provides SPI and implementations to integrate OpenTelemetry with Vert.x-based services.

Project details
- Organization: GuicedEE
- Architecture: Microservices (DDD)
- Primary language: Java 25
- Tech stack: Vert.x 5, MapStruct, Lombok, Logging
- CI: GitHub Actions

Structure of Work (Pact → Rules → Guides → Implementation)
- Pact: docs/PACT.md
- Rules (project-specific): docs/RULES.md
- Guides index: docs/GUIDES.md
- Implementation mapping: docs/IMPLEMENTATION.md

Enterprise rules submodule
- The enterprise Rules Repository is included as a git submodule at: rules/
- Initialize/update submodule locally if needed:
  - git submodule update --init --recursive
- Key topics we reference:
  - Architecture (DDD): rules/generative/architecture/ddd/
  - Vert.x 5: rules/generative/backend/vertx/
  - MapStruct: rules/generative/backend/mapstruct/
  - Lombok: rules/generative/backend/lombok/
  - Logging: rules/generative/backend/logging/
  - Env variables: rules/generative/platform/secrets-config/env-variables.md

Environment configuration
- See .env.example for local settings aligned to the env-variables guide.

CI
- GitHub Actions workflow: .github/workflows/ci.yml (build and test on Java 25)

License
- Apache-2.0 (see LICENSE)


Quick start
- Ensure the rules submodule is initialized: git submodule update --init --recursive
- Build the project: mvn -B -U -DskipTests package
- Run tests: mvn -B -U test
- Telemetry enablement:
  - Configuration is handled via the GuiceTelemetryRegistration SPI. A default no-op implementation is provided.
  - GuicedEE environments can supply their own implementation to configure OpenTelemetry.
  - Configure exporters via environment variables (see .env.example), e.g. OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_SERVICE_NAME.

Example (conceptual)
- During application bootstrap, the environment resolves GuiceTelemetryRegistration via ServiceLoader and calls configure(OpenTelemetry) to finalize the instance for use.

Notes
- No frontend or database modules are included in this repository.
