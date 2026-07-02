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
Persistent narrative storage remains disabled until a separate approved pass
wires it through encrypted storage; there is no unencrypted fallback. Draft
locale resources were added for the Phase 4 strings and require human review
before production use.

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
remains the replaceable interface and `UnavailableLocalAiRuntime` preserves
no-AI core operation. `ModelPackManager` requires signed Ed25519-labeled
manifests, SHA-256 artifact checksums, monotonic versions, revocation policy,
resumable chunk offsets and removable installed packs. Later runtime work wires
Android to Google's `litertlm-android` `0.13.1` dependency and iOS to the
official `LiteRTLM` Swift Package `0.13.1` through a native Swift bridge. Model
installation still requires explicit user file selection and signature/checksum
verification; no model weights, automatic downloads or cloud AI are committed.
The iOS bridge requires Codemagic/Xcode validation because Windows cannot build
the Swift project path.

## Phase 6.4: App privacy lock

Goal: gate private local content and encrypted storage key release behind local
authentication.

Acceptance criteria:

- Shared privacy-lock policy and lock timeout settings exist.
- Local authentication succeeds before storage-key release.
- Android and iOS platform authentication adapters exist.
- Step-up authentication is required for sensitive actions.
- Background/app-switcher privacy protection exists.
- PIN/passphrase policy and rate limiting exist behind an approved KDF
  boundary.

Status: implemented. Owner confirmed Codemagic `ios-simulator-unsigned` passed
after the iOS LocalAuthentication fix. Production PIN/passphrase enablement
still requires an audited Argon2id provider.

## Phase 6.5: Relational boundaries

Goal: add deterministic relational-boundary enforcement before AI response
modes.

Acceptance criteria:

- Deterministic relational risk levels and signals exist.
- Future AI modes have pre-generation and post-generation boundary contracts.
- Romantic, sexual, possessive or dependency-building Bettamind persona output
  is blocked or redirected.
- Ordinary human relationship, dating, consent, sexuality, loneliness and
  boundary discussion remains allowed.
- Memory, export, sync, notification, voice and avatar policy decisions exist.

Status: implemented. Owner confirmed Codemagic `ios-simulator-unsigned` passed
for the Phase 6.5 commit. Category and fallback copy still require production
review before Phase 7 relies on them broadly.

## Phase 6.6: Deterministic daily tools

Goal: build the non-AI daily-use core with encrypted local records.

Acceptance criteria:

- Daily check-ins, breathing, grounding, timers, reminders, private calendar,
  local trends and deterministic worksheets exist.
- Daily records use encrypted local storage only.
- Reminders are local, optional and neutral.
- System-calendar handoff requires explicit approval and does not read broad
  calendar data.
- No public feed, ranking, manipulative streak or human-worth score exists.

Status: implemented. Owner confirmed Codemagic `ios-simulator-unsigned` passed
for the Phase 6.6 commit. Platform reminder scheduling and production calendar
handoff UI remain later integration work.

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

## Roadmap reconciliation after Phase 6X

Goal: preserve the original Phase 7 through Phase 12 objectives while adding
entry conditions from Phase 6.4, Phase 6.5, Phase 6.6 and Phase 6.7.

Acceptance criteria:

- Previous plan archive remains unchanged.
- Phase 7 through Phase 12 original objectives remain visible.
- App privacy lock, relational boundaries, deterministic daily tools and
  harm-safety safeguards are integrated into the continuation criteria.
- `docs/planning/phase-7-to-12-continuation-plan.md` records the controlled
  continuation plan.
- Roadmap amendment, traceability, risk register and project memory are
  updated.
- No production code is edited.

Status: completed as a planning-only pass after owner-confirmed Codemagic
`ios-simulator-unsigned` pass for Phase 6.7.

## Phase 7: AI-assisted response modes

Goal: add optional AI-assisted modes after deterministic flows are stable.

Acceptance criteria:

- Quick Guidance, Guided Reflection, Deep Exploration and Action-Only modes
  exist.
- AI remains optional, local, replaceable and removable.
- No-model deterministic fallback remains complete for every mode.
- No cloud AI is used for core functionality.
- App-lock step-up applies to sensitive AI context and generated summaries.
- Check-in, worksheet, timer, calendar, local trend and practice context is
  used only with explicit user consent.
