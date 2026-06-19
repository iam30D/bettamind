# Local AI No-Model Fallback

Date updated: 2026-06-19

## Summary

`LocalAiRuntime` is an interface, not an AI model. Bettamind can run with
`UnavailableLocalAiRuntime`, which means no model weights are loaded and no
free-form generation happens. In that state, Bettamind remains useful through
deterministic Kotlin code, structured flows, local resources, signed knowledge
pack retrieval when packs exist, and local safety policies.

## What powers no-model mode

No-model mode is powered by:

- deterministic growth-flow state machines;
- daily-tool logic for check-ins, worksheets, timers, reminders and local
  trends;
- Compose resource strings and fallback identifiers;
- signed local knowledge-pack retrieval when user-approved content packs are
  installed;
- relational-boundary and harmful-intent policies that are model-free;
- encrypted local storage boundaries where persistence is enabled.

This mode does not infer new language from learned weights. It selects,
structures and routes approved local content and fallback responses according
to explicit product rules.

## What changes after a model is installed

When a signed, checksum-verified and user-approved model pack is installed,
`LiteRtLmRuntimeAdapter` can delegate to a platform LiteRT-LM bridge. Only then
can Bettamind produce model-generated text. Generated text still must pass
deterministic relational-boundary and harmful-intent validation before display,
storage, export, sync, notification, voice or avatar use.

If the model is missing, declined, removed, unavailable, malformed or unsafe,
Bettamind returns to deterministic fallback.

## Product boundary

Bettamind must never imply that no-model mode is AI-generated intelligence. The
correct product framing is:

- core Bettamind works without AI;
- optional local AI can add more flexible reflection after user approval;
- no cloud AI is required for core use;
- no model pack is downloaded, imported or activated automatically.
