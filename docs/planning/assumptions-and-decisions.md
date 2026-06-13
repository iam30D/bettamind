# Assumptions and Decisions

## Assumptions

- The definitive setup prompt is the controlling source because the cloned repo
  was empty.
- Android and iOS production identifiers have not been supplied yet.
- The owner will provide the canonical Bettamind logo before Phase 2.
- Codemagic will provide macOS, Xcode and network access for dependency
  resolution.
- Windows checks may be limited by local JDK, SDK and dependency availability.

## Decisions

- Use Kotlin 2.2.20, Compose Multiplatform 1.8.2, Android Gradle Plugin 8.11.1
  and Gradle 8.14.3 for Phase 1 because they are compatible with Xcode 16.4
  according to the Kotlin Multiplatform compatibility guidance.
- Use `brand/` as the canonical brand root from the definitive prompt.
- Use a single shared KMP module in Phase 1 and package-level architecture
  foundations inside it. More Gradle modules may be added when product features
  need stricter build boundaries.
- Keep placeholder bundle identifiers in configuration only so debug builds can
  compile before the owner confirms legal identifiers.
- Keep the FastAPI backend optional and independent from mobile startup.

## Deferred decisions

- Final Android application ID.
- Final iOS bundle identifier.
- Bettamind colour palette.
- Platform icons and generated brand assets.
- SQLCipher implementation details.
- Local AI runtime and model pack choices.
- Signed release and TestFlight configuration.
