# Requirements Traceability

| Requirement | Phase | Phase 1 handling |
| --- | --- | --- |
| Kotlin Multiplatform mobile stack | 1 | Create shared KMP module with Android and iOS targets. |
| Compose Multiplatform shared UI | 1 | Create shared Compose foundation and platform entry points. |
| Android local build | 1 | Add Android app module and debug build task. |
| Real iOS Xcode host project | 1 | Add `iosApp/iosApp.xcodeproj` and shared scheme. |
| Unsigned iOS simulator validation | 1 | Add Codemagic workflow that runs `xcodebuild`. |
| Optional FastAPI backend | 1 | Add backend skeleton that mobile startup does not require. |
| Core works without backend | 1+ | Keep no mobile dependency on backend. |
| Core works without AI | 1+ | Add AI interfaces only, no model logic or downloads. |
| No finished UI or branding assets | 2 | Phase 1 uses only a minimal foundation screen. |
| Logo pipeline | 2 | Document and keep placeholders only. |
| Global localisation architecture | 2+ | Add Compose resource folders and planning docs only. |
| Encrypted storage | 3 | Add interfaces only; no SQLCipher implementation. |
| Deterministic product engines | 4 | Not implemented in Phase 1. |
| Signed knowledge packs | 5 | Not implemented in Phase 1. |
| On-device AI model manager | 6 | Not implemented in Phase 1. |
| Safety and support bridge | 8 | Not implemented in Phase 1. |
| Optional encrypted sync | 9 | Backend skeleton only; no sync implementation. |

## Phase 1 acceptance criteria

- Gradle wrapper and version catalogue exist.
- Shared KMP module declares Android, iOS simulator and iOS device targets.
- Shared module includes common tests and platform adapter foundations.
- Android app target has display name Bettamind and starts without backend.
- `iosApp` is a valid Xcode project with a shared scheme.
- Codemagic unsigned workflow compiles shared tests, iOS targets and runs
  `xcodebuild` against the real Xcode project.
- GitHub Actions run non-Xcode checks.
- Optional FastAPI skeleton has health endpoint and test scaffold.
- Project memory is updated.
