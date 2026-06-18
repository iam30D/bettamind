# Implementation Plan

## Phase 0: Audit and locked plan

Goal: establish the repository contract and phase sequence without production
code.

Acceptance criteria:

- Repository audit, traceability, assumptions, implementation plan and risk
  register exist.
- System context, module boundaries and architecture decisions exist.
- Brand and localisation plans exist.
- Project memory is current.

Status: completed as a prerequisite for Phase 1 because the cloned repository
was empty.

## Phase 1: Monorepo and cross-platform build foundation

Goal: create the production-quality foundation for Android, iOS, shared Kotlin,
optional backend and CI.

Acceptance criteria:

- Kotlin Multiplatform and Compose Multiplatform project exists.
- Gradle wrapper metadata and version catalogue exist.
- Shared module declares Android, `iosX64`, `iosArm64` and
  `iosSimulatorArm64` targets.
- Android app target displays Bettamind and starts without backend.
- iOS target is hosted by a valid minimal `iosApp` Xcode project.
- Codemagic has an unsigned simulator workflow that runs `xcodebuild`.
- Optional FastAPI skeleton exists with health endpoint and test scaffold.
- Windows checks are run where available.
- Project memory is updated.

Out of scope:

- finished UI;
- final brand assets;
- encryption implementation;
- AI implementation or model downloads;
- deterministic product engines;
- signed iOS release.

## Phase 2: Brand, design system, navigation and localisation foundation

Goal: inspect the canonical logo, choose one accessible palette, generate
platform assets, bundle fonts, implement themes, five placeholder destinations,
accessibility foundations and locale packs.

Acceptance criteria:

- Source logo remains unchanged.
- One final accessible palette is documented.
- Android and iOS brand assets are generated from the approved source.
- Noto Sans Variable and Atkinson Hyperlegible are locally bundled with
  licences.
- Five primary destinations exist: Today, Reflect, Grow, Support and Settings.
- RTL is validated with Arabic.

Status: implemented and validated. Windows checks validate shared and Android
code; the owner confirmed Codemagic iOS validation passed.

## Phase 3: Encrypted storage and privacy technical spike

Goal: prove encrypted local storage on Android and iOS.

Acceptance criteria:

- SQLCipher-backed SQLite works on Android and iOS.
- Android Keystore and iOS Keychain adapters exist.
- Wrong-key rejection, key rotation, backup, restore and deletion are tested.
- No unencrypted fallback storage exists.

Status: completed after owner-confirmed Codemagic `ios-simulator-unsigned`
validation. Android SQLCipher storage, Android Keystore wrapping, the shared
encrypted-storage contract, iOS Keychain key management and iOS SQLCipher
storage source exist. The selected iOS route is `SQLCipher.swift` pinned to
`4.16.0`; Xcode links the Swift Package product and Gradle checksum-verifies the
same XCFramework for Kotlin/Native cinterop. The app-hosted Codemagic simulator
validation proves the real iOS Keychain and SQLCipher route. Do not use system
SQLite as a substitute fallback.

## Phase 4: Deterministic Human Growth application

Goal: complete core app flows without AI.

Acceptance criteria:

- Awareness, choice, action, consequence, reflection, repair and growth flows
  exist.
- Core flows work offline and account-free.
- Adult age assurance gates personal features before narrative storage.

Status: narrow implementation completed locally and validated by owner-reported
Codemagic pass. Shared code now includes a
deterministic in-memory growth sequence, adult gate, encrypted-storage
availability status and Compose panels for Today, Reflect, Grow and Support.
Persistent narrative storage remains disabled because iOS SQLCipher storage is
not yet Codemagic-validated; there is no unencrypted fallback. Draft locale
resources were added for the Phase 4 strings and require human review before
production use.

## Phase 5: Signed knowledge packs and local retrieval

Goal: add offline signed content packs and local search.

Acceptance criteria:

- Pack manifests are signed and checksum-verified.
- Rollback and revocation are handled.
- Local retrieval works without network.

Status: implemented as a shared offline pack contract. Common code now requires
Ed25519-labeled signed manifests, SHA-256 payload checksums, injected signature
verification, rollback/replay rejection, revocation policy and in-memory local
retrieval. Production signing keys, approved content packs, backend delivery and
AI/model-pack logic remain out of scope. Owner later confirmed Codemagic
`ios-simulator-unsigned` passed for the pushed Phase 5 commit.

## Phase 6: On-device AI abstraction and model manager

Goal: add optional AI interfaces, capability checks and removable model packs.

Acceptance criteria:

- LiteRT-LM adapter sits behind a replaceable shared interface.
- Model packs are optional, signed, checksum-verified, resumable and removable.
- No model weights are committed.

Status: implemented as a shared optional AI/model-pack foundation. `LocalAiRuntime`
remains the replaceable interface, `LiteRtLmRuntimeAdapter` delegates to a
platform bridge without adding LiteRT or model files, and
`UnavailableLocalAiRuntime` preserves no-AI core operation. `ModelPackManager`
requires signed Ed25519-labeled manifests, SHA-256 artifact checksums,
monotonic versions, revocation policy, resumable chunk offsets and removable
installed packs. No model weights, downloads, cloud AI or Phase 7 response modes
were added.

## Phase 6.7: Harmful intent and dangerous capability safeguards

Goal: add deterministic safety boundaries for harmful intent, dangerous
capability and unsafe generated output before any AI-assisted response modes.

Acceptance criteria:

- Risk levels and categories for harmful intent and dangerous capability exist.
- Pre-generation safeguards decide whether normal generation is allowed.
- Post-generation safeguards reject actionable harmful generated output.
- Deterministic fallback works without AI, classifier, valid JSON or network.
- Memory, export, sync, support summary and notification defaults protect
  harmful-intent content and require step-up authentication where needed.
- Daily tools and relational boundaries integrate with harm safety.

Status: implemented as a shared deterministic foundation. No finished AI
response modes, backend product engine, emergency-service automation, cloud AI,
unencrypted storage or persistent harmful narrative storage were added.

## Phase 7: AI-assisted response modes

Goal: add optional AI-assisted modes after deterministic flows are stable.

Acceptance criteria:

- Quick Guidance, Guided Reflection, Deep Exploration and Action-Only modes
  exist.
- Deterministic fallback remains available.
- Safety does not rely solely on AI.

## Phase 8: Safety and support bridge

Goal: implement layered safety and consent-based encrypted third-party support.

Acceptance criteria:

- Risk levels are deterministic and tested.
- Export preview shows included and excluded data.
- Sharing is user-controlled and minimum-data.

## Phase 9: Optional backend and encrypted sync

Goal: add ciphertext-only sync and signed pack delivery.

Acceptance criteria:

- Backend never receives plaintext journal content.
- Sync is off by default and optional.
- Revocation records and manifests are versioned.

## Phase 10: Global localisation and accessibility completion

Goal: complete target locale packs and accessibility validation.

Acceptance criteria:

- Target locales are complete and reviewed where required.
- RTL, large text, screen readers and script fallback are tested.
- Low-literacy mode is validated.

## Phase 11: Optional offline speech

Goal: add optional offline speech-to-text and text-to-speech packs.

Acceptance criteria:

- Speech packs are optional, licensed, removable and locally validated.
- OS offline voices are preferred before local voice packs.

## Phase 12: Performance, red-team and release readiness

Goal: harden for real release.

Acceptance criteria:

- Low-resource devices, battery, thermal and memory behaviour are tested.
- Threat model and red-team findings are resolved or accepted.
- Store metadata, rollback and TestFlight gates are complete.
