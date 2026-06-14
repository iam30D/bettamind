# Phase 6 AI Model Manager

## Scope

Phase 6 adds optional on-device AI interfaces and a removable model-pack
manager. It does not add response modes, chatbot behaviour, cloud AI, model
downloads, production model weights or production trust roots.

## Implemented controls

- `LocalAiRuntime` remains the replaceable shared runtime interface.
- `LiteRtLmRuntimeAdapter` delegates to a platform-provided `LiteRtLmBridge`
  and adds no LiteRT dependency or model files to the repository.
- `UnavailableLocalAiRuntime` is the default no-model runtime shape, keeping
  core app use independent of AI.
- Model pack manifests require:
  - model ID;
  - monotonic version;
  - runtime ID;
  - artifact file name;
  - artifact size;
  - SHA-256 artifact checksum;
  - declared capabilities;
  - signing key ID;
  - `Ed25519` signature algorithm;
  - non-empty signature.
- `ModelPackManager` verifies the signed manifest before accepting chunks.
- Chunk offsets must match the staged byte count, enabling resumable installs
  without accepting overlapping or out-of-order data.
- Final install requires exact artifact size and matching SHA-256 checksum.
- Rollback and revocation policy reject old versions, revoked model IDs,
  revoked signing keys and versions below the configured minimum.
- Installed model packs are removable through the store boundary.

## Non-goals

- No model weights are committed.
- No automatic model download exists.
- No cloud AI or backend dependency exists.
- No AI-assisted response modes exist.
- No production Ed25519 trust anchors are committed.
