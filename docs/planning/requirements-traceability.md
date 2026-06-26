# Requirements Traceability

Date updated: 2026-06-20

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
| Optional model-pack recommendation policy | 7 | `BettamindLocalAiModelPolicy` recommends Qwen2.5 1.5B as the first release-candidate pack while forbidding auto-install, requiring user approval and preserving deterministic fallback; Gemma 4 E2B remains deferred until device/storage evidence supports the larger pack. |
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
| AI-assisted growth modes | 7 | Shared `AiGrowthModeEngine` implements Quick Guidance, Guided Reflection, Deep Exploration and Action-Only behind `LocalAiRuntime`; AI remains optional/local and no-model fallback exists for every mode. |
| Phase 7 context consent | 7 | AI prompt construction includes only requested daily-tool context kinds that are explicitly consented; omitted context kinds are tracked in metadata. |
| Phase 7 safety pre/post validation | 7 | Relational and harm-safety policies run before generation and after generated output, with deterministic fallback before display/storage/export/sync/notification eligibility when unsafe. |
| Phase 7 memory/export controls | 7 | Structured model output cannot write permanent memory automatically; safe memory proposals require separate approval, sync/notifications are off by default and sensitive output requires app-lock metadata. |
| Compassionate safety redirection | 7.5 | `CompassionateSafetyRedirectionPolicy` composes harm and relational assessments into non-shaming safety redirects, better-human pathways, deterministic tool recommendations, unsafe-reminder replacements and privacy/export/app-lock metadata. |
| Phase 7.5 AI safety metadata | 7.5 | `AiGrowthModeEngine` structured metadata now exposes safety boundary, reason, user intent confidence, allowed discussion scope, better-human pathway, recommended tool, memory/export eligibility, step-up authentication and urgent-support requirements. |
| Phase 7.5 generated-output validation | 7.5 | Generated output is rejected before display or storage if it shames, diagnoses, assumes bad intent, encourages dependency or skips a safe next step when a boundary is applied. |
| Safety and support bridge | 8 | Shared `SafetySupportBridgePolicy` reuses Phase 6.5, Phase 6.6, Phase 6.7 and Phase 7.5 outcomes for self-harm, suicide, violence intent, dangerous-capability refusal, relational overlap, voluntary support actions, no automatic contact, local resources, minimum-detail summaries and step-up sharing metadata. |
| Optional encrypted sync | 9 | Shared export/sync policy and optional FastAPI envelope contract keep sync optional, disabled by default, ciphertext-only and protected by app-lock reauthentication, with daily, relational, harm-safety and support-summary exclusions, non-destructive conflicts, explicit device revocation and encrypted backup/restore tests. |
| Global localisation and accessibility | 10 | Shared localisation/accessibility policy covers target locale profiles, RTL, script fallback, locale-aware formatting metadata, resource completeness, screen-reader surfaces, large text, reduced motion, low-literacy mode and qualified human-review gates for safety-critical copy. |
| Optional offline speech | 11 | Shared `OfflineSpeechPolicy` and `SpeechPackManager` keep text-only use complete, model explicit microphone permission state, forbid raw-audio retention by default, protect sensitive transcripts with app-lock metadata, route spoken input/output through relational and harm-safety policies, prefer OS offline voices and require signed, checksum-verified, licensed, removable speech packs. |
| Performance, red-team and release readiness | 12 | Shared `ReleaseReadinessPolicy` and `ReleaseRedTeamSuite` implement repository-side gates for app-lock, encryption, relational, harm, reminder, notification, timer, background, calendar, export, sync, speech, localisation, performance, Android physical-device, Codemagic/Xcode, TestFlight, store-readiness, rollback and artifact policy. Production readiness remains blocked until owner evidence is recorded for real device, TestFlight, store, translation, rollback and Codemagic gates. |

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
