# Requirements Traceability

Date updated: 2026-06-18

| Requirement | Phase | Current handling |
| --- | --- | --- |
| Kotlin Multiplatform mobile stack | 1 | Shared KMP module with Android and iOS targets exists. |
| Compose Multiplatform shared UI | 1 | Shared Compose foundation and platform entry points exist. |
| Android local build | 1 | Android app module and Gradle checks exist. |
| Real iOS Xcode host project | 1 | `iosApp/iosApp.xcodeproj` exists and is validated through Codemagic. |
| Unsigned iOS simulator validation | 1+ | Codemagic `ios-simulator-unsigned` runs real `xcodebuild`; still required after shared/iOS-affecting commits. |
| Optional FastAPI backend | 1 | Backend health skeleton exists and mobile startup does not require it. |
| Core works without backend | 1+ | Mobile modules remain independent of backend. |
| Core works without AI | 1+ | Deterministic flow, storage, packs and app shell work without AI; AI runtime is optional. |
| No cloud AI in core | 1+ | No cloud AI integration exists. |
| Logo pipeline | 2 | Brand assets are generated from approved local source; canonical SVG is still preferred when available. |
| Global localization architecture | 2+ | Compose resource folders, BCP 47 planning, RTL detection and bundled fonts exist; translations are draft until reviewed. |
| Encrypted local storage | 3 | Shared contracts, Android SQLCipher/Keystore, iOS SQLCipher/Keychain and app-hosted iOS validation exist; no unencrypted fallback. |
| Deterministic growth engine | 4 | Fixed growth-flow sequence exists and remains in-memory for narrative content. |
| Adult-only MVP gate | 4 | Self-declared local adult gate exists; no date of birth or identity document is requested. |
| Signed knowledge packs | 5 | Shared installer verifies signed manifests, SHA-256 checksums, rollback/replay and revocation policy. |
| Local knowledge retrieval | 5 | Shared in-memory retriever searches installed packs offline without backend or AI. |
| On-device AI runtime abstraction | 6 | `LocalAiRuntime`, unavailable runtime and LiteRT bridge boundary exist. |
| On-device AI model manager | 6 | `ModelPackManager` verifies signed, checksum-checked, resumable and removable model packs without committing weights. |
| App privacy lock | 6.4 | Shared lock policy, timeout settings, Settings copy and key-release gate implemented. |
| Authentication-bound storage key release | 6.4 | `VaultKeyReleaseService` calls the platform storage key manager only after successful local authentication; Android and iOS platform authentication adapters exist. |
| Bettamind PIN/passphrase fallback | 6.4 | Shared policy, verifier and rate limiter implemented behind a KDF interface; production Argon2id provider remains a release-hardening dependency. |
| Background/app-switcher privacy protection | 6.4 | Android sets `FLAG_SECURE`; iOS covers inactive app content with a neutral system-background privacy shield. |
| Relational boundaries before AI response modes | 6.5 | Shared deterministic policy contracts, pre-generation assessment, post-generation validation, fallback identifiers and tests exist under `shared/src/commonMain/kotlin/org/bettamind/shared/safety/`. |
| Memory/export/sync/notification/voice boundary rules | 6.5+ | Phase 6.5 policy metadata and surface decisions define default exclusions for relationally sensitive memory, export, sync, notification and voice/avatar use. |
| Deterministic daily tools | 6.6 | Shared daily foundation exists for check-ins, breathing/grounding, timers, reminders, private calendar handoff, worksheets and local trend summaries. |
| Encrypted product-record persistence | 6.6 | Daily records are serialized through `EncryptedDailyRecordRepository`, which writes only to the shared `EncryptedRecordStore` contract and has no unencrypted fallback. |
| Harmful-intent and dangerous-capability safeguards | 6.7 | Shared deterministic harm-safety policy handles pre-generation, post-generation, no-model fallback, memory/export/sync/notification/support decisions, daily tools, relational overlap and app-lock step-up rules. |
| Roadmap reconciliation after Phase 6X | 6X | Phase 7 through Phase 12 objectives are preserved and amended in `implementation-plan.md`, `roadmap-amendment-phase-6x.md` and `phase-7-to-12-continuation-plan.md`; no production code changed. |
| AI-assisted growth modes | 7 | Planned only; must keep AI optional/local, preserve no-model fallback, require app-lock step-up for sensitive context, require consent before daily-tool context, enforce relational and harm-safety pre/post-generation checks, use structured schemas and avoid cloud AI. |
| Safety and support bridge | 8 | Planned only; should reuse Phase 6.5, Phase 6.6 and Phase 6.7 outcomes for self-harm, violence intent, dangerous-capability refusal, voluntary support, no automatic contact, minimum support summaries and step-up sharing. |
| Optional encrypted sync | 9 | Backend skeleton only; future sync must remain optional, disabled by default, ciphertext-only and protected by app-lock reauthentication, with daily, relational and harm-safety export exclusions. |
| Global localisation and accessibility | 10 | Planned only; must include app-lock, relational, harm-safety, daily-tool and support surfaces, with RTL, screen-reader, large-text, reduced-motion, low-literacy and qualified human-review gates. |
| Optional offline speech | 11 | Planned only; text-only use must remain complete, microphone use optional, raw audio not retained by default and spoken input/output routed through the same relational and harm-safety policies as text. |
| Performance, red-team and release readiness | 12 | Planned only; must include app-lock, encryption, relational, harm, reminder, notification, timer, calendar, export, sync, low-resource, Android physical-device, Codemagic/Xcode, TestFlight and store-readiness gates. |

## Phase 6X Audit Acceptance Criteria

- Current implementation plan archived unchanged.
- Existing app, storage, AI, growth, safety and daily-tool functions inventoried.
- Phase 6.4 implementation status reflected after app privacy-lock work.
- Phase 6.5 implementation status reflected after relational-boundary work.
- Phase 6.6 implementation status reflected after deterministic daily-tool work.
- Phase 6.7 implementation status reflected after harmful-intent safeguard work.
- Roadmap amendment and active implementation plan reconciled after Phase 6X.
- Risk register and project memory updated.
- No production source code edited.
