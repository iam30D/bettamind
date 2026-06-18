# Roadmap Amendment: Phase 6X

Date: 2026-06-18

## Status

This document amends the roadmap without editing the active
`docs/planning/implementation-plan.md`. The pre-amendment implementation plan
has been archived at
`docs/planning/archive/implementation-plan-before-phase-6x.md`.

Phase 6.4 and Phase 6.5 foundations are implemented. Phase 6.6 remains the
next proposed implementation slice before Phase 7.

## Amendment

Insert three intermediate phases after Phase 6 and before Phase 7:

| Phase | Name | Purpose |
| --- | --- | --- |
| 6.4 | App Privacy Lock | Gate private local content and encrypted storage key use behind local authentication. |
| 6.5 | Relational Boundaries | Add deterministic safety boundaries before AI response modes. |
| 6.6 | Deterministic Daily Tools | Build the non-AI daily-use core with encrypted local records. |

## Effect On Existing Roadmap

| Existing phase | Original objective remains | Amendment |
| --- | --- | --- |
| Phase 7: On-device AI response modes | Yes | Must wait until Phase 6.5 boundary contracts exist. |
| Phase 8: Safety and support bridge | Yes | Must reuse Phase 6.5 policy outcomes and avoid claiming automatic contact. |
| Phase 9: Optional backend and encrypted sync | Yes | Must keep Phase 6.6 daily-tool records ciphertext-only and consent-based if synced. |
| Phase 10: Accessibility, localization and inclusive QA | Yes | Must include lock, boundary and daily-tool copy and flows. |
| Phase 11: Multimodal and voice expansion | Yes | Must obey Phase 6.5 rules for voice, avatar and attachment-sensitive behavior. |
| Phase 12: Hardening, store readiness and launch candidate | Yes | Must include lock-bypass, key-release, notification-privacy and relational-boundary red-team checks. |

## Non-Goals

- Do not reopen completed Phase 0 through Phase 6 foundations except where a
  Phase 6.4, 6.5 or 6.6 adapter needs to integrate with them.
- Do not add cloud AI to core functionality.
- Do not make the backend mandatory.
- Do not add unencrypted fallback storage.
- Do not add finished AI response modes before Phase 7.
- Do not add public feeds, rankings, human-worth scores or manipulative
  streaks.

## Roadmap Effects

- The next implementation task should be Phase 6.4, not Phase 7.
- Phase 6.4 may require careful changes to Android Keystore, iOS Keychain and
  encrypted-storage key-release flow.
- Phase 6.5 creates the safety contract that future AI, memory, notification,
  voice and avatar systems must call.
- Phase 6.6 gives Bettamind a stronger offline daily-use core before AI becomes
  user-visible.
- More Codemagic runs should be expected because Phase 6.4 and likely Phase
  6.6 will touch shared/iOS code.

## Stop Rule

After this audit, do not begin Phase 6.4, Phase 6.5, Phase 6.6 or Phase 7
until the owner explicitly approves the next implementation prompt.
