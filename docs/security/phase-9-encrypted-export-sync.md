# Phase 9 Encrypted Export And Sync

Date: 2026-06-20

## Scope

Phase 9 adds shared export, backup, device-management and ciphertext-only sync
contracts plus a narrow optional FastAPI backend endpoint. It does not make the
backend mandatory, enable sync by default, add cloud AI, commit secrets or
store model artifacts.

The shared implementation lives in
`shared/src/commonMain/kotlin/org/bettamind/shared/sync/EncryptedExportSync.kt`.

## Implemented Controls

- Core mobile use remains backend-independent and account-free.
- `OptionalBackendSyncSettings` defaults to no configured backend and disabled
  sync.
- Export and sync decisions require encrypted packages or encrypted envelopes.
- Sync envelopes declare `XChaCha20-Poly1305`, schema version, manifest
  version, key version, nonce, ciphertext and SHA-256 ciphertext checksum.
- Backend sync accepts only ciphertext-envelope fields and rejects plaintext
  fields through strict Pydantic schema validation.
- App-lock step-up is required for export and sync decisions through
  `SensitiveAction.ExportPrivateInformation` and `SensitiveAction.EnableSync`.
- Daily-tool records are excluded from export and sync by default.
- Relationally sensitive records are excluded by default.
- Harm-safety records and support summaries are excluded by default.
- Sensitive export requires explicit selection and preview acceptance.
- Actionable harmful details are rejected from export and sync decisions.
- Conflict resolution keeps both divergent encrypted versions and requires
  review instead of silently overwriting local data.
- Device revocation requires explicit user action plus step-up authentication
  and produces versioned revocation records.
- Encrypted backup export, restore and sync-envelope conversion are supported
  as contracts around `EncryptedBackupPackage`.
- System-calendar handoff remains local and explicit and must not read broad
  calendar data.

## Backend Contract

The optional backend route is `POST /sync/envelopes`. It validates envelope
shape, declared algorithm, base64 nonce/ciphertext and SHA-256 ciphertext
checksum, then returns `202 Accepted`.

The backend does not receive or store plaintext personal content in this phase.
The route is a contract foundation only; durable server storage, user accounts,
device key exchange, retention policy and deployment configuration remain later
work.

## Non-Goals

- No automatic sync.
- No mandatory account or backend.
- No plaintext journal upload.
- No production server persistence.
- No production key exchange.
- No secrets, certificates, signing files, model weights or databases committed
  to Git.
- No Phase 10 production localization or accessibility completion.

## Verification Coverage

Shared common tests in
`shared/src/commonTest/kotlin/org/bettamind/shared/sync/EncryptedExportSyncTest.kt`
cover disabled-by-default sync, step-up gates, daily/relational/harm default
exclusions, ciphertext-only backend contract helpers, optional encrypted daily
sync inclusion, deterministic conflict resolution, device revocation,
encrypted backup/restore and calendar handoff privacy.

Backend tests in `backend/tests/test_sync.py` cover accepting valid encrypted
envelopes, rejecting plaintext fields and rejecting checksum mismatches.

Non-English Compose resource entries added in Phase 9 are draft fallback text
and require qualified human review before production use.
