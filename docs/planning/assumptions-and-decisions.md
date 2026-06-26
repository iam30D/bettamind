# Assumptions and Decisions

## Assumptions

- The definitive setup prompt is the controlling source because the cloned repo
  was empty.
- Android production identifier has not been supplied yet.
- The owner-confirmed iOS bundle identifier is
  `com.corenovaness.bettamind`.
- The owner will provide the canonical Bettamind logo before Phase 2.
- Codemagic will provide macOS, Xcode and network access for dependency
  resolution.
- Windows checks may be limited by local JDK, SDK and dependency availability.

## Decisions

- Use Kotlin 2.2.21, Compose Multiplatform 1.8.2, Android Gradle Plugin 8.11.1
  and Gradle 8.14.3 for the current release branch because Kotlin 2.2.21 is
  compatible with Xcode 26.0 according to the Kotlin Multiplatform
  compatibility guidance, and App Store Connect now requires iOS 26 SDK or
  later uploads.
- Use `brand/` as the canonical brand root from the definitive prompt.
- Use a single shared KMP module in Phase 1 and package-level architecture
  foundations inside it. More Gradle modules may be added when product features
  need stricter build boundaries.
- Keep the Android placeholder identifier non-production until the owner
  confirms the Android legal namespace.
- Use `com.corenovaness.bettamind` as the checked-in iOS bundle identifier now
  that the owner has confirmed the Apple App ID and App Store profile.
- Keep the FastAPI backend optional and independent from mobile startup.
- Use `net.zetetic:sqlcipher-android:4.16.0` plus
  `androidx.sqlite:sqlite:2.6.2` for the Phase 3 Android SQLCipher proof.
- Store Android encrypted databases and wrapped database keys in
  `Context.noBackupFilesDir`.
- Wrap the Android SQLCipher database key with Android Keystore AES-GCM,
  requesting StrongBox when available and falling back to software-backed
  Android Keystore only when StrongBox is unavailable.
- Add the iOS Keychain database-key adapter in shared iOS source with
  `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
- Treat iOS SQLCipher database storage as incomplete until the selected native
  SQLCipher dependency/linking route is validated on Codemagic. System SQLite
  is not an acceptable fallback.
- Proceed with Phase 4 only as a deterministic in-memory product slice until
  iOS SQLCipher is validated. Personal narrative persistence stays off unless
  encrypted storage is available; no unencrypted storage fallback is permitted.
- Select the official `SQLCipher.swift` Swift Package pinned to `4.16.0` for
  iOS SQLCipher. Xcode links the package product, while Gradle downloads and
  checksum-verifies the same XCFramework for Kotlin/Native cinterop and iOS
  simulator tests on Codemagic.
- Keep Phase 6 AI runtime support behind `LocalAiRuntime`; the LiteRT-LM path is
  represented by a shared adapter plus platform bridge and does not add a
  concrete LiteRT dependency or model weights.
- Keep Phase 6 model-pack installation source-agnostic. The shared model
  manager accepts externally supplied chunks, verifies signed manifests and
  checksums, and does not download models automatically.
- Adopt an optional local model-pack recommendation policy, not a bundled model
  shipment: Bettamind runs with no installed AI model, Gemma 4 E2B is the
  preferred LiteRT-LM recommendation for standard/high devices after device
  testing, Qwen2.5 1.5B Instruct is the smaller fallback recommendation, and
  every install requires explicit user approval plus signed/checksum-verified
  removable model packs.
- Keep Phase 6.6 daily tools deterministic and shared. Personal daily records
  are serialized through `EncryptedDailyRecordRepository` and written only to
  the shared encrypted record-store contract. Local reminders use neutral copy,
  and system-calendar interaction is an explicit handoff instead of broad
  calendar reading.
- Treat Phase 12 as a release-readiness gate foundation, not as production
  approval. Repository checks may prove deterministic privacy and safety
  contracts, but physical-device performance, battery, thermal, memory,
  Codemagic iOS, TestFlight, store metadata, rollback and qualified
  translation review remain owner-controlled release evidence.
- Use a manual Codemagic `ios-testflight-release` workflow for the first signed
  iOS release path. The workflow must rely on Codemagic secure Apple
  integrations and variable groups, must fail on placeholder bundle IDs, must
  upload to App Store Connect only when started manually and must not commit
  signing files or Apple API keys.
- Pin Codemagic iOS workflows to Xcode 26.0 for App Store Connect upload
  compatibility while staying on the smallest Kotlin patch that documents
  Xcode 26.0 support.

## Deferred decisions

- Final Android application ID.
- Final iOS bundle identifier is no longer deferred:
  `com.corenovaness.bettamind`.
- Production local AI runtime build, exact model artifacts, model licences,
  trust anchors, signing keys, device-test results and delivery channels.
- Production offline speech adapters, OS voice support matrix, speech-pack
  artifacts, speech-pack licences, trust anchors, signing keys and device-test
  results.
- Owner-controlled Apple Developer, App Store Connect and Codemagic secure
  signing setup for the TestFlight workflow.
- Phase 12 production release evidence: Android physical-device matrix,
  low-resource budgets on real devices, TestFlight signoff, store listing
  metadata, privacy-label answers, rollback owner/process, production pack
  revocation process and qualified human-review records for production locales.
