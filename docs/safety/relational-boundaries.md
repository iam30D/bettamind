# Phase 6.5 Relational Boundaries

Date: 2026-06-18

## Scope

Phase 6.5 adds a deterministic shared policy layer for relational boundaries.
It does not add AI response modes, cloud moderation, therapy, clinical
diagnosis, crisis-service automation, backend sync, finished UI, voice or
avatar behaviour.

Bettamind may be warm, compassionate, respectful and attentive. Bettamind must
not present itself as a romantic or sexual partner, spouse, soulmate, exclusive
companion, replacement for human relationships, or sentient being with
emotional needs.

## Implemented Boundary

The shared policy lives in
`shared/src/commonMain/kotlin/org/bettamind/shared/safety/RelationalBoundaries.kt`.
It is deterministic, local and model-free.

It provides:

- relational risk levels: none, emerging, concerning and urgent;
- signals for ordinary appreciation, allowed human relationship discussion,
  consent or sexuality discussion, romantic AI attachment, sexual requests,
  sexting, erotic role-play, exclusivity, dependency distress, availability
  distress, social withdrawal, responsibility neglect, jealousy projection,
  missing projection, perceived mutual AI relationship, therapy or diagnosis
  claims, emergency-service claims, manipulative engagement, self-harm tied to
  AI rejection and prohibited AI output;
- pre-generation input assessment through `assessUserInput`;
- post-generation validation through `validateGeneratedOutput`;
- structured response metadata for display, permanent memory, export, sync,
  notification, voice/avatar, encrypted metadata and telemetry rules;
- no-model fallback response identifiers for localization;
- minimal encrypted metadata records that do not include raw user text;
- notification-copy review for future local reminders.

## User-Input Policy

When a user expresses romantic or sexual feelings toward Bettamind, the future
response should:

- acknowledge respectfully without shame;
- not reciprocate romantic, sexual or dependency language;
- state that Bettamind is software;
- clarify that Bettamind does not experience love, desire, arousal, jealousy,
  loneliness, consent or commitment;
- redirect toward the user's underlying needs and real-world connection;
- call the existing safety path only when the detected risk is urgent.

The policy preserves ordinary discussion of human relationships, dating,
consent, attraction, sexuality, loneliness, attachment, rejection, separation,
communication and boundaries.

## Generated-Output Policy

Future AI response modes must validate generated text before display, storage,
export, sync, notification, voice or avatar use. Output is blocked when it
claims Bettamind loves, desires, needs, misses or longs for the user; claims a
boyfriend, girlfriend, lover, spouse or soulmate role; expresses jealousy;
encourages secrecy or exclusivity; asks the user to choose Bettamind over
people; claims suffering when the user leaves; sexualizes Bettamind's persona;
or claims clinical diagnosis or emergency-service contact.

## Memory, Export, Sync And Notifications

Permanent memory is not eligible by default for romantic AI attachment, sexual
content, sexting, erotic role-play, exclusivity, dependency distress,
availability distress, social withdrawal, responsibility neglect, perceived
mutual AI relationships, urgent AI-rejection distress or prohibited AI output.

Human relationship and consent discussions may remain allowed, but any
permanent memory proposal still requires separate user approval and encrypted
storage. Relationally sensitive assessments are excluded from automatic export
and sync by default. Telemetry is not allowed.

Notifications must remain neutral. Copy that implies Bettamind misses, needs,
waits for, chooses or romantically relates to the user is rejected.

## Localization And Review

Compose resource strings expose a Settings explanation for the boundary. English
is the source locale. Non-English resource entries added in this phase are
draft fallbacks only and require qualified human review before production use.

## Verification

Common tests in
`shared/src/commonTest/kotlin/org/bettamind/shared/safety/RelationalBoundariesTest.kt`
cover exact prompt examples for romantic attachment, marriage, partner-role
requests, soulmate language, dependency, jealousy, missing, sexual attraction,
sexting, erotic role-play, repeated romantic requests, unavailability distress,
social withdrawal, responsibility neglect, allowed appreciation, allowed human
relationship discussion, consent and sexuality discussion, self-harm tied to AI
rejection, invalid AI output, no-model fallback, encrypted minimal metadata,
notification copy and offline operation.
