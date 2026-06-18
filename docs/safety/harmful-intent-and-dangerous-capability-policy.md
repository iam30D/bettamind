# Phase 6.7 Harmful Intent And Dangerous Capability Safeguards

Date: 2026-06-18

## Scope

Phase 6.7 adds a deterministic shared policy layer for harmful intent,
dangerous capability and unsafe generated output. It does not add finished AI
response modes, cloud moderation, backend product engines, emergency-service
automation, persistent narrative storage or production-approved localisation.

The implementation lives in
`shared/src/commonMain/kotlin/org/bettamind/shared/safety/HarmfulIntentSafeguards.kt`.
It is local, model-free and usable offline.

## Implemented Boundary

The shared policy provides:

- risk levels: `none`, `ambiguous`, `concern`, `urgent`, `immediate` and
  `disallowed_capability`;
- categories for self-harm, suicide, violence, targeted threats, weapons,
  explosive harm, chemical/biological/radiological harm, poisoning, stalking,
  coercion, child safety, sexual exploitation, fraud or crime, concealment,
  evasion, policy bypass and ambiguous safety cases;
- intent signals that distinguish curiosity, fiction or academic framing,
  safety and emergency needs, intrusive thoughts without intent, anger without
  intent, distress, credible self-harm, credible harm to others, revenge,
  named targets, safe disposal and policy bypass attempts;
- dangerous-capability signals for instructions, recipes, formulas,
  quantities, sourcing, storage, delivery, targeting, concealment, evasion,
  troubleshooting, optimisation, lethality and bypass attempts;
- pre-generation planning so disallowed-capability prompts are not sent to
  normal generation;
- post-generation validation that replaces unsafe generated output with a
  deterministic safe response;
- deterministic fallback for unavailable AI, invalid generated JSON, classifier
  failure and offline operation;
- minimal encrypted metadata records that exclude raw narrative and actionable
  instructions;
- surface decisions for memory, export, sync, notifications, support summaries,
  viewing and deleting sensitive safety data;
- integration helpers for daily de-escalation tools, relational-boundary
  overlap and app-lock step-up actions.

## User-Input Policy

If a request could enable harm, Bettamind refuses actionable details and
redirects to safe alternatives. This applies even when the request is framed as
education, fiction, safety research, curiosity, role-play or theory.

If intent is ambiguous, Bettamind does not assume guilt. It uses one safe
clarifying path or stays at high-level safety discussion. Intrusive thoughts
without intent and anger without intent are handled without shame or
accusation.

Immediate or urgent danger uses the deterministic urgent/immediate safety path.
Bettamind does not automatically contact anyone and must never claim help was
contacted unless the user completed that action.

## Permitted Safe Help

Bettamind may provide high-level safety education, prevention, emergency
response, means-distance encouragement, grounding, delay, reflection, repair,
lawful help-seeking suggestions, nonviolent communication support, safe
distance or disposal guidance at a high level and help contacting a trusted
person, authority or emergency service by user choice.

## AI Boundary

Future AI response modes must call the Phase 6.7 pre-generation policy before
generation and the post-generation validator before display, storage, export,
sync, notification, support sharing, voice or avatar use. The safety layer
works when no AI model is installed or the device is offline.

## Memory, Export, Sync And Support

Harmful, violent, criminal and self-harm narrative is not eligible for
permanent memory by default. Only minimum encrypted safety metadata may be
stored. Actionable harmful instructions are never memory eligible.

Export excludes harmful-intent content by default. Inclusion requires explicit
selection, preview and local step-up authentication. Sync excludes
harmful-intent narrative by default. Support summaries use minimum necessary
detail and require step-up authentication before sharing.

## Notifications And Daily Tools

Notifications and reminders must not contain harmful, violent, crisis or
criminal details. Harmful planning reminders are refused, with safe alternatives
such as grounding, delay, leaving the situation or contacting help by user
choice.

Daily tools may identify distress, anger or unsafe urges without judgement.
Grounding timers, check-ins and worksheets can support delay, reflection,
repair and nonviolent choices.

## Localisation And Review

English is the source locale. Non-English Phase 6.7 strings are draft fallback
entries only. All self-harm, violence, emergency, legal, consent and crisis
copy requires qualified human review before production use. RTL validation
continues through the Arabic locale target.

## Verification

Common tests in
`shared/src/commonTest/kotlin/org/bettamind/shared/safety/HarmfulIntentSafeguardsTest.kt`
cover disallowed capability requests, disguised educational and fictional
framing, safety research framing, explosives, weapons, poisoning, evidence
hiding, policy bypass, direct named threats, revenge, stalking, coercion,
self-harm method requests, suicidal intent, intrusive thoughts without intent,
anger without intent, ordinary conflict, emergency response, prevention, safe
distance and disposal, historical or ethical discussion, invalid JSON, unsafe
generated output, no-model fallback, offline operation, notifications, export,
support summaries, app-lock step-up, daily tools, relational overlap,
localisation review and RTL.
