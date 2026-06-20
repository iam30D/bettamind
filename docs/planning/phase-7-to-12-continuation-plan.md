# Phase 7 To 12 Continuation Plan

Date updated: 2026-06-18

## Purpose

This is the controlled continuation plan after Phase 6X, Phase 6.4, Phase 6.5,
Phase 6.6 and Phase 6.7. It preserves the original Phase 7 through Phase 12
objectives while adding entry conditions and acceptance criteria from the
completed privacy-lock, relational-boundary, deterministic daily-tool and
harm-safety foundations.

This plan is a roadmap control document and does not replace the archived
pre-Phase-6X plan at
`docs/planning/archive/implementation-plan-before-phase-6x.md`.

Status update: Phase 7 is implemented as an optional local AI growth-mode
foundation, Phase 7.5 is implemented as a deterministic compassionate
safety-redirection layer, and Phase 8 is implemented as a deterministic safety
and support bridge foundation. Phase 9 and later phases are not started.

## Completed Preconditions

- Phase 6.4 App Privacy Lock: local authentication, step-up actions and
  vault-key release policy exist.
- Phase 6.5 Relational Boundaries: deterministic relational pre-generation,
  post-generation and surface policies exist.
- Phase 6.6 Daily Tools: check-ins, breathing, grounding, timers, reminders,
  private calendar, worksheets, encrypted daily records and local trends exist.
- Phase 6.7 Harm Safety: deterministic harmful-intent and dangerous-capability
  safeguards exist for pre-generation, post-generation, privacy surfaces,
  support summaries, daily tools and relational overlap.

## Continuation Principles

- Core use remains offline, account-free and useful without AI.
- AI remains optional, local, replaceable and removable.
- No cloud AI is added to core functionality.
- No backend feature is mandatory for core mobile use.
- Personal content leaves the device only after purpose-specific consent.
- Permanent memory still requires separate approval.
- No unencrypted fallback storage is allowed.
- Bettamind does not act as therapy, diagnosis, legal advice, financial advice,
  emergency service, romantic partner or sentient companion.
- Bettamind preserves dignity, autonomy, responsibility, truth-seeking,
  compassionate love, justice, humility, repair and growth.
- Bettamind must not judge, condemn, insult or misunderstand users, while still
  refusing assistance for harm, abuse, exploitation, self-harm, violence or
  dangerous capability.

## Phase 7: AI-Assisted Growth Modes

Original objective: add optional AI-assisted response modes after deterministic
flows are stable.

Amended objective: add optional on-device AI-assisted growth support only after
the deterministic safety, privacy and daily-tool contracts are enforced around
every AI path.

Acceptance criteria:

- Quick Guidance, Guided Reflection, Deep Exploration and Action-Only modes
  exist.
- AI remains optional, local, replaceable and removable.
- The no-model deterministic fallback remains complete for every mode.
- No cloud AI is used for core functionality.
- App-lock step-up protects sensitive AI context and generated summaries.
- Check-in, worksheet, timer, calendar, local trend and practice context is
  included in AI prompts only after explicit user consent.
- Relational-boundary metadata is enforced before generation, after generation
  and before display, storage, export, sync, notification, voice or avatar use.
- Harmful-intent and dangerous-capability safeguards are enforced before
  generation, after generation and before all privacy surfaces.
- Pre-generation classification produces a structured decision.
- Post-generation validation rejects unsafe or boundary-violating output.
- Responses use a structured schema with safety metadata, memory eligibility,
  export eligibility and fallback identifiers.
- Permanent memory is off by default and proposed only when eligible and
  separately approved.
- Export eligibility is explicit and excludes sensitive relational or
  harm-safety content by default.
- Safe refusal and safe redirection templates exist and are nonjudgmental.
- Golden tests cover each response mode and no-model fallback.
- Adversarial tests cover relational attachment, sexualized persona, harmful
  capability, self-harm, violence, jailbreak and malformed model output.
- Model weights, native runtime dependencies and trust anchors are not
  committed unless separately approved.

Required tests:

- Mode selection and structured schema tests.
- No-model fallback tests for all modes.
- App-lock step-up tests for sensitive AI context.
- Relational-boundary pre/post-generation tests.
- Harm-safety pre/post-generation tests.
- Memory/export eligibility tests.
- Golden and adversarial response tests.

Status: completed for the shared Phase 7 foundation. Production model choices,
model licences, trust anchors, human review and Codemagic validation remain
owner/release gates.

## Phase 7.5: Compassionate Safety Redirection

Objective: make safety responses firm, useful and dignity-preserving before
the Phase 8 support bridge is started.

Acceptance criteria:

- Compassionate safety-redirection mode, reason, pathway, response and decision
  models exist.
- Better-human pathways connect safety boundaries to grounding, breathing,
  delay, leaving the situation, contact support, emergency help, reflection,
  repair, values, difficult conversations, consent and boundaries,
  self-compassion or no follow-up needed.
- Safety redirects acknowledge feelings without validating harmful action,
  refuse harmful assistance, avoid shame and diagnosis, separate person from
  action, and offer practical deterministic next steps.
