# Phase 12 Release Readiness

Date updated: 2026-06-26

## Scope

Phase 12 adds the repository-side release-readiness foundation for Bettamind.
It does not approve a production release by itself. Production release still
requires owner-controlled device testing, TestFlight smoke evidence, store
metadata review, qualified translation review and rollback evidence.

The owner confirmed Codemagic `ios-simulator-unsigned` passed for the Phase 12
release-readiness foundation on 2026-06-25. A manual Codemagic
`ios-testflight-release` workflow is now present for the signed IPA and App
Store Connect upload path, but it requires owner-managed Apple signing and
Codemagic secure credentials before it can pass.

## Implemented Repository Gates

- `ReleaseReadinessPolicy` records required gates for privacy lock,
  encryption-key protection, red-team coverage, reminders, notifications,
  timer lifecycle, background privacy, calendar, export, sync, speech,
  localisation/accessibility, low-resource behavior, Android devices,
  Codemagic iOS, TestFlight, store metadata, rollback and artifact policy.
- `ReleaseRedTeamSuite` covers romantic attachment, sexualization, harmful
  capability, self-harm, violence, jailbreak/policy bypass and unsafe spoken
  output using existing deterministic safety policies.
- Repository checks intentionally block `productionReady` until owner evidence
  is recorded for physical devices, Codemagic, TestFlight, store metadata,
  low-resource performance, battery/thermal/memory, rollback and qualified
  translation review.
- Settings exposes a release-readiness foundation panel without claiming that
  production approval is complete.

## Code-Side App UX Integration

The shared Compose app now exposes more of the implemented repository
foundation as usable local UI:

- the header renders the Bettamind brand mark from approved Compose resources;
- Today exposes encrypted check-in controls after adult confirmation, plus
  breathing steps, grounding steps and deterministic worksheet prompts without
  creating unencrypted fallback storage;
- Grow exposes Quick Guidance, Guided Reflection, Deep Exploration and
  Action-Only as selectable concern-prompt modes backed by the existing
  `AiGrowthModeEngine`;
- no-model operation is explicit: when no local model pack is installed, the
  app shows deterministic fallback guidance and records no automatic memory,
  export, sync or notification;
- Support exposes deterministic local support assessment, voluntary actions
  and local resource types without automatic contact.
- Settings exposes local platform integration states for reminders, calendar
  handoff, OS speech and optional signed model packs.
- Qwen2.5 1.5B Instruct is the first optional model-pack target; production
  model-pack status stays blocked until the real `.litertlm` artifact, signed
  manifest, owner public trust anchor and device evidence are complete.

This is a code-side production-readiness improvement, not production approval.
Model-pack installation/runtime artifacts, platform reminder scheduling,
platform calendar handoff, TestFlight evidence, store metadata and qualified
translation review remain separate release gates.

## Manual Owner Gates

- Run Android release-candidate smoke tests on representative low, standard and
  high devices.
- Record startup, memory, battery, thermal and background behavior evidence on
  physical devices.
- Configure the Apple Developer, App Store Connect and Codemagic secure signing
  setup documented in `docs/operations/testflight-readiness.md`.
- Run Codemagic `ios-testflight-release` against the pushed release-candidate
  commit and retain the uploaded build number.
- Complete TestFlight installation and privacy/safety smoke testing.
- Review App Store and Play Store metadata, screenshots, privacy labels,
  support claims and safety disclaimers under the publishing entity.
- Complete qualified human review records for every production non-English
  safety, crisis, legal, privacy and consent string.
- Document rollback owners and revocation paths for app binary releases,
  signed model packs, speech packs and knowledge packs.
- Record release evidence in `docs/operations/release-evidence-template.md`
  and model-pack evidence in
  `docs/operations/model-pack-owner-evidence-template.md`.

## Artifact Rules

Do not commit model weights, converted model artifacts, speech packs,
production knowledge packs, signing private keys, provisioning profiles,
certificates, store upload archives, release packages, databases, logs with
personal content or secrets. Public templates and non-secret metadata are
acceptable only when they contain no weights or credentials.

## Verification

The Windows-side Phase 12 task is:

```powershell
.\gradlew.bat phaseTwelveCheck
```

This task cannot replace Codemagic, TestFlight or physical-device evidence.
