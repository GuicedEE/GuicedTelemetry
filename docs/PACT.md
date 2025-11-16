# PACT — Guiced Telemetry

- Organization: GuicedEE
- Project: Guiced Telemetry
- Short description: The GuicedEE Plugin for Vert.x Telemetry
- Repository: GuicedEE/GuicedTelemetry
- License: Apache-2.0
- Primary language: Java 25
- Architecture: Microservices
- Tech stack (selected): Vert.x 5, DDD, MapStruct, Lombok, Logging, GitHub Actions
- Authors: GedMarc, AI
- Date: 2025-11-03

## Purpose
Deliver a GuicedEE plugin that integrates OpenTelemetry for Vert.x-based services in the GuicedEE ecosystem.

## Scope
- Provide SPI and implementations for telemetry integration.
- Offer reference configuration and guidelines aligned with the Enterprise Rules Repository.

## Rules Linkage
- Enterprise rules are tracked via the submodule at `../rules/`.
- Behavioral Agreements: ./RULES.md#4-behavioral-agreements
- Technical Commitments: ./RULES.md#5-technical-commitments
- Document Modularity Policy: ./RULES.md#document-modularity-policy
- Forward-Only Change Policy: ./RULES.md#6-forward-only-change-policy

## Guides Linkage
- See GUIDES.md for topic indices:
  - Architecture (DDD): rules/generative/architecture/ddd/
  - Backend: Vert.x 5 — rules/generative/backend/vertx/
  - Backend: MapStruct — rules/generative/backend/mapstruct/
  - Backend: Lombok — rules/generative/backend/lombok/
  - Backend: Logging — rules/generative/backend/logging/

## Implementation Linkage
- See IMPLEMENTATION.md for the module structure and how code maps back to the selected guides.

## Non-Goals
- UI/frontend scaffolding (not in scope for this repository).
- Database selections (none defined for this project).

## Success Criteria
- Clear documentation that closes loops: PACT ↔ RULES ↔ GUIDES ↔ IMPLEMENTATION.
- A minimal CI pipeline and environment example provided.