- AI structured metadata exposes safety boundary, reason, user intent
  confidence, allowed discussion scope, better-human pathway, recommended
  tool, memory/export eligibility, step-up authentication and urgent support.
- Generated output is blocked if it shames, diagnoses, assumes bad intent,
  encourages dependency or skips a safe next step when a boundary is applied.
- Unsafe reminder creation is refused with neutral safe replacement reminders.
- Sensitive safety narrative remains memory-ineligible and export-excluded by
  default; selected sensitive export requires preview and step-up
  authentication.

Status: completed as a deterministic foundation. It does not add Phase 8
support bridge behavior, automatic contact, backend sync, cloud AI or model
downloads.

## Phase 8: Safety And Support Bridge

Original objective: implement layered safety and consent-based encrypted
third-party support.

Amended objective: implement a deterministic safety and support bridge that
uses Phase 6.5 relational policy, Phase 6.6 daily tools and Phase 6.7
harm-safety policy before any AI-assisted support flow is visible.

Acceptance criteria:

- Self-harm handling uses deterministic urgent/immediate pathways.
- Violence intent handling protects others without accusation or automatic
  contact.
- Dangerous capability requests are refused with safe redirection.
- Relational-risk integration detects AI-rejection, dependency, jealousy and
  attachment overlap.
- Daily tools offer check-ins, grounding, breathing, delay, repair and
  nonviolent-choice support where appropriate.
- Support actions are voluntary and user-controlled.
- Bettamind never automatically contacts a third party.
- Support summaries use minimum necessary detail.
- Step-up authentication is required before sensitive sharing.
- Local emergency and support resources can be displayed without revealing
  personal data.
- Crisis or harmful narrative is not stored by default.
- Safety-critical translations require qualified human review.

Required tests:

- Self-harm, suicidal intent, violence intent and dangerous capability tests.
- Relational-risk overlap tests.
- Daily-tool crisis integration tests.
- No-auto-contact tests.
- Support summary minimum-detail and step-up-authentication tests.
- Localisation review-flag tests.

Status: implemented as a shared deterministic foundation in
`shared/src/commonMain/kotlin/org/bettamind/shared/support/SafetySupportBridge.kt`.
It adds voluntary support actions, minimum-detail sharing previews, local
resource metadata and step-up sharing metadata without automatic contact,
backend sync, cloud AI, model downloads, speech or persistent crisis narrative
storage.

## Phase 9: Optional Backend, Encrypted Export And Sync

Original objective: add ciphertext-only sync and signed pack delivery through
an optional backend.

Amended objective: add optional export, backup, device management and
ciphertext-only sync without making backend use mandatory or expanding plaintext
handling.

Acceptance criteria:

- Backend remains optional and mobile core runs without it.
- Sync is disabled by default.
- Sync payloads are ciphertext-only; backend never receives plaintext personal
  content.
- App-lock reauthentication is required before enabling sync or export.
- Daily-tool records obey export and sync rules.
- Relationally sensitive content is excluded by default.
- Harmful-intent content is excluded by default.
- Export inclusion requires explicit selection and preview where sensitive.
- Conflict handling is deterministic and does not silently overwrite local
  private data.
- Device revocation and recovery paths are explicit.
- Encrypted backup and restore are tested.
- Optional system-calendar handoff remains a local explicit handoff and does
  not read broad calendar data.

Required tests:

- Ciphertext-only backend contract tests.
- Sync disabled-by-default tests.
- Step-up authentication for sync/export tests.
- Daily, relational and harm-safety export-exclusion tests.
- Conflict resolution and device revocation tests.
- Encrypted backup/restore tests.

Status: implemented as a shared deterministic export/sync contract in
`shared/src/commonMain/kotlin/org/bettamind/shared/sync/EncryptedExportSync.kt`
plus an optional FastAPI `POST /sync/envelopes` contract. The implementation
keeps mobile core backend-independent, sync disabled by default, payloads
ciphertext-only, app-lock step-up required for export/sync, daily/relational/
harm-safety/support records excluded by default, conflict resolution
non-destructive, device revocation explicit and backup/restore encrypted. It
does not begin Phase 10 localization/accessibility completion.

## Phase 10: Global Localisation And Accessibility

Original objective: complete target locale packs and accessibility validation.

Amended objective: complete localisation and accessibility for the expanded
privacy, boundary, daily-tool, harm-safety and support surfaces.

Acceptance criteria:

- Target locales are complete only after qualified review where required.
- App-lock flows are localised and screen-reader usable.
- Relational-boundary copy is reviewed before production.
- Harmful-intent, crisis, emergency, legal and consent copy is reviewed before
  production.
- Daily tools, reminders, calendar, local trends, worksheets, timer and support
  bridge copy are localised.
- RTL testing covers Arabic across affected screens.
- Screen-reader testing covers lock, daily tools, support and safety flows.
- Large text and dynamic type do not break layouts.
- Reduced motion is supported for timers, transitions and grounding exercises.
- Low-literacy mode is validated.
- Locale-aware dates, numbers and plurals are used.

