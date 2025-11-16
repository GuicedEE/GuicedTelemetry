# Project RULES — Guiced Telemetry

This document extends the Enterprise Rules Repository included as a submodule in `rules/`. It selects relevant topics and records project-specific scope/overrides. Follow the Document Modularity Policy: do not place project docs inside the submodule.

- Parent Rules (submodule): ../rules/
  - Behavioral Agreements: ../rules/RULES.md#4-behavioral-agreements
  - Technical Commitments: ../rules/RULES.md#5-technical-commitments
  - Document Modularity Policy: ../rules/RULES.md#document-modularity-policy
  - Forward-Only Change Policy: ../rules/RULES.md#6-forward-only-change-policy

## Project Scope
- Purpose: OpenTelemetry integration plugin for GuicedEE services (Vert.x-based).
- Architecture: Microservices aligned to DDD tactical patterns.
- Languages/Runtime: Java 25 (source/target aligned per build tooling).
- Non-goals: Frontend/UI and database-specific guidance.

## Chosen Stacks and Topics
- Architecture
  - Domain-Driven Design (DDD): rules/generative/architecture/ddd/
- Backend
  - Vert.x 5: rules/generative/backend/vertx/
  - MapStruct: rules/generative/backend/mapstruct/
  - Lombok: rules/generative/backend/lombok/
  - Logging: rules/generative/backend/logging/
- Platform
  - Secrets & Config — Environment Variables: rules/generative/platform/secrets-config/env-variables.md
  - CI/CD: rules/generative/platform/ci-cd/

## Project Overrides/Decisions
- Logging: prefer structured logging (JSON where supported) and MDC-compatible correlation IDs.
- Telemetry: adhere to OpenTelemetry conventions for spans/metrics; expose configuration via environment variables.
- Build/CI: minimal GitHub Actions pipeline maintained in this repo.

## Cross-Links
- Pact: ./PACT.md
- Guides index: ./GUIDES.md
- Implementation notes: ./IMPLEMENTATION.md

Adopt forward-only edits. Any change to this document must update all cross-references (PACT/GUIDES/IMPLEMENTATION) accordingly.
