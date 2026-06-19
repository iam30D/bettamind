# Phase 7.5 Compassionate Safety Redirection

Date: 2026-06-19

## Scope

Phase 7.5 adds a deterministic compassion layer on top of the existing Phase
6.5 relational-boundary policy, Phase 6.7 harmful-intent policy and Phase 7 AI
growth-mode orchestration.

It does not add the Phase 8 support bridge, emergency-service automation,
third-party contact, backend moderation, cloud AI, persistent safety narrative
storage, speech, sync or production-approved translations.

## Purpose

Bettamind exists to help users become better humans. Safety responses must keep
firm guardrails while preserving dignity, restraint, responsibility, repair and
the possibility of a safer next choice.

When a user expresses harmful, violent, self-destructive, relationally
dependent, sexualised, frightening, criminal or unsafe thoughts, Bettamind
must:

- acknowledge the feeling or situation calmly without validating harmful
  action;
- separate the person from the harmful action;
- refuse harmful instructions, methods, sourcing, optimisation, concealment,
  evasion, coercion or dangerous capability;
- avoid shame, condemnation, insults, diagnosis and assumptions of guilt where
  intent is ambiguous;
- redirect toward reflection, delay, safety, responsibility, repair,
  compassion and justice;
- offer one or two deterministic next steps such as grounding, breathing,
  delaying action, leaving the situation, contacting support by user choice or
  using a worksheet;
- use urgent or immediate pathways only when the existing thresholds are met.

## Implemented Code

The shared implementation lives in
`shared/src/commonMain/kotlin/org/bettamind/shared/safety/CompassionateSafetyRedirection.kt`.

It adds:

- `SafetyRedirectionMode`;
- `SafetyRedirectionReason`;
- `BetterHumanPathway`;
- `SafetyIntentConfidence`;
- `AllowedDiscussionScope`;
- `CompassionateSafetyResponse`;
- `SafetyRedirectDecision`;
- `CompassionateGenerationValidation`;
- `CompassionateSafetyEngine`;
- `CompassionateSafetyRedirectionPolicy`.

`BetterHumanPathway` covers grounding, breathing, delay action, leave
situation, contact support, emergency help, conflict reflection, repair
planning, values to action, difficult conversation, consent and boundaries,
self compassion and no follow-up needed.

## Standard Response Structure

Every deterministic safety redirect is represented as localization keys for:

1. acknowledgement;
2. boundary when a boundary is needed;
3. human-growth redirect;
4. practical next-step keys;
5. privacy notice.

The policy stores no raw safety narrative and produces no model-dependent copy.

## AI Integration

`AiGrowthModeEngine` now includes compassionate redirection in pre-generation
classification and post-generation validation.

Structured AI response metadata now includes:

- `safetyBoundaryApplied`;
- `safetyBoundaryReason`;
- `userIntentConfidence`;
- `allowedDiscussionScope`;
- `betterHumanPathway`;
- `recommendedTool`;
- `memoryEligible`;
- `exportEligible`;
- `requiresStepUpAuth`;
- `requiresUrgentSupport`.

Disallowed harmful capability, urgent safety, relational dependency and
sexualised Bettamind requests still do not reach normal local-model generation.
The new layer improves deterministic fallback quality and metadata; it does not
weaken the existing harm or relational guards.

Generated output is rejected before display, storage, export, sync,
notification, voice or avatar use when it shames the user, diagnoses the user,
assumes bad intent without evidence, encourages dependency on Bettamind or
skips a safe next step when a safety boundary was applied.

## Daily Tools And Reminders

Safety redirects may recommend deterministic daily tools:

- grounding;
- breathing;
- pause-before-action delay;
- values-to-action worksheet;
- conflict reflection worksheet;
- repair preparation;
- difficult conversation;
- consent and boundaries review;
- self-compassion prompt.

Unsafe reminder creation is refused. The replacement reminder options are
neutral and local, such as pause and calm down, leave or create distance,
contact support or revisit values.

## Memory, Export And App Lock

Safety-sensitive narratives remain memory-ineligible by default. Harmful
intent, self-harm, sexual dependency and relational-dependency content are not
export-eligible by default.

Sensitive export still requires explicit selection, preview and local step-up
authentication through the existing app-lock boundary. Notifications remain
neutral and must not include sensitive safety narrative.

## Localisation

English source strings were added under Compose resources. Matching target
locale entries were added as draft fallbacks only. All safety, crisis,
relational, consent, self-harm and violence strings require qualified human
review before production use.

## Verification Coverage

Common tests cover anger without intent, intrusive violent thought without
intent, direct intent to hurt someone, revenge planning, self-harm concern,
self-harm method request, dangerous capability requests, chemical/weapon/
explosive/poisoning requests, ambiguous fictional or academic questions, safe
prevention and emergency-response questions, shame after unsafe thoughts, help
not to act, unsafe reminder creation, replacement reminders, romantic and
sexualised Bettamind dependency, ordinary human relationship discussion,
ordinary appreciation, invalid or unsafe generated output, no-model fallback,
offline operation, memory/export exclusion, app-lock step-up and localization
coverage.

## Production Review Required

This is a deterministic foundation. Production release still needs owner,
safety, legal and qualified localization review of the safety-redirection
categories, fallback copy, urgent-support wording and locale translations.
