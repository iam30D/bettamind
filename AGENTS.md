# Bettamind repository instructions

## Read before working

Read completely:

1. `docs/product/bettamind-product-report.md`
2. `docs/specification/bettamind-locked-specification.md`
3. `docs/working-notes/project-memory.md`
4. `docs/planning/implementation-plan.md`, when it exists
5. `docs/planning/assumptions-and-decisions.md`, when it exists
6. `docs/planning/risk-register.md`, when it exists

## Durable project memory

Keep durable progress notes current in `docs/working-notes/project-memory.md`
after major repository changes so future Codex passes can resume quickly.

Project memory must contain the current phase, locked decisions, completed
work, important files, commands that pass, unresolved blockers, manual owner
actions and the next approved task. Never place secrets, real user content or
long raw logs in it.

## Product identity and brand

- Product name: Bettamind.
- Canonical logo: `brand/source/bettamind-logo-master.svg`.
- PNG fallback: `brand/source/bettamind-logo-master.png`.
- Never overwrite the canonical source.
- Generate platform icon and logo variants only from the approved source.
- Use Bettamind consistently in user-facing text.

## Product rules

- Core use is offline.
- Core use requires no account.
- Core use remains useful without AI.
- The backend is optional.
- Personal content never leaves the device without specific consent.
- Permanent memory requires separate approval.
- The MVP is for adults aged 18 and above.
- Do not store exact dates of birth or request identity documents in the MVP.
- No advertising, public feed, public ranking, human-worth score or manipulative streak.
- Do not present Bettamind as therapy, medical diagnosis, legal advice, financial advice or an emergency service.
- Never claim that help was contacted unless the user completed that action.

## Global localisation

- No hardcoded user-facing strings in feature code.
- Use BCP 47 locale identifiers.
- Support RTL layout and locale-aware dates, numbers and plurals.
- English is the source locale.
- Translation drafts are not production-approved until reviewed.
- Safety, crisis, legal, privacy and consent content requires qualified human review for each production locale.

## Engineering rules

- Work on one approved phase at a time.
- Inspect existing files before editing.
- Do not silently replace the locked stack.
- Do not add cloud AI to core functionality.
- Do not create unencrypted fallback storage.
- Do not make the backend mandatory.
- Do not automatically download models.
- Never commit secrets, signing files, certificates, model weights, databases or real personal content.
- Never weaken tests to make a phase pass.
- Stop and report security, privacy, encryption, localisation, iOS or model feasibility blockers.
- Do not automatically begin the next phase.

## Windows and iOS workflow

- Windows is the primary development environment.
- Android builds and tests run locally where possible.
- iOS must compile through Xcode on Codemagic macOS.
- Successful Kotlin compilation alone does not prove iOS compatibility.
- Maintain a valid minimal `iosApp` Xcode project.
- Keep native integrations behind shared interfaces and platform adapters.
- Store Apple signing material only in Codemagic secure credentials.

## Required verification after implementation

- run unit and integration tests;
- run linting and static analysis;
- build Android;
- run Codemagic iOS validation when shared or iOS code changes;
- review brand consistency;
- review localisation;
- review privacy, safety and accessibility;
- update planning documents;
- update `docs/working-notes/project-memory.md`;
- report changed files, commands, results, risks and owner actions.
