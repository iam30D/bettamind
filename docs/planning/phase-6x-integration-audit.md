# Phase 6X Integration Audit

Date: 2026-06-18

## Purpose

This audit reconciles the completed Phase 0 through Phase 6 foundations with
the proposed intermediate Phase 6.4, Phase 6.5 and Phase 6.6 work. It is a
planning pass only. It does not authorize production source changes, does not
replace the locked implementation plan and does not begin Phase 7.

The current `docs/planning/implementation-plan.md` was preserved unchanged at
`docs/planning/archive/implementation-plan-before-phase-6x.md`.

Post-implementation note: Phase 6.4, Phase 6.5 and Phase 6.6 foundations have
since been implemented. This audit remains the historical gap analysis that
motivated those phases.

## Sources Reviewed

- Product and locked specification docs.
- Existing planning, risk and project-memory docs.
- Architecture and security docs.
- Shared Kotlin source under `shared/src/commonMain`, `shared/src/androidMain`,
  `shared/src/iosMain` and tests.
- Compose source string resources.
- Android and iOS host app entry points, manifests, entitlements and storage
  validation files.
- Backend skeleton and CI/Codemagic workflows.

## Existing Functions Discovered

| Area | Existing implementation | Notes |
| --- | --- | --- |
| Mobile foundation | Kotlin Multiplatform, Compose Multiplatform, Android app and iOS Xcode host project | Phase 1 foundation exists and has Windows plus Codemagic validation history. |
| Brand and localization foundation | Generated brand assets, Compose resource string folders, RTL locale detection tests, bundled fonts | Locale packs remain draft and require human review before production. |
| Encrypted storage | Shared encrypted-storage contracts, Android SQLCipher plus Keystore key wrapping, iOS SQLCipher plus Keychain key management, app-hosted iOS validation path | Storage is fail-closed and has no unencrypted fallback. Existing platform keys are not currently bound to user authentication. |
| Deterministic growth flow | Fixed sequence of Awareness, Choice, Action, Consequence, Reflection, Repair and Growth | In-memory product flow only; it is not a complete daily-tools system. |
| Adult gate | Session-local self-declared adult confirmation | No date of birth or identity document is requested. |
| Signed knowledge packs | Offline signed manifest verification, SHA-256 checksum checks, rollback/replay rejection and revocation boundary | Production content packs and trust anchors are not committed. |
| Optional AI foundation | Replaceable `LocalAiRuntime`, unavailable runtime, LiteRT bridge boundary and signed model-pack manager | No model weights, downloads, cloud AI or Phase 7 response modes exist. |
| Support boundary copy | Static Compose string states Bettamind does not contact anyone automatically | This is product copy only, not a relational-boundary engine or classifier. |
| Optional backend | FastAPI health skeleton | Mobile core is not dependent on backend. |

## Missing Functions

| Required capability | Current status | Proposed phase |
| --- | --- | --- |
| App privacy lock | Missing as a user-authenticated app feature | Phase 6.4 |
| Android BiometricPrompt and device credential flow | Missing | Phase 6.4 |
| iOS LocalAuthentication flow | Missing | Phase 6.4 |
| Bettamind PIN fallback with secure KDF/storage | Missing | Phase 6.4, if owner approves PIN fallback |
| User-authentication-bound key release for vault access | Missing | Phase 6.4 |
| Auto-lock on lifecycle, timeout, backgrounding and sensitive operations | Missing | Phase 6.4 |
| Background/app-switcher privacy protection | Missing | Phase 6.4 |
| Failed-attempt throttling and local reset/recovery policy | Missing | Phase 6.4 |
| Relational boundary classifier and policy engine | Missing | Phase 6.5 |
| AI system-response contracts for boundary-sensitive content | Missing | Phase 6.5 |
| Post-generation validation and deterministic fallback responses | Missing | Phase 6.5 |
| Memory, export, sync, notification, voice and avatar boundary rules | Missing | Phase 6.5 |
| Deterministic check-ins, mood/energy/stress/sleep tools | Missing | Phase 6.6 |
| Timers, grounding, breathing and background recovery | Missing | Phase 6.6 |
| Local reminders, quiet hours, snooze and pause-all | Missing | Phase 6.6 |
| Private in-app calendar and optional system-calendar handoff | Missing | Phase 6.6 |
| Local trend summaries and decision worksheets | Missing | Phase 6.6 |
| Encrypted export/restore for daily-tool records | Partially covered by encrypted storage primitive, not wired to product records | Phase 6.6 |