- Relational-boundary metadata is enforced before generation, after generation
  and before display, storage, export, sync, notification, voice or avatar use.
- Harmful-intent and dangerous-capability safeguards are enforced before
  generation, after generation and before all privacy surfaces.
- AI requests use pre-generation safety classification and generated output
  uses post-generation validation.
- Responses use a structured schema with safety metadata, memory eligibility,
  export eligibility and fallback identifiers.
- Safe refusal and safe redirection templates are nonjudgmental.
- Golden and adversarial tests cover response modes, no-model fallback,
  relational risk, harmful intent, dangerous capability, malformed model output
  and jailbreak attempts.

Status: implemented as a shared optional local AI growth-mode foundation.
`AiGrowthModeEngine` adds Quick Guidance, Guided Reflection, Deep Exploration
and Action-Only orchestration behind `LocalAiRuntime`. The no-model path is
complete through deterministic fallback localization keys. Daily-tool context
is included in model prompts only when explicitly requested and consented.
Relational and harm-safety policies run before generation and after generated
output before display, memory, export, sync, notification, voice or avatar
eligibility decisions. Responses use a structured JSON schema with safety
metadata, memory/export eligibility and fallback identifiers. Permanent memory
is proposal-only, disabled for automatic writes and requires separate approval.
No cloud AI, model downloads, model weights, backend dependency, speech,
support bridge or sync implementation was added. The release-candidate model
policy now records Qwen2.5 1.5B Instruct as the first optional LiteRT-LM pack
to prove the signed `.litertlm` pipeline, while Gemma 4 E2B remains cataloged
as a later candidate after storage, thermal and memory testing. Explicit user
approval plus owner licence acceptance is required before any signed model
pack is distributed or installed.

## Phase 7.5: Compassionate safety redirection and better-human pathways

Goal: improve safety-response usefulness and dignity without weakening Phase
6.5 relational boundaries, Phase 6.7 harmful-intent safeguards or Phase 7 AI
response-mode gates.

Acceptance criteria:

- Safety-redirection domain models exist for mode, reason, pathway, response
  and decision metadata.
- Better-human pathways cover grounding, breathing, delay action, leaving the
  situation, contact support, emergency help, conflict reflection, repair
  planning, values to action, difficult conversation, consent and boundaries,
  self compassion and no follow-up needed.
- Deterministic safety responses use acknowledgement, boundary,
  human-growth redirect, practical next step and privacy metadata.
- AI pre-generation classification exposes safety boundary, intent confidence,
  allowed discussion scope, better-human pathway and recommended
  deterministic tool.
- AI post-generation validation rejects shaming, diagnosis, bad-intent
  assumptions, dependency-building output, unsafe instructions and missing
  safe next steps when a boundary is applied.
- Unsafe reminders are refused with neutral safe replacement reminders.
- Memory/export defaults keep safety-sensitive narrative ineligible and
  require preview plus local step-up authentication before selected sensitive
  export.
- English source strings exist and all target locale entries are present as
  draft fallbacks pending qualified human review.

Status: implemented as a deterministic shared foundation. The new
`CompassionateSafetyRedirectionPolicy` composes existing harm and relational
assessments into dignity-preserving fallback keys, better-human pathways,
recommended deterministic tools, privacy/export/app-lock metadata and unsafe
reminder replacements. `AiGrowthModeEngine` now includes the Phase 7.5
metadata in structured responses and blocks generated output that shames,
diagnoses, assumes guilt, encourages dependency or omits a safe next step after
a boundary. No Phase 8 support bridge, automatic third-party contact, backend,
cloud AI, model download, speech, sync or persistent safety narrative storage
was added.

## Phase 8: Safety and support bridge

Goal: implement layered safety and consent-based encrypted third-party support.

Acceptance criteria:

- Risk levels are deterministic and tested.
- Export preview shows included and excluded data.
- Sharing is user-controlled and minimum-data.
- Self-harm, suicide, violence intent and dangerous capability handling reuses
  Phase 6.7 harm-safety policy.
