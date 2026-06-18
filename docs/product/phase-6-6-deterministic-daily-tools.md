# Phase 6.6 Deterministic Daily Tools

Date: 2026-06-18

## Scope

Phase 6.6 adds the first shared deterministic daily-use foundation. It does not
add AI response modes, AI-generated coaching, cloud sync, public feeds, public
rankings, manipulative streaks, human-worth scoring, broad calendar reading or
backend dependency.

The implementation lives in
`shared/src/commonMain/kotlin/org/bettamind/shared/daily/DeterministicDailyTools.kt`.

## Implemented Foundation

The shared daily module includes:

- daily check-in records for mood, energy, stress and sleep;
- an encrypted daily-record repository backed only by `EncryptedRecordStore`;
- encrypted backup and restore delegation through the existing encrypted
  storage contract;
- deterministic box-breathing and grounding exercise catalogs;
- reusable timer recovery for background or interrupted sessions;
- local reminder policy with quiet hours, snooze, pause-all and neutral
  notification preview;
- private in-app calendar entry model and explicit system-calendar handoff
  policy that does not read the calendar by default;
- deterministic worksheet templates for values-to-action, problem-solving,
  repair preparation and difficult-conversation preparation;
- local trend summaries from check-ins without AI or human-worth scoring;
- Compose copy on the Today screen describing the daily-tool foundation.

## Privacy Boundary

Daily personal records require encrypted local storage. There is no
unencrypted fallback path. Backend sync is off by default and not implemented
in this phase. Reminder previews are neutral and must not include personal
content on the lock screen.

Calendar integration remains private in-app by default. A system-calendar
handoff can be offered only after explicit user approval and does not grant
Bettamind broad calendar-read access.

## Localization

English is the source locale. Non-English Phase 6.6 resource entries are draft
fallback text and require qualified human review before production use.

## Verification

Common tests in
`shared/src/commonTest/kotlin/org/bettamind/shared/daily/DeterministicDailyToolsTest.kt`
cover encrypted-only record persistence, encrypted backup/restore, privacy
policy flags, deterministic breathing and grounding tools, timer recovery,
neutral reminder behavior, quiet hours, pause-all, snooze, private calendar
handoff, worksheet templates, local trend summaries and validation of record
IDs and local dates.