## Phase 6.4 Assessment: App Privacy Lock

Phase 6.4 is required and should be a full focused implementation slice. The
existing storage layer protects records at rest, but there is no app-level
privacy lock and no user-authenticated release of the storage key. A visual
lock screen alone would not satisfy the privacy requirement because an already
released database key could still be available to the app process.

Existing helpful pieces:

- Shared encrypted-storage interfaces already centralize record-store access.
- Android SQLCipher storage uses Android Keystore key wrapping.
- iOS SQLCipher storage uses Keychain key management.
- Android backup and data extraction exclusions are already present.
- iOS app-hosted encrypted-storage validation exists through Codemagic.
- Growth UI already has an adult gate and storage-readiness state that can be
  separated from privacy-lock state.

Missing pieces:

- Android `BiometricPrompt` integration and device credential fallback.
- iOS `LocalAuthentication` integration.
- Bettamind PIN fallback policy, KDF and encrypted storage, if approved.
- Authentication-bound release or unwrap of the SQLCipher database key.
- Lifecycle lock state, idle timeout, background lock and reauthentication for
  sensitive operations.
- Android app-switcher and screenshot privacy handling such as `FLAG_SECURE`,
  if accepted by owner and accessibility review.
- iOS background snapshot privacy handling.
- Failed-attempt rate limiting and local reset semantics.
- Recovery copy that does not imply account recovery or cloud recovery.
- Lock-state tests and platform validation.

Architecture recommendation:

- Add a shared privacy-lock domain contract that separates lock policy,
  authentication result, key-release eligibility and local reset.
- Keep platform authentication in Android and iOS adapters.
- Bind encrypted storage key use to successful platform authentication where
  the platform allows it.
- Do not create an unencrypted fallback path for content or keys.
- Treat any PIN fallback as another secret used to unwrap/release local key
  material, not as plain app state.

## Phase 6.5 Assessment: Relational Boundaries

Phase 6.5 is required before Phase 7 AI response modes. Current product docs
state that Bettamind is not therapy, diagnosis, an emergency service or a
human substitute. Current code does not yet enforce those rules for generated
responses, memory, notifications, voice or future avatars.

Existing helpful pieces:

- Product and specification docs include strong non-goals.
- AI runtime is optional and replaceable.
- No cloud AI or model execution is wired into core flows.
- Deterministic growth flow can remain useful without AI.
- Support copy already avoids claiming automatic contact.

Missing pieces:

- Relational-boundary categories and examples.
- A deterministic pre-response classifier/policy boundary.
- AI system prompts and structured response contracts.
- Post-generation validation before display, storage, export or notification.
- Deterministic fallback responses when content is disallowed.
- Rules for memory proposals, permanent memory, export, sync, notifications,
  voice and avatars.
- Tests for false positives and false negatives, including ordinary allowed
  discussion of love, dating, consent, loneliness and relationships.
- Qualified localization review path for sensitive boundary copy.

Architecture recommendation:

- Add a shared safety or boundary module before Phase 7.
- Keep classification deterministic and testable.
- Make future AI modes call the boundary service before and after generation.
- Store no boundary-sensitive memory without explicit consent.
- Block dependency-building, romantic or sexualized AI-persona behavior while
  preserving ordinary human relationship support.

## Phase 6.6 Assessment: Deterministic Daily Tools

Phase 6.6 is required if the product roadmap expects a useful non-AI core
before advanced AI modes. Phase 4 added a deterministic growth-flow skeleton,
but it did not implement the daily tools listed in the locked product direction.

Existing helpful pieces:

- Shared deterministic flow model and tests.
- Compose app destinations for Today, Reflect, Grow, Support and Settings.
- Encrypted storage primitives ready for product-specific records.
- Localization, theme and accessibility-oriented font foundations.

