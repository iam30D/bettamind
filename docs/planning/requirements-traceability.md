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
| Relational boundaries before AI response modes | 6.5 | Missing; Phase 6X audit recommends deterministic policy contracts before Phase 7. |
| Memory/export/sync/notification/voice boundary rules | 6.5+ | Missing; should be defined before those surfaces use AI or personal content. |
| Deterministic daily tools | 6.6 | Missing beyond the Phase 4 growth-flow skeleton; check-ins, timers, reminders, calendar, worksheets and trend summaries are planned. |
| Encrypted product-record persistence | 6.6 | Storage primitive exists, but daily-tool product records are not wired yet. |
| Safety and support bridge | 8 | Not implemented; should reuse Phase 6.5 policy outcomes. |
| Optional encrypted sync | 9 | Backend skeleton only; no sync implementation. |

## Phase 6X Audit Acceptance Criteria

- Current implementation plan archived unchanged.
- Existing app, storage, AI, growth, safety and daily-tool functions inventoried.
- Phase 6.4 implementation status reflected after app privacy-lock work.
- Roadmap amendment created without editing active implementation plan.
- Risk register and project memory updated.
- No production source code edited.
