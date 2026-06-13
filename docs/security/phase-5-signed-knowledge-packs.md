# Phase 5 Signed Knowledge Packs

## Scope

Phase 5 adds the local contract for offline knowledge packs. It does not add
cloud delivery, backend sync, AI model packs, production content packs or
release trust roots.

## Implemented controls

- Knowledge pack manifests require:
  - pack ID;
  - monotonic version;
  - BCP 47 locale tag;
  - SHA-256 payload checksum;
  - signing key ID;
  - `Ed25519` signature algorithm;
  - non-empty signature.
- The installer canonicalises the payload with Kotlin serialization and checks
  the manifest SHA-256 checksum before accepting a pack.
- The installer verifies the signed manifest bytes through
  `KnowledgePackSignatureVerifier`.
- Rollback and replay protection rejects pack versions that are not newer than
  the installed version.
- Revocation policy rejects revoked pack IDs, revoked signing keys and versions
  below a configured minimum accepted version.
- Local retrieval indexes installed packs in memory and searches without
  network access.

## Trust boundary

The shared code enforces the Ed25519 verifier boundary, but does not commit
production signing keys or production content. Release work must provide a
reviewed Ed25519 verifier implementation and owner-approved trust anchors before
accepting real public packs.

## Non-goals

- No backend delivery endpoint.
- No persistent pack database.
- No AI model manager or model packs.
- No automatic downloads.
- No unverified or unsigned fallback packs.
