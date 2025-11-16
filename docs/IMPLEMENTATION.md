# IMPLEMENTATION — Guiced Telemetry

- Parent: GUIDES — ./GUIDES.md
- Parent: RULES — ./RULES.md
- Sibling: PACT — ./PACT.md

This document maps code modules and packages in this repository to the selected guides and rules. Maintain this mapping as the code evolves (forward-only).

## Repository Layout
- docs/ — Pact, Rules, Guides, Implementation (you are here)
- rules/ — Enterprise Rules Repository (git submodule)
- src/main/java — Source
  - com.guicedee.telemetry — Root package
  - com.guicedee.telemetry.spi — SPIs and registration points
  - com.guicedee.telemetry.implementations — Implementations
- src/test/java — Tests

## Initial Modules and Mapping
- Telemetry SPI (com.guicedee.telemetry.spi)
  - Rules/Guides linkage:
    - Vert.x 5: ../rules/generative/backend/vertx/
    - Logging: ../rules/generative/backend/logging/
    - DDD: ../rules/generative/architecture/ddd/
- Implementations (com.guicedee.telemetry.implementations)
  - Rules/Guides linkage:
    - Lombok: ../rules/generative/backend/lombok/
    - MapStruct: ../rules/generative/backend/mapstruct/

## Configuration
- Environment variables follow: ../rules/generative/platform/secrets-config/env-variables.md
- Local sample: see project root .env.example

## CI/CD
- Minimal GitHub Actions workflow provided at .github/workflows/ci.yml
- Patterns reference: ../rules/generative/platform/ci-cd/

## Future Work
- Expand tests with guidance from rules/generative/backend/vertx testing and threading sections.
- Add examples of span/metric emission aligned to OpenTelemetry specs.

Keep this document synchronized with changes to code and guides to close the loop PACT ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION.
