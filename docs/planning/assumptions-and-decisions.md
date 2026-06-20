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

## Deferred decisions

- Final Android application ID.
- Final iOS bundle identifier.
- Production local AI runtime build, exact model artifacts, model licences,
  trust anchors, signing keys, device-test results and delivery channels.
- Production offline speech adapters, OS voice support matrix, speech-pack
  artifacts, speech-pack licences, trust anchors, signing keys and device-test
  results.
- Signed release and TestFlight configuration.
