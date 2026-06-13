# Risk Register

| Risk | Impact | Mitigation | Status |
| --- | --- | --- | --- |
| Empty remote repository lacked approved docs | Phase 1 could drift from plan | Reconstruct Phase 0 docs from definitive owner prompt | Mitigated |
| Local Java not on `PATH` | Windows Gradle checks may fail | Checks use Android Studio bundled JBR via `JAVA_HOME` | Mitigated |
| No canonical SVG logo in repo | Phase 2 brand work may rely on PNG fallback | Owner must confirm PNG fallback or add SVG under `brand/source/` | Open |
| iOS cannot be fully checked on Windows | Invalid Xcode project or native iOS dependency issue could go unnoticed locally | Codemagic runs real `xcodebuild` simulator build | Open for each shared/iOS change |
| Placeholder bundle IDs | Release identifiers could be wrong | Keep placeholders non-production and document owner action | Open |
| Dependency compatibility drift | CI may fail after tool updates | Use documented compatible Kotlin, Gradle, AGP and Xcode versions | Open |
| Backend accidentally becomes mandatory | Violates offline/account-free rule | Keep mobile modules independent of backend | Mitigated in Phase 1 design |
| Encryption implemented incorrectly | Privacy failure | Phase 3 keeps storage behind explicit contracts, uses Android SQLCipher only, and forbids unencrypted fallback storage | Active |
| iOS SQLCipher dependency route unresolved | Phase 3 cannot prove encrypted SQLite on iOS | Select a maintained native SQLCipher package/build route, implement the iOS storage adapter, then validate through Codemagic `ios-simulator-unsigned` | Open blocker |
| AI added before deterministic flows | Product and safety risk | Phase 1 creates interfaces only; AI starts at Phase 6 | Deferred |
| Android SDK missing on Windows | Android build could not run locally | Installed command-line tools, platform-tools, Android 36 platform and build-tools 36.0.0 | Mitigated |
