# Bettamind Locked Implementation Specification

## Product identity

- Product name: Bettamind.
- Canonical logo path: `brand/source/bettamind-logo-master.svg`.
- PNG fallback path: `brand/source/bettamind-logo-master.png`.
- Android application ID and iOS bundle ID remain owner-supplied configuration
  values until a legal namespace or owned domain is confirmed.

## Mobile stack

- Kotlin Multiplatform.
- Compose Multiplatform shared UI.
- Kotlin coroutines and Flow.
- Kotlin serialization.
- Koin dependency injection.
- Unidirectional state management.
- Feature-oriented clean architecture.
- Platform adapters for Android and iOS capabilities.
- Valid minimal `iosApp` Xcode project.

## Optional backend stack

- FastAPI.
- Pydantic.
- SQLAlchemy.
- Alembic.
- PostgreSQL.
- S3-compatible object storage for encrypted packages.
- Docker.
- Pytest, Ruff and mypy.

The backend is optional and must never be required for core app operation.

## Security requirements

Later phases must use:

- SQLCipher-backed SQLite;
- Android Keystore with StrongBox when available;
- iOS Keychain;
- XChaCha20-Poly1305 encrypted exports;
- Argon2id;
- Ed25519 signatures;
- SHA-256 integrity checks.

Never create unencrypted fallback storage.

## AI requirements

- AI is optional, local, replaceable and removable.
- No AI model is required for core operation.
- LiteRT-LM must sit behind a shared replaceable interface.
- Model packs must be signed, checksum-verified, versioned and removable.
- Never commit or automatically download model weights.

## Localisation requirements

- Use BCP 47 locale identifiers.
- Externalise user-facing strings.
- Support RTL layout and locale-aware dates, numbers and plurals.
- English is the source locale.
- Translation drafts are not production-approved until reviewed.
- Safety, crisis, legal, privacy and consent text needs qualified human review.

## Age assurance

Store only:

- `ageBand`: adult, minor or unknown;
- `source`: self-declared, Apple declared range or Google Play signal;
- `confirmedAt`: local timestamp;
- `policyVersion`: integer;
- `signalExpiry`: optional timestamp.

Do not store exact dates of birth or request identity documents in the MVP.

## Third-party support

Sharing must be user-controlled, consent-based and minimum-data. MVP transfer
methods are encrypted export files, operating-system share sheets and QR codes
for key exchange, package fingerprint or professional verification.

## Phase order

1. Phase 0: audit and locked plan.
2. Phase 1: monorepo and cross-platform build foundation.
3. Phase 2: brand, design system, navigation and localisation foundation.
4. Phase 3: encrypted storage and privacy technical spike.
5. Phase 4: deterministic Human Growth application.
6. Phase 5: signed knowledge packs and local retrieval.
7. Phase 6: on-device AI abstraction and model manager.
8. Phase 7: AI-assisted response modes.
9. Phase 8: safety and support bridge.
10. Phase 9: optional backend and encrypted sync.
11. Phase 10: global localisation and accessibility completion.
12. Phase 11: optional offline speech.
13. Phase 12: performance, red-team and release readiness.

Stop after the requested phase. Do not begin the next phase automatically.
