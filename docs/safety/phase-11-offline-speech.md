# Phase 11 Offline Speech

## Scope

Phase 11 adds the shared policy foundation for optional offline
speech-to-text and text-to-speech. It does not ship a speech engine, microphone
integration, speech model artifact, cloud speech service, automatic speech-pack
download or production voice pack.

Text-only Bettamind remains complete. Voice input and voice output are optional
interfaces that must route through the same deterministic text, relational and
harm-safety policies before user-visible output, storage, export, sync,
notification, support sharing, voice or avatar use.

## Implemented Controls

- `OfflineSpeechPolicy` keeps text-only fallback available for every speech
  path.
- Microphone use is represented as explicit, permission-scoped state.
- Raw audio retention is disallowed by policy by default.
- Sensitive transcripts are excluded from default storage and require local
  app-lock step-up metadata.
- Spoken input is converted into the existing text safety pipeline before
  normal generation or deterministic fallback decisions.
- Spoken output is blocked if relational, seductive, possessive, manipulative
  or harmful-output checks fail.
- OS offline voices are preferred before local text-to-speech packs.
- `SpeechPackManager` requires user approval, publisher licence approval,
  approved licence identifiers, Ed25519-labeled signatures, SHA-256 artifact
  checksums, monotonic versions, revocation policy and removability.

## Non-Goals

- No raw audio is stored.
- No cloud speech is added.
- No speech packs, model weights, voices or binary artifacts are committed.
- No microphone permission is requested automatically.
- No voice persona is allowed to sound romantic, sexual, possessive,
  dependency-building or manipulative.
- No Phase 12 performance, red-team, TestFlight or release-readiness work is
  started.

## Production Requirements

Before enabling real speech in production, the owner must provide or approve:

- platform microphone permission copy and store privacy labels;
- platform offline speech adapter validation on Android and iOS;
- OS voice availability review for target locales;
- licences and publisher approval records for any third-party speech pack;
- signed manifest, checksum, size, version and revocation records for every
  speech pack artifact;
- accessibility review for users who cannot or do not want to use speech;
- qualified human review for speech, microphone, privacy, consent, safety and
  support translations.

## Verification

Common tests cover text-only fallback, explicit microphone permission, no raw
audio retention, sensitive transcript app-lock requirements, relational and
harm-safety handling for spoken input, spoken-output persona restrictions, OS
offline voice preference and signed removable speech-pack validation.
