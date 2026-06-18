# Phase 6X Integration Plan

Date: 2026-06-18

## Scope

Phase 6X is a bridge between the completed Phase 0 through Phase 6 foundations
and the future Phase 7 AI response-mode work. It inserts three focused phases:

- Phase 6.4: App Privacy Lock.
- Phase 6.5: Relational Boundaries.
- Phase 6.6: Deterministic Daily Tools.

This plan does not modify the active implementation plan. It is an amendment
and execution guide for the next owner-approved implementation prompts.

## Recommended Order

1. Phase 6.4 App Privacy Lock.
2. Phase 6.5 Relational Boundaries.
3. Phase 6.6 Deterministic Daily Tools.
4. Roadmap reconciliation.
5. Phase 7 AI response modes.

Phase 6.4 comes first because private local content should not expand before
the app can lock access and release encrypted storage keys only after local
authentication. Phase 6.5 comes before Phase 7 because AI response modes need a
deterministic boundary contract. Phase 6.6 comes before Phase 7 if the product
needs a fuller offline, non-AI daily-use core first.

## Phase 6.4: App Privacy Lock

Goal: add a real app privacy lock that gates private local content and storage
key use without introducing account recovery, cloud dependency or unencrypted
fallback storage.

Status after implementation: Phase 6.4 foundation is implemented and the owner
confirmed Codemagic `ios-simulator-unsigned` passed for the pushed fix commit.
Shared code gates storage-key release behind local authentication, Android has
a `BiometricPrompt` adapter, iOS has a `LocalAuthentication` adapter, Android
and iOS host apps conceal app previews while backgrounded, and common tests
cover the core lock, recovery, PIN verifier and notification-privacy policies.

In scope:

- Shared privacy-lock policy interfaces.
- Lock state and authentication result models.
- Android `BiometricPrompt` plus device credential adapter.
- iOS `LocalAuthentication` adapter.
- Authentication-bound key release or key unwrap for SQLCipher usage where
  platform APIs support it.
- Optional Bettamind PIN fallback only if owner approves it.
- Auto-lock on app backgrounding, idle timeout and sensitive operations.
- Android and iOS background/app-switcher privacy handling.
- Failed-attempt throttling.
- Local reset policy and copy.
- Tests for lock state, key-release failures, reset behavior and no fallback
  storage.
- Documentation and project-memory updates.

Deferred release-hardening dependency:

- Production Bettamind PIN/passphrase storage still needs an audited Argon2id
  provider on Android and iOS. Phase 6.4 intentionally does not replace
  Argon2id with an ad hoc hash fallback.

Out of scope:

- Cloud account recovery.
- Remote lock management.
- Sync.
- Finished Phase 7 AI behavior.
- Daily tools beyond the minimum needed to prove protected content access.

Acceptance criteria:

- Private content access is unavailable while locked.
- Storage key access fails closed before successful authentication.
- Android path uses platform authentication or approved fallback.
- iOS path uses platform authentication or approved fallback.
- No unencrypted key or content fallback exists.
- App backgrounding protects sensitive content from casual preview.
- Reset behavior is explicit and tested.
- Windows checks pass where possible.
- Codemagic `ios-simulator-unsigned` passes after push because shared/iOS code
  will change.

Verification:

- Shared unit tests for lock policy and key-release contracts.
- Android unit or instrumentation-compatible tests for adapter boundaries where
  available on Windows.
- Existing encrypted storage tests.
- Android lint and build checks.
- Codemagic iOS simulator validation.
- Manual review of privacy, accessibility and localization copy.

## Phase 6.5: Relational Boundaries

Goal: add deterministic boundary enforcement before any AI response modes can
produce user-visible content.

Status after implementation: Phase 6.5 foundation is implemented. Shared code
adds deterministic relational risk levels, signals, pre-generation input
assessment, post-generation output validation, no-model fallback identifiers,
minimal encrypted metadata, and default memory/export/sync/notification/voice
surface decisions. Settings exposes the boundary explanation through Compose
resources. Common tests cover allowed ordinary human relationship discussion
and blocked or redirected AI attachment, dependency and sexualized persona
behavior. Codemagic validation is still required for the pushed shared/Compose
changes.

In scope:

- Boundary categories for human relationship support, dependency-building,
  romantic/sexual AI-persona behavior, therapy/diagnosis claims, emergency
  claims, manipulation and consent.
- Shared boundary classifier or rule engine with deterministic results.
- Structured policy result model for allowed, allowed-with-caution, redirect
  and blocked cases.
- AI pre-generation and post-generation contracts for Phase 7.
- Deterministic fallback responses that preserve ordinary human relationship
  discussion.
- Memory proposal rules and permanent-memory consent gates.
- Notification, export, sync, voice and avatar policy rules.
- Tests for allowed relationship topics and blocked AI attachment behavior.
- Safety documentation updates in a later implementation pass.

Out of scope:

- Cloud moderation services.
- Therapy or clinical diagnosis.
- Crisis-service automation.
- Finished Phase 7 AI response modes.

Acceptance criteria:

- Future AI modes cannot bypass boundary checks.
- Ordinary discussion of love, dating, sexuality, consent, loneliness and
  relationships remains allowed when not framed as AI dependency.
- Disallowed persona dependency, romantic or sexualized AI behavior is blocked
  or redirected.
- Memory, notification, export, sync, voice and avatar hooks have explicit
  policy outcomes.
- Tests cover false positive and false negative risks.

Verification:

- Common tests for policy categories and boundary outcomes.
- Review of all new user-facing strings.
- Accessibility and localization review notes.
- Codemagic if shared/iOS code changes.

## Phase 6.6: Deterministic Daily Tools

Goal: build the offline, non-AI daily-use product layer using encrypted local
records and privacy-safe local interactions.

In scope:

- Daily check-ins for mood, energy, stress and sleep.
- Grounding and breathing exercises.
- Reusable timers with background recovery.
- Local reminders, quiet hours, snooze and pause-all if approved.
- Neutral notification strings.
- Private in-app calendar.
- Optional system-calendar handoff if approved.
- Local trend summaries.
- Decision worksheets, thought or assumption review, problem-solving,
  values-to-action, repair preparation and difficult-conversation preparation.
- Encrypted local records and export/restore for daily-tool data.
- Tests for deterministic behavior, encryption boundaries and no backend/AI
  dependency.

Out of scope:

- Public feeds.
- Public ranking.
- Human-worth scoring.
- Manipulative streak systems.
- Cloud sync.
- AI-generated coaching.

Acceptance criteria:

- Daily tools work offline, without account, backend or AI.
- Personal tool records use encrypted storage only.
- Notifications are optional, local and neutral.
- Calendar handoff does not read broad calendar data by default.
- No streak, ranking or worth score is introduced.
- Windows checks pass where possible.
- Codemagic iOS simulator validation passes if shared/iOS code changes.

Verification:

- Common tests for daily-tool domain models.
- Encrypted-storage integration checks.
- Android build and lint.
- Backend checks only if backend files change.
- Codemagic for shared/iOS changes.

## Roadmap Reconciliation

The original Phase 7 through Phase 12 objectives should remain, but they need
new entry conditions:

- Phase 7 cannot start until Phase 6.5 boundary contracts exist.
- Phase 8 safety/support bridge should reuse Phase 6.5 policy outcomes and
  Phase 6.6 deterministic support tools.
- Phase 9 sync must remain ciphertext-only and consent-based for any Phase 6.6
  daily records.
- Phase 10 localization/accessibility must include Phase 6.4 lock copy, Phase
  6.5 boundary copy and Phase 6.6 daily-tool flows.
- Phase 11 multimodal/voice/avatar work must obey Phase 6.5 relational
  boundaries.
- Phase 12 hardening must include lock bypass, key-release, notification
  privacy and relational-boundary red-team tests.

## Database And Migration Plan

Phase 6.4:

- Prefer platform secure storage for lock key metadata where possible.
- Store any shared lock metadata as encrypted records.
- Version key material so future rewrap migrations can be tested.
- Do not add backend migrations.

Phase 6.5:

- Store policy definitions and tests in source, not user data.
- Store user-facing boundary preferences only if explicitly approved and
  encrypted.
- Do not add backend migrations.

Phase 6.6:

- Define encrypted local record types for check-ins, timers, reminders,
  calendar entries, worksheets and trend snapshots.
- Add local schema versioning inside encrypted records.
- Keep export/restore ciphertext-only unless an explicitly approved encrypted
  export format is implemented.
- Defer backend schema until optional ciphertext-only sync is approved.

## Owner Decisions Before Implementation

- Approve or reject Bettamind PIN fallback for Phase 6.4.
- Choose lock default: opt-in, required before private records, or required
  after first private record.
- Choose auto-lock timeout defaults.
- Choose screenshot/app-switcher privacy default.
- Define local reset scope.
- Decide whether reminders are in Phase 6.6 MVP.
- Decide whether system-calendar handoff is in Phase 6.6 MVP.
- Confirm Phase 6.5 boundary categories for internal test release. The initial
  deterministic category set is implemented for Phase 6.5 and should be
  reviewed before production localization or Phase 7 response-mode prompts.

## Exact Next Prompt

Use this prompt to begin the next implementation slice after Phase 6.5 is
committed, pushed and validated:

```text
Implement Phase 6.6 only from docs/planning/phase-6x-integration-plan.md.

Do not implement Phase 7.
Do not replace the existing implementation plan.
Preserve the offline, account-free, backend-optional and no unencrypted
fallback rules.

Build the deterministic daily-use core using encrypted local records. Include
the smallest complete slice of daily check-ins, grounding or breathing tools,
local reminder policy, neutral notification copy and private record models that
can run offline without account, backend or AI. Do not add cloud sync, public
feeds, rankings, manipulative streaks, human-worth scores or AI-generated
coaching.

Before coding, read AGENTS.md and the required product, specification,
planning, risk and project-memory docs. Then implement the smallest complete
Phase 6.6 slice, run available Windows checks, update docs/working-notes/project-memory.md
and relevant planning/risk docs, report results, and stop.
```