Required tests:

- Locale resource completeness tests.
- RTL layout tests.
- Screen-reader label checks where automatable.
- Large-text, reduced-motion and low-literacy review checks.
- Human-review tracking tests for safety-critical strings.

Status: implemented as a shared localisation and accessibility foundation in
`shared/src/commonMain/kotlin/org/bettamind/shared/accessibility/GlobalLocalizationAccessibility.kt`.
The implementation covers target locale profiles, RTL and script fallback
metadata, locale-aware formatting metadata, resource completeness, qualified
human-review tracking gates for safety-critical strings, screen-reader surface
requirements, large text, reduced motion and low-literacy mode. Settings now
exposes accessibility controls. Target locale keys are complete as draft
fallbacks, but production approval still requires qualified human-review
records. It does not begin Phase 11 speech.

## Phase 11: Optional Offline Speech

Original objective: add optional offline speech-to-text and text-to-speech
packs.

Amended objective: add optional offline speech while preserving text-only
completeness and applying the same privacy, relational and harm-safety
boundaries to spoken input and output.

Acceptance criteria:

- Text-only app remains complete.
- Microphone use is optional and permission-scoped.
- Raw audio is not retained by default.
- App-lock behavior protects sensitive speech transcripts and summaries.
- Voice input uses the same safety pipeline as text.
- Voice output cannot sound romantic, seductive, possessive or manipulative.
- Harmful-intent safeguards apply to spoken input and generated spoken output.
- Accessibility fallback exists for users who cannot or do not want to use
  speech.
- Offline operation is preserved.
- Speech packs are optional, licensed, signed, removable and locally validated.

Required tests:

- Permission and no-raw-audio-retention tests.
- Text-only fallback tests.
- Relational and harm-safety speech pipeline tests.
- Voice-output persona boundary tests.
- Offline speech-pack validation tests where packs are introduced.

## Phase 12: Performance, Red-Team And Release Readiness

Original objective: harden for real release.

Amended objective: harden the full offline-first, encrypted, optional-AI,
support-capable product across privacy, safety, performance, accessibility,
store-readiness and iOS validation.

Acceptance criteria:

- App-lock bypass attempts are tested.
- Encryption-key protection and key-release failure modes are tested.
- Romantic attachment red-team cases are resolved or accepted.
- Sexualisation red-team cases are resolved or accepted.
- Harmful capability, self-harm, violence and jailbreak red-team cases are
  resolved or accepted.
- Reminder and notification privacy are reviewed.
- Timer lifecycle and background behavior are tested.
- Calendar privacy is tested.
- Export and sync privacy are tested.
- Low-memory, battery, thermal and startup behavior are tested.
- Android physical-device testing is complete.
- Codemagic/Xcode validation is complete for iOS.
- TestFlight testing is complete before release.
- Store-readiness review covers privacy labels, screenshots, metadata, support
  claims and safety disclaimers.

Required tests:

- Privacy-lock bypass tests.
- Encryption and key-release tests.
- Relational, sexualisation, harm-capability, self-harm, violence and jailbreak
  red-team suites.
- Reminder, notification, timer, calendar, export and sync privacy tests.
- Low-resource and physical-device checks.
- Codemagic/Xcode and TestFlight validation gates.

## Phase 8 Entry Requirements

Before Phase 8 begins:

- Owner confirms Phase 7.5 Codemagic `ios-simulator-unsigned` validation passed
  for the pushed shared/Gradle/resource changes.
- Owner confirms Phase 8 will implement the safety and support bridge only, not
  Phase 9 sync, Phase 10 localisation completion, Phase 11 speech or Phase 12
  release readiness.
- Owner reviews Phase 6.5 relational categories, Phase 6.7 harm-safety
  categories, Phase 7 AI-growth fallback identifiers and Phase 7.5
  compassionate safety-redirection copy before support flows rely on them
  broadly.
- Owner confirms local emergency/support resource scope and copy-review
  expectations.
- Codemagic remains the required iOS validation path after shared/iOS-affecting
  changes.

## Exact Recommended Phase 8 Prompt Summary

Implement Phase 8 only: safety and support bridge. Preserve offline,
account-free, backend-optional and no-model operation. Reuse Phase 6.5
relational boundaries, Phase 6.6 daily tools, Phase 6.7 harm-safety safeguards
and Phase 7 AI-growth metadata. Add deterministic self-harm, suicide, violence
intent and dangerous-capability support pathways; voluntary support actions;
minimum-necessary support summaries; local emergency/support resource display;
explicit preview and app-lock step-up before sensitive sharing; no automatic
third-party contact; no claim that help was contacted unless the user completed
that action. Add tests for self-harm, violence, dangerous capability,
relational overlap, daily-tool crisis integration, no-auto-contact,
minimum-detail summaries, step-up sharing, localisation review flags and
accessibility. Update docs and project memory, run Windows checks, commit and
push, then stop. Do not implement Phase 9 sync, Phase 10 localisation
completion, Phase 11 speech, Phase 12 release readiness, cloud AI, model
downloads, model weights or backend-mandatory behavior.