- Relational-risk integration reuses Phase 6.5 policy.
- Daily tools can offer check-ins, grounding, breathing, delay, reflection,
  repair and nonviolent-choice support.
- Support actions are voluntary only; Bettamind never automatically contacts a
  third party.
- Step-up authentication is required before sensitive sharing.
- Local emergency/support resources can be displayed without revealing
  personal data.
- Crisis or harmful narrative is not stored by default.
- Safety-critical translations require qualified human review.

Status: implemented as a shared deterministic foundation. The new
`SafetySupportBridgePolicy` composes harm-safety, relational-boundary,
compassionate-redirection and daily-tool decisions into support risk levels,
voluntary support actions, local resource metadata, minimum-detail summaries
and explicit preview plus app-lock step-up metadata before sensitive sharing.
Bettamind still does not automatically contact anyone, does not claim help was
contacted, does not store crisis or harmful narrative by default and does not
add backend sync, cloud AI, model downloads, speech or production-approved
translations.

## Phase 9: Optional backend and encrypted sync

Goal: add ciphertext-only sync and signed pack delivery.

Acceptance criteria:

- Backend never receives plaintext journal content.
- Sync is off by default and optional.
- Revocation records and manifests are versioned.
- App-lock reauthentication is required before enabling sync or export.
- Daily-tool export and sync rules are enforced.
- Relationally sensitive and harmful-intent content is excluded by default.
- Export inclusion requires explicit selection and preview where sensitive.
- Conflict handling is deterministic and avoids silent local data loss.
- Device revocation is explicit and tested.
- Encrypted backup and restore are supported.
- Optional system-calendar handoff remains explicit and does not read broad
  calendar data.

Status: implemented as a shared deterministic export/sync policy foundation and
optional ciphertext-only FastAPI contract. `BettamindExportSyncPolicy` keeps
sync disabled by default, requires encrypted packages/envelopes, explicit
approval, preview where sensitive and app-lock step-up before export or sync.
Daily-tool, relationally sensitive, harm-safety and support-summary content is
excluded by default. Conflict handling keeps divergent encrypted versions for
review instead of silently overwriting local data, device revocation is explicit
and versioned, encrypted backup/restore contracts are present and calendar
handoff remains local without broad calendar reads. This phase does not add
automatic sync, mandatory accounts, production backend persistence, secrets,
model artifacts, cloud AI, speech or Phase 10 production localization.

## Phase 10: Global localisation and accessibility completion

Goal: complete target locale packs and accessibility validation.

Acceptance criteria:

- Target locales are complete and reviewed where required.
- RTL, large text, screen readers and script fallback are tested.
- Low-literacy mode is validated.
- App-lock, relational-boundary, harmful-intent, crisis, emergency, legal,
  consent, daily-tool, reminder, calendar, local trend, worksheet, timer and
  support-bridge copy are covered.
- Reduced motion is supported for timers, transitions and grounding exercises.
- Human review is tracked for safety-critical translations before production.

Status: implemented as a shared localisation and accessibility foundation.
`BettamindLocaleAccessibilityCatalog`, `LocalizationReadinessPolicy`,
`LocaleFormattingPolicy` and `AccessibilityReadinessPolicy` cover target locale
profiles, RTL/script/font fallback metadata, locale-aware date/number/plural
metadata, resource completeness, qualified human-review gates for
safety-critical strings, screen-reader label requirements, large-text support,
reduced-motion static presentation and low-literacy mode. Settings exposes
accessible typography, reduced motion and simple wording controls. Target
locale resource keys are complete as draft fallbacks, but safety, crisis,
legal, privacy and consent translations remain blocked from production until
qualified human review records are supplied. This phase does not add speech,
release readiness, TestFlight, store metadata, cloud AI, model artifacts or
production-approved non-English safety copy.

## Phase 11: Optional offline speech

Goal: add optional offline speech-to-text and text-to-speech packs.

Acceptance criteria:

- Speech packs are optional, licensed, removable and locally validated.
- OS offline voices are preferred before local voice packs.
- Text-only app remains complete.
- Microphone use is optional and permission-scoped.
- Raw audio is not retained by default.
- App-lock behavior protects sensitive speech transcripts and summaries.
- Voice input uses the same safety pipeline as text.
- Voice output cannot sound romantic, seductive, possessive or manipulative.
- Harmful-intent safeguards apply to spoken input and generated spoken output.
- Accessibility fallback and offline operation are preserved.

