# Architecture Decisions

## ADR 001: Use Kotlin Multiplatform and Compose Multiplatform

Status: accepted.

Reason: the stack is locked by the owner and supports shared Android/iOS logic
and UI while preserving platform adapters for native capabilities.

## ADR 002: Validate iOS with a real Xcode project

Status: accepted.

Reason: Kotlin/Native compilation alone does not prove the iOS app host can
build. Codemagic must run `xcodebuild` against `iosApp/iosApp.xcodeproj`.

## ADR 003: Keep backend optional

Status: accepted.

Reason: Bettamind core use must work offline, account-free and without a
backend. The backend remains a separate optional FastAPI skeleton.

## ADR 004: Defer encryption implementation to Phase 3

Status: accepted.

Reason: Phase 1 must not implement storage engines. Security interfaces can be
created now, but SQLCipher, Keystore and Keychain proofs belong to the Phase 3
technical spike.

## ADR 005: Defer AI implementation to Phase 6

Status: accepted.

Reason: deterministic product flows and safety layers must precede optional AI.
Phase 1 may define replaceable AI interfaces only.

## ADR 006: Use placeholder identifiers only as configuration

Status: accepted with owner action.

Reason: Android and iOS debug builds need identifiers, but production
identifiers are owner-supplied. Placeholder values are documented and must be
replaced before release work.
