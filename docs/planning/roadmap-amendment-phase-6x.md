# Roadmap Amendment: Phase 6X

Date: 2026-06-18

## Status

This document records the Phase 6X roadmap amendment and the later roadmap
reconciliation. The pre-amendment implementation plan has been archived at
`docs/planning/archive/implementation-plan-before-phase-6x.md`.

Phase 6.4, Phase 6.5, Phase 6.6 and Phase 6.7 foundations are implemented.
Roadmap reconciliation is complete as a planning-only pass. The next
implementation slice may be Phase 7 only after explicit owner approval.

## Amendment

Insert four intermediate phases after Phase 6 and before Phase 7:

| Phase | Name | Purpose |
| --- | --- | --- |
| 6.4 | App Privacy Lock | Gate private local content and encrypted storage key use behind local authentication. |
| 6.5 | Relational Boundaries | Add deterministic safety boundaries before AI response modes. |
| 6.6 | Deterministic Daily Tools | Build the non-AI daily-use core with encrypted local records. |
| 6.7 | Harmful Intent And Dangerous Capability Safeguards | Add deterministic pre/post-generation harm-safety, privacy and daily-tool rules before AI response modes. |

## Reconciled Effect On Existing Roadmap

| Affected phase | Original requirement | Amended requirement | Reason | Tests added or required | Original requirement unchanged |
| --- | --- | --- | --- | --- | --- |
| Phase 7: AI-assisted response modes | Add Quick Guidance, Guided Reflection, Deep Exploration and Action-Only modes with deterministic fallback and safety not solely reliant on AI. | Add the same modes as optional local AI features with no-model fallback, app-lock protection for sensitive context, consent before using daily-tool context, relational-boundary enforcement, harm-safety enforcement, pre-generation classification, post-generation validation, structured response schema, memory/export eligibility controls, safe refusal/redirection templates, golden tests, adversarial tests and no cloud AI. | Phase 6.4-6.7 created required privacy, daily-context and safety contracts that must wrap every AI path before AI becomes user-visible. | Required Phase 7 tests: response-mode golden tests, no-model fallback tests, app-lock step-up tests, context-consent tests, relational and harm-safety pre/post-generation tests, structured schema tests, memory/export eligibility tests and adversarial jailbreak tests. | Yes |
| Phase 8: Safety and support bridge | Implement layered safety and consent-based encrypted third-party support. | Reuse harm-safety, relational-boundary and daily-tool decisions for self-harm, violence intent, dangerous-capability refusal, crisis/daily-tool integration, voluntary support actions, no automatic third-party contact, minimum-necessary support summaries, step-up authentication, local emergency/support resources, no crisis narrative storage by default and human-reviewed safety translations. | Phase 6.5 and 6.7 define safety outcomes; Phase 6.6 supplies non-AI de-escalation tools; Phase 6.4 supplies step-up sharing controls. | Required Phase 8 tests: self-harm, violence, dangerous capability, relational overlap, daily-tool crisis, no-auto-contact, support-summary minimum-detail, step-up sharing and localisation review-flag tests. | Yes |
| Phase 9: Optional backend, encrypted export and sync | Add ciphertext-only sync and signed pack delivery. | Keep backend optional, sync disabled by default and ciphertext-only; require app-lock reauthentication for sync/export; enforce daily-tool export rules, relational-content exclusion, harmful-intent exclusion, conflict handling, device revocation, encrypted backup/restore and optional system-calendar handoff without broad calendar reading. | Phase 6.4-6.7 added sensitive local records and content categories that must not leak through export or sync. | Required Phase 9 tests: ciphertext-only backend contract, sync disabled by default, step-up sync/export, daily/relational/harm-safety export exclusion, conflict handling, device revocation and backup/restore tests. | Yes |
| Phase 10: Global localisation and accessibility | Complete target locale packs and accessibility validation. | Include app-lock flows, relational-boundary copy, harmful-intent/crisis copy, daily tools, reminders, calendar, trend summaries, worksheets, timer, support bridge, RTL testing, screen-reader testing, large text, reduced motion, low-literacy mode and human review for safety-critical translations. | Phase 6.4-6.7 added sensitive privacy, wellbeing and safety surfaces requiring accessibility and qualified localisation review. | Required Phase 10 tests: locale completeness, RTL, screen-reader labels where automatable, large text, reduced motion, low-literacy review and human-review tracking for safety-critical strings. | Yes |
| Phase 11: Optional offline speech | Add optional offline speech-to-text and text-to-speech packs. | Keep text-only app complete; make microphone optional; retain no raw audio by default; integrate app-lock; route voice input through the same safety pipeline as text; prevent romantic, seductive, possessive or manipulative voice output; apply harm-safety to spoken input/output; preserve accessibility fallback and offline operation. | Speech can amplify privacy, relational and harm-safety risks, so it must inherit Phase 6.4, 6.5 and 6.7 boundaries. | Required Phase 11 tests: permission and no-raw-audio-retention tests, text-only fallback tests, relational and harm-safety speech pipeline tests, voice persona boundary tests and offline speech-pack validation tests if packs are introduced. | Yes |
| Phase 12: Performance, red-team and release readiness | Harden for release with low-resource testing, threat model, red-team resolution, store metadata, rollback and TestFlight gates. | Include app-lock bypass, encryption-key protection, romantic attachment, sexualisation, harmful capability, self-harm, violence, jailbreak, reminder privacy, notification privacy, timer lifecycle, background behavior, calendar privacy, export privacy, sync privacy, low-memory testing, Android physical-device testing, Codemagic/Xcode validation, TestFlight testing and store-readiness review. | Phase 6.4-6.7 added new privacy and safety surfaces that must be included in hardening and release gates. | Required Phase 12 tests: privacy-lock bypass, encryption/key-release, relational and harm red-team suites, reminder/notification/timer/calendar/export/sync privacy, low-resource, physical-device, Codemagic/Xcode and TestFlight checks. | Yes |

## Non-Goals

- Do not reopen completed Phase 0 through Phase 6 foundations except where a
  later approved phase needs to integrate with existing Phase 6.4, 6.5, 6.6 or
  6.7 contracts.
- Do not add cloud AI to core functionality.
- Do not make the backend mandatory.
- Do not add unencrypted fallback storage.
- Do not add finished AI response modes before Phase 7.
- Do not add public feeds, rankings, human-worth scores or manipulative
  streaks.

## Roadmap Effects

- The next implementation task after this reconciliation may be Phase 7 only if
  the owner explicitly approves a Phase 7 implementation prompt.
- Phase 6.4 established the privacy-lock and storage-key release boundary that
  later sensitive actions must use.
- Phase 6.5 established the safety contract that future AI, memory,
  notification, voice and avatar systems must call.
- Phase 6.6 established Bettamind's offline daily-use core before AI becomes
  user-visible.
- Phase 6.7 established the deterministic harm-safety boundary that future AI,
  memory, export, sync, notification, support and daily-tool surfaces must use.
- More Codemagic runs should be expected after future commits that touch
  shared Kotlin, Compose resources, `iosApp`, iOS-affecting Gradle
  configuration or Codemagic iOS workflow files.

## Stop Rule

After this reconciliation, do not begin Phase 7 or later work until the owner
explicitly approves the next implementation prompt.