Status: implemented as a shared optional offline speech foundation.
`OfflineSpeechPolicy` keeps text-only fallback complete, represents microphone
use as explicit permission-scoped state, forbids raw-audio retention by
default, requires app-lock metadata for sensitive transcripts, routes spoken
input through existing relational-boundary and harm-safety decisions and blocks
spoken output that is romantic, seductive, possessive, manipulative or harmful.
`SpeechPackManager` requires user approval, publisher licence approval,
approved licence identifiers, Ed25519-labeled signatures, SHA-256 checksums,
monotonic versions, revocation handling and removability for any local speech
pack. Settings exposes the offline speech foundation copy. This phase does not
add platform microphone integration, bundled speech packs, cloud speech,
automatic downloads, voice/model artifacts, TestFlight, store metadata,
performance hardening or Phase 12 release readiness.

## Phase 12: Performance, red-team and release readiness

Goal: harden for real release.

Acceptance criteria:

- Low-resource devices, battery, thermal and memory behaviour are tested.
- Threat model and red-team findings are resolved or accepted.
- Store metadata, rollback and TestFlight gates are complete.
- App-lock bypass attempts and encryption-key protection are tested.
- Romantic attachment, sexualisation, harmful capability, self-harm, violence
  and jailbreak red-team cases are resolved or accepted.
- Reminder, notification, timer lifecycle, background behavior, calendar,
  export and sync privacy are tested.
- Android physical-device testing, Codemagic/Xcode validation and TestFlight
  testing are complete.
- Store-readiness review covers privacy labels, screenshots, metadata, support
  claims and safety disclaimers.

Status: implemented as a repository-side release-readiness foundation.
`ReleaseReadinessPolicy` and `ReleaseRedTeamSuite` cover app-lock bypass,
encryption-key protection, relational, sexualization, harmful-capability,
self-harm, violence, jailbreak, reminder, notification, timer, background,
calendar, export, sync, speech, localization, performance, store, TestFlight,
rollback and artifact-policy gates. The repo-side report intentionally remains
not production-ready until owner evidence is recorded for low-resource
physical-device testing, battery/thermal/memory behavior, Android devices,
Codemagic iOS validation for the pushed Phase 12 commit, TestFlight,
store metadata/privacy labels/screenshots/support claims, qualified
translation review and rollback. Later code-side production-readiness work
made the shared Compose surface more release-candidate-like by rendering the
Bettamind brand mark, removing production-facing scaffold copy, wiring Today
check-ins through Android/iOS SQLCipher-backed app services after adult
confirmation, exposing platform integration states in Settings, adding a
concern prompt backed by deterministic no-model AI growth fallbacks and adding
deterministic support assessment. Qwen2.5 1.5B Instruct is now the first
model-pack target, with an owner-supplied LiteRT-LM release-candidate
artifact, app-compatible signed manifest, public trust anchor and owner
evidence templates in place. This does not complete owner-controlled
production gates for platform LiteRT-LM runtime validation, Android/iOS device
evidence, rollback/revocation review, screenshots or store records. A later
usability pass adds keyboard Send handling for Grow and Support prompts,
loading/result states, safe routing from support recommendations to local Today
tools, honest signed-pack model status and branded Android/iOS launch screens.
A later runtime pass implements Android and iOS LiteRT-LM install/load/
generate/remove bridges for the signed Qwen pack without committing weights or
adding automatic downloads. A later Codemagic iOS simulator run failed during
Swift Package resolution because the upstream LiteRT-LM checkout tried to
smudge an unrelated missing Git LFS Android prebuilt; the iOS workflows now
skip LFS smudge for Xcode package/build steps while still using the pinned
release-hosted iOS binary target. Production release still remains blocked
until iOS Codemagic/Xcode validation, Android/iOS device model-smoke evidence,
low-storage/interrupted-import behavior, battery/thermal/memory observations,
rollback/revocation review, screenshots and store records are complete.
