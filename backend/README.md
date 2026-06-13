# Bettamind Optional Backend

This FastAPI backend is optional. Bettamind mobile startup and core offline use
must not depend on it.

Phase 1 provides only a skeleton:

- health endpoint;
- settings object;
- database session placeholder;
- Alembic scaffold;
- tests and lint/type tooling declarations.

Encrypted sync, signed pack delivery and production storage are deferred to
later approved phases.
