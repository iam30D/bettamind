# Module Boundaries

## Phase 1 Gradle modules

- `:shared`: Kotlin Multiplatform library containing shared foundations,
  Compose UI foundation, interfaces and tests.
- `:androidApp`: Android application host that depends on `:shared`.
- `iosApp`: Xcode host project that embeds the shared Kotlin framework.
- `backend`: optional FastAPI skeleton outside the mobile build.

## Shared package boundaries

- `foundation`: platform information and app environment.
- `domain`: domain abstractions and use-case contracts.
- `data`: repository contracts and data boundaries.
- `security`: security interfaces only; no encryption implementation in Phase 1.
- `age`: age-assurance contracts only.
- `ai`: optional AI runtime contracts only.
- `design`: minimal Compose theme shell; full design system starts in Phase 2.
- `localization`: locale identifier contracts and resource structure.
- `feature`: placeholder feature shell contracts only.
- `di`: dependency registration foundations.

## Rules

- Feature code depends inward on domain contracts.
- Platform-specific code stays in Android/iOS source sets or host projects.
- Backend code is optional and must not be imported by mobile modules.
- Security implementations must not add unencrypted fallback storage.