Missing pieces:

- Daily check-in records for mood, energy, stress and sleep.
- Reusable timers and background recovery.
- Grounding and breathing exercises.
- Local reminders, quiet hours, snooze and pause-all.
- Neutral notification copy and privacy-safe lock-screen behavior.
- Private in-app calendar.
- Optional system-calendar handoff with explicit consent and no broad calendar
  reading by default.
- Local trend summaries.
- Decision worksheets, thought or assumption review, problem-solving,
  values-to-action, repair preparation and difficult-conversation preparation.
- Encrypted export/restore for the new product records.

Architecture recommendation:

- Build daily tools as deterministic, offline-first shared domain modules.
- Store all personal tool records through the encrypted storage boundary.
- Keep notifications local, neutral and optional.
- Keep system-calendar integration as an explicit handoff, not passive calendar
  ingestion.
- Do not add public feeds, rankings, manipulative streaks or human-worth scores.

## Database And Migration Implications

Current storage is a generic encrypted record store, not a product schema. Phase
6.4 should not require a backend migration. It may require local lock metadata,
key versioning and rewrap state, stored either in platform secure storage or as
encrypted local records.

Phase 6.6 will need a durable local record model for daily check-ins, timers,
reminders, calendar items, worksheets, local trend snapshots and export/restore
metadata. Those records must remain encrypted at rest and must not be mirrored
to backend storage by default.

Backend migrations are not required for Phase 6.4, Phase 6.5 or Phase 6.6
unless a later explicitly approved sync phase adds ciphertext-only sync tables.

## Security And Privacy Implications

- Phase 6.4 must fail closed: if authentication, key release or lock policy
  fails, private content stays unavailable.
- PIN fallback, if approved, needs KDF, throttling and local reset semantics.
- Local reset must clearly define whether it deletes encrypted vault content,
  installed model packs, knowledge packs, settings or all local data.
- Notifications must be neutral and should not expose personal content on the
  lock screen.
- Relational-boundary enforcement must cover AI output, memory proposals,
  reminders, voice/avatar behavior, exports and sync.
- No passive microphone, location, contacts or broad calendar reading should be
  added for these phases.

## Accessibility And Localization Implications

- All user-facing strings must continue to use Compose resources.
- Lock and reset flows must be screen-reader usable and have large touch
  targets.
- Authentication failures and reset warnings need clear accessible copy.
- Boundary, crisis, consent, legal, privacy and lock copy need qualified human
  review before production localization.
- RTL layouts must be verified for new lock, boundary and daily-tool screens.

## Likely Regressions

- Keychain or Keystore auth-bound key changes can break SQLCipher database
  access, backup/restore or key rotation.
- Lock lifecycle logic can deadlock app startup or lock users out of local data.
- Background privacy measures can interfere with screenshots, accessibility
  tools or testing workflows.
- Reminder and timer background behavior can diverge between Android and iOS.
- Boundary classifiers can over-block ordinary human relationship discussion or
  under-block dependency-building AI behavior.
- Calendar handoff can accidentally over-request permissions if not kept narrow.
- New daily records can bypass encryption if storage adapters are wired
  directly instead of through the encrypted-storage boundary.

## Owner Decisions Required

- Whether Bettamind should support a Bettamind-specific PIN fallback in addition
  to platform biometrics/device credentials.
- Default privacy-lock behavior: opt-in, required before personal storage, or
  required after first private record is created.
- Auto-lock timeout defaults and whether screenshots/app-switcher previews
  should be blocked by default.
- Local reset scope and exact warning copy.
- Whether local reminders are in Phase 6.6 MVP or deferred.
- Whether optional system-calendar handoff is in Phase 6.6 MVP or deferred.
- Which boundary categories require human-reviewed copy before internal test
  release.

## Conclusion

Phase 6.4 is required and should be implemented before Phase 6.5, Phase 6.6 and
Phase 7. Phase 6.5 should follow before any AI response-mode work. Phase 6.6
should follow if the next goal is a complete non-AI daily-use core before Phase
7.
