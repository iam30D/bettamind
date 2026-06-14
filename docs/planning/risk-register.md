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
| Encryption implemented incorrectly | Privacy failure | Phase 3 keeps storage behind explicit contracts, uses Android/iOS SQLCipher, Android Keystore, iOS Keychain and forbids unencrypted fallback storage | Mitigated for Phase 3 spike |
| iOS SQLCipher dependency route, native link or app-hosted Keychain validation fails on macOS | Phase 3 cannot prove encrypted SQLite on iOS | Use official `SQLCipher.swift` pinned to `4.16.0`, checksum-verify the XCFramework for Gradle cinterop, link the package in Xcode, and validate SQLCipher plus app-hosted Keychain through Codemagic `ios-simulator-unsigned` | Mitigated after owner-confirmed Codemagic pass |
| Phase 4 flow appears to store personal narrative before encrypted storage is ready | Privacy and trust failure | Keep Phase 4 deterministic and in-memory only until a separate approved pass wires encrypted persistence; no unencrypted fallback exists | Mitigated |
| Production knowledge-pack trust roots are not supplied | Unsigned or incorrectly trusted content could be accepted in release builds | Phase 5 enforces signed manifests, checksums, rollback/revocation policy and a verifier boundary; release work must add owner-approved Ed25519 trust anchors | Open |
| AI added before deterministic flows | Product and safety risk | Phase 6 started only after deterministic Phase 4 and signed-pack Phase 5 validation; Phase 6 adds optional runtime/model interfaces only | Mitigated |
| Model weights accidentally committed or downloaded automatically | Repository bloat, licence breach or privacy/safety risk | Phase 6 adds no model files and no downloader; model installation accepts externally supplied signed chunks only | Mitigated for Phase 6 |
| Production model-pack trust roots are not supplied | Untrusted model packs could be accepted in release builds | Phase 6 enforces signed manifests, SHA-256 checksums, rollback/revocation policy and a verifier boundary; release work must add owner-approved Ed25519 trust anchors | Open |
| Android SDK missing on Windows | Android build could not run locally | Installed command-line tools, platform-tools, Android 36 platform and build-tools 36.0.0 | Mitigated |
