# Phase 8 Safety And Support Bridge

Date: 2026-06-20

## Scope

Phase 8 adds a deterministic shared safety-support bridge. It composes the
existing Phase 6.5 relational-boundary policy, Phase 6.6 daily-tool foundation,
Phase 6.7 harm-safety policy and Phase 7.5 compassionate redirection layer.

It does not add emergency-service automation, automatic third-party contact,
backend sync, cloud AI, model downloads, speech, persistent crisis narrative
storage or production-approved translations.

The implementation lives in
`shared/src/commonMain/kotlin/org/bettamind/shared/support/SafetySupportBridge.kt`.

## Implemented Boundary

The shared bridge provides:

- deterministic support risk levels: `none`, `reflective`, `concern`,
  `urgent`, `immediate` and `refused_capability`;
- support needs for self-harm, suicide, violence de-escalation, dangerous
  capability refusal, relational-boundary overlap, grounding, breathing,
  delay, repair, nonviolent choices, trusted-human support, local emergency
  resources and safe prevention;
- voluntary support actions for check-ins, grounding, breathing, delay,
  leaving or creating distance, contacting trusted support by user choice,
  local emergency help by user choice, conflict reflection, repair planning,
  nonviolent messages, values-to-action and safe prevention;
- local resource metadata for emergency, crisis/community, trusted-person and
  professional support without personal-data use;
- minimum-detail support summaries that exclude raw narrative, harmful
  methods, diagnosis claims, location, contacts and unrelated history;
- explicit sharing preview metadata and local step-up authentication through
  `SensitiveAction.ShareWithProfessional`;
- no automatic contact and no claim that help has been contacted.

## Support And Sharing Policy

Support actions are always voluntary and user-initiated. Bettamind may display
resource types or prepare a minimum-detail summary, but the user decides
whether to contact a person, professional or emergency service.

Sensitive support sharing requires explicit selection, preview acceptance and
local step-up authentication. Dangerous-capability requests are refused and are
not shareable as support summaries because the bridge must not package
actionable harmful details.

Crisis, violent, criminal, self-harm and relationally sensitive narrative is
not stored by default. The bridge produces structured metadata only and keeps
permanent memory, export, sync and notification eligibility off by default for
sensitive support contexts.

## Daily Tools

The bridge maps safety outcomes to deterministic daily tools when appropriate:

- check-ins;
- grounding exercises;
- breathing timers;
- delay and problem-solving worksheets;
- conflict reflection;
- repair preparation;
- difficult-conversation and nonviolent-message prompts;
- values-to-action prompts.

These tools remain local, offline and usable without AI.

## Local Resources

`LocalSupportResourceCatalog` exposes resource-type metadata for the current
locale. It does not read the user's location, contacts, calendar, personal
records or network state. Each resource has `usesPersonalData = false`,
`storesPersonalData = false`, `autoContactAllowed = false` and
`claimsHelpContacted = false`.

## Localisation And Review

English source strings were added to Compose resources, with matching draft
fallback keys in the configured target locale folders. Safety, crisis,
support, emergency, privacy and consent copy still requires qualified human
review before production release.

## Verification Coverage

Common tests in
`shared/src/commonTest/kotlin/org/bettamind/shared/support/SafetySupportBridgeTest.kt`
cover risk wire names, self-harm and suicidal intent, violence intent,
dangerous capability refusal, relational overlap, daily-tool crisis
integration, no automatic contact, minimum-detail preview, step-up sharing,
local resource privacy and localization review flags.
