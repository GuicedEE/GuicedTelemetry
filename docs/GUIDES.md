# GUIDES — Guiced Telemetry

This index links to the selected topics from the Enterprise Rules Repository (submodule at `rules/`) and local implementation notes. Follow links for deeper guidance.

- Parent: Project RULES — ./RULES.md
- Child: IMPLEMENTATION — ./IMPLEMENTATION.md

## Architecture
- Domain-Driven Design (DDD): ../rules/generative/architecture/ddd/

## Backend
- Vert.x 5: ../rules/generative/backend/vertx/
- MapStruct: ../rules/generative/backend/mapstruct/
- Lombok: ../rules/generative/backend/lombok/
- Logging: ../rules/generative/backend/logging/

## Platform & Operations
- Environment Variables & Secrets: ../rules/generative/platform/secrets-config/env-variables.md
- CI/CD (patterns): ../rules/generative/platform/ci-cd/

## How to Use These Guides
1. Start with PACT (./PACT.md) to understand scope and intent.
2. Review RULES (./RULES.md) for commitments and selected topics.
3. Use these guide links while developing features; cite specific rules/sections in PRs.
4. Keep IMPLEMENTATION.md updated with mappings from code to relevant guide sections (close the loop).
