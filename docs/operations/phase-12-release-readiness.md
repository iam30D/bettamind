# Phase 12 Release Readiness

Date updated: 2026-06-20

## Scope

Phase 12 adds the repository-side release-readiness foundation for Bettamind.
It does not approve a production release by itself. Production release still
requires owner-controlled device testing, TestFlight, store metadata review,
qualified translation review, rollback evidence and Codemagic iOS validation
for the pushed release-candidate commit.

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

## Manual Owner Gates

- Run Android release-candidate smoke tests on representative low, standard and
  high devices.
- Record startup, memory, battery, thermal and background behavior evidence on
  physical devices.
- Run Codemagic `ios-simulator-unsigned` against the pushed Phase 12 commit.
- Complete TestFlight installation and privacy/safety smoke testing.
- Review App Store and Play Store metadata, screenshots, privacy labels,
  support claims and safety disclaimers under the publishing entity.
- Complete qualified human review records for every production non-English
  safety, crisis, legal, privacy and consent string.
- Document rollback owners and revocation paths for app binary releases,
  signed model packs, speech packs and knowledge packs.

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
