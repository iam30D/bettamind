# Bettamind Project Memory

## Current phase

Phase 6X planning audit is complete locally as a documentation-only bridge
after owner-confirmed Codemagic success for Phase 6. Phases 0 through 6 are
treated as implemented and stable. Do not begin Phase 6.4, Phase 6.5, Phase 6.6
or Phase 7 until the owner explicitly approves the next implementation prompt.

## Locked decisions

- Mobile stack: Kotlin Multiplatform and Compose Multiplatform.
- Primary development environment: Windows.
- iOS validation: Codemagic macOS using the real `iosApp` Xcode project.
- Optional backend: FastAPI.
- Core mobile use remains offline, account-free, backend-independent and useful
  without AI.
- Phase 2 uses the available PNG fallback at
  `brand/source/bettamind-logo-master.png`; the preferred SVG source is still
  absent.
- Final Phase 2 palette is documented in
  `docs/design/brand-and-colour-decision.md`.
- Default UI font is bundled Noto Sans with script fallbacks; Atkinson
  Hyperlegible is bundled as an accessibility-oriented display option.
- Initial locale targets remain `en`, `fr`, `es`, `pt`, `ar`, `hi`,
  `zh-Hans`, `ha`, `yo` and `ig`.
- Phase 3 Android encrypted storage uses `net.zetetic:sqlcipher-android:4.16.0`
  and `androidx.sqlite:sqlite:2.6.2`.
- Android encrypted databases and wrapped keys live under
  `Context.noBackupFilesDir`.
- Android SQLCipher keys are wrapped with Android Keystore AES-GCM, requesting
  StrongBox when available.
- iOS SQLCipher route: official `SQLCipher.swift` Swift Package pinned to
  `4.16.0`, with Gradle checksum-verifying the same XCFramework for
  Kotlin/Native cinterop. Do not use system SQLite as an iOS SQLCipher
  substitute.
- Real iOS Keychain behaviour must be validated from an app-hosted simulator
  process. The standalone Kotlin/Native simulator test binary is not treated as
  the Keychain proof.
- Phase 4 deterministic growth flow is allowed to run without storage, but
  narrative persistence remains disabled until a separate approved pass wires
  encrypted persistence into the growth flow.
- Phase 4 adult gate is self-declared and records no exact date of birth or
  identity document.
- Phase 5 knowledge-pack manifests use SHA-256 payload checksums and an
  Ed25519-labeled signature verification boundary. Production content packs,
  signing keys and trust anchors are not committed.
- Phase 6 AI remains optional. `LocalAiRuntime` is the shared replaceable
  interface, `LiteRtLmRuntimeAdapter` delegates to a platform bridge, and no
  LiteRT dependency or model weights are committed.
- Phase 6 model-pack installation is source-agnostic and accepts externally
  supplied signed chunks only. No automatic model download exists.
- Phase 6X inserts three proposed phases before Phase 7: Phase 6.4 App Privacy
  Lock, Phase 6.5 Relational Boundaries and Phase 6.6 Deterministic Daily Tools.
- Phase 6.4 must be a real storage-key/privacy-lock implementation, not only a
  visual lock screen.
- Phase 6.5 must exist before Phase 7 AI response modes.
- Phase 6.6 must keep daily tools deterministic, offline-first and encrypted.

## Completed work

- Empty GitHub repository was cloned into the workspace.
- Baseline repository documentation and Phase 0 planning files were created.
- Phase 1 Kotlin Multiplatform, Compose Multiplatform, Android, iOS,
  optional FastAPI, GitHub Actions and Codemagic foundation was completed.
- GitHub Actions `gradlew` permission failure was repaired.
- Codemagic shared-test scope and Kotlin/Native `LocaleTag` compatibility were
  repaired; owner later confirmed Codemagic passed.
- Phase 2 source logo was inspected. The source PNG was not overwritten.
- `scripts/generate_brand_assets.py` now generates repeatable PNG-derived brand
  assets from the source logo.
- Android adaptive launcher foreground, background, monochrome and
  notification icons were generated.
- iOS `Assets.xcassets` was generated with complete AppIcon, mark and lockup
  image sets, and the Xcode project now includes the asset catalog.
- Brand masters were generated under `brand/generated/`.
- Noto Sans Variable, Noto Sans Arabic, Noto Sans Devanagari, Noto Sans SC and
  Atkinson Hyperlegible Regular/Bold were bundled with OFL licence files.
- Compose resources now include source English strings plus draft locale packs
  for all initial target locales.
- Shared app shell now has five placeholder primary destinations: Today,
  Reflect, Grow, Support and Settings.
- Settings placeholder exposes theme mode and readable-font display controls.
- Shared design tokens, Material colour schemes and typography foundations were
  added.
- Common tests now cover palette contrast, Phase 2 locale targets and Arabic
  RTL detection.
- Android backup and data extraction rules exclude app data in line with the
  offline/private product stance.
- Android compile/target SDK was updated to 36 locally and in GitHub Actions.
- Android lint now reports no issues.
- Backend dev dependencies now use `httpx2` for Starlette's test client path;
  pytest no longer emits the deprecated `httpx` warning.
- `AGENTS.md` now reminds future passes to ask the owner to run Codemagic
  `ios-simulator-unsigned` after pushed commits that affect shared/iOS/iOS
  workflow surfaces.
- Phase 3 shared encrypted-storage contract was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/privacy/`.
- Android SQLCipher encrypted record storage and Android Keystore key wrapping
  were added under `shared/src/androidMain/kotlin/org/bettamind/shared/privacy/`.
- iOS Keychain database-key management source was added under
  `shared/src/iosMain/kotlin/org/bettamind/shared/privacy/`.
- Common storage contract tests cover wrong-key rejection, key rotation,
  backup/restore and deletion with a test-only fake.
- Codemagic `:shared:iosSimulatorArm64Test` initially failed because
  `EncryptedStorageContractTest` used JVM-only `toSortedMap()` in common test
  code. The test now uses common Kotlin key sorting and unsigned byte
  formatting.
- `phaseThreeCheck` now runs Phase 1 mobile checks plus Android lint, and
  GitHub Actions now invokes `phaseThreeCheck`.
- `docs/security/phase-3-encrypted-storage-spike.md` documents the Android
  proof, iOS proof boundary and no-fallback rule.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for commit
  `06125cc` and approved proceeding to Phase 4.
- Phase 4 deterministic growth engine was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/growth/`.
- Common tests for the Phase 4 flow cover locked step order, unknown/minor
  blocking, adult-only entry, no narrative-storage fallback and completion.
- Compose app panels now expose the deterministic Today, Reflect, Grow and
  Support Phase 4 flow with adult gating and encrypted-storage availability
  status.
- Compose resources now include Phase 4 source English strings plus draft
  fallback entries in all initial locale packs. Non-English Phase 4 strings are
  implementation drafts and require human review.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for commit
  `536dd62`, then approved the iOS SQLCipher completion slice.
- Gradle now has `prepareSqlCipherIos`, which downloads
  `SQLCipher.xcframework.zip` for `SQLCipher.swift` `4.16.0`, verifies SHA-256
  `510fd00fa51fb017909a159bb1cc233b012e8ce18dc9c2f09014fe47f557c1a6`, and
  supplies headers/framework paths to Kotlin/Native cinterop.
- `shared/src/nativeInterop/cinterop/BettamindSqlCipher.def` and header wrapper
  expose SQLCipher `sqlite3.h` APIs to Kotlin/Native.
- `IosSqlCipherEncryptedRecordStore` was added under
  `shared/src/iosMain/kotlin/org/bettamind/shared/privacy/`.
- iOS integration tests were added under `shared/src/iosTest/kotlin/` for real
  SQLCipher store behaviour and Keychain key-manager replacement/deletion.
- `iosApp.xcodeproj` now links the pinned `SQLCipher.swift` package product,
  and Codemagic now resolves Swift packages before `xcodebuild`.
- Codemagic `ios-simulator-unsigned` for commit `7704ad7` failed in
  `:shared:cinteropBettamindSqlCipherIosSimulatorArm64` because cinterop could
  not find `BettamindSqlCipher.h`. The Gradle cinterop compiler options now add
  `shared/src/nativeInterop/cinterop` before the SQLCipher framework header
  path.
- Codemagic `ios-simulator-unsigned` for commit `2dffec6` advanced past
  `:shared:cinteropBettamindSqlCipherIosSimulatorArm64` and failed in
  `:shared:compileKotlinIosSimulatorArm64` because the iOS SQLCipher adapter
  used out-pointer allocation types that did not match the generated
  Kotlin/Native SQLCipher signatures, missed an `ExperimentalForeignApi` opt-in
  on `requireDone`, and called unavailable `NSNumber.numberWithBool`.
- `IosSqlCipherEncryptedRecordStore` now uses
  `CPointerVarOf<CPointer<...>>` for SQLCipher out pointers, marks
  `requireDone` with the required cinterop opt-in, and keeps iOS backup
  exclusion through `NSURL.setResourceValue(true, ...)`.
- Codemagic `ios-simulator-unsigned` for commit `31d5274` still failed in
  `:shared:compileKotlinIosSimulatorArm64`; the remaining errors showed the
  `CPointerVarOf<CPointer<...>>` allocation itself did not expose `ptr` or
  `value` under Kotlin/Native's typed cinterop API.
- A temporary local Kotlin/Native compiler probe confirmed the correct
  out-pointer pattern is `allocPointerTo<T>()` with explicit
  `kotlinx.cinterop.ptr` and `kotlinx.cinterop.value` imports. The iOS
  SQLCipher store now uses that pattern for `sqlite3**`, `sqlite3_stmt**` and
  `char**` SQLCipher out parameters.
- Codemagic `ios-simulator-unsigned` for commit `270ec88` advanced past the
  SQLCipher out-pointer errors and failed in
  `:shared:compileKotlinIosSimulatorArm64` at the iOS backup file read path:
  `output.size.convert()` had no concrete target type inside an equality
  comparison, so Kotlin/Native attempted to convert `Int` to `Any`.
- `IosSqlCipherEncryptedRecordStore` now converts `fread` and `fwrite` byte
  counts into explicit `platform.posix.size_t` variables before calling and
  comparing POSIX results.
- Codemagic `ios-simulator-unsigned` for commit `d99cf09` compiled, linked and
  started `:shared:iosSimulatorArm64Test`; 17 of 18 tests passed. The remaining
  failure was
  `IosEncryptedStorageIntegrationTest.keychainManagerStoresReplacesAndDeletesDatabaseKey`,
  where the real iOS Keychain adapter reported encrypted storage unavailable.
- `IosKeychainStorageKeyManager` now builds Security.framework queries with
  CoreFoundation type dictionary callbacks instead of null callbacks, sets
  `kSecUseDataProtectionKeychain` explicitly, and attaches OSStatus details as
  the cause for Keychain failures. This keeps the adapter fail-closed and does
  not add fallback key storage.
- The iOS Keychain integration test now wraps Keychain operations with a
  stricter diagnostic helper so any remaining simulator failure reports the
  underlying OSStatus cause instead of an opaque `StoreUnavailable`.
- Codemagic `ios-simulator-unsigned` for commit `064f77c` still failed in
  `:shared:iosSimulatorArm64Test` at the real Keychain manager test. The
  failure was isolated to the standalone Kotlin/Native simulator test process,
  not to SQLCipher compilation or SQLCipher database behaviour.
- `IosKeychainStorageKeyManager` no longer adds
  `kSecUseDataProtectionKeychain` to iOS queries. It keeps
  `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, CoreFoundation type
  dictionary callbacks and fail-closed OSStatus reporting.
- `runIosEncryptedStorageAppValidation()` was added under `iosMain` to exercise
  the real iOS Keychain manager together with the real SQLCipher record store
  from an app-hosted process.
- `iosApp` now runs that validation only when
  `BETTAMIND_IOS_STORAGE_VALIDATION=1` is supplied. It writes
  `bettamind-ios-storage-validation.txt` into the app temporary directory and
  exits with success only after a PASS result.
- The raw Kotlin/Native Keychain integration test is now explicitly ignored
  with a reason pointing to the app-hosted Codemagic validation path.
- Codemagic still runs `:shared:iosSimulatorArm64Test`, compiles iOS targets and
  performs the required unsigned `xcodebuild` simulator build. It then installs
  the simulator app, launches it with the storage-validation environment flag
  and requires the app-written result file to begin with `PASS:`.
- Codemagic `ios-simulator-unsigned` for commit `35779c1` failed before
  app-hosted validation in `:shared:compileTestKotlinIosSimulatorArm64` because
  Kotlin/Native's `kotlin.test.Ignore` annotation constructor accepts no
  message argument. The ignored standalone Keychain test now uses no-arg
  `@Ignore`, with the reason kept in a nearby source comment and project docs.
- Codemagic `ios-simulator-unsigned` for commit `cc38dee` passed the shared iOS
  simulator tests, all iOS Kotlin compile tasks, Swift package resolution and
  the unsigned `xcodebuild` simulator build. The app-hosted validation step then
  failed before launching Bettamind because this Xcode/simctl version rejects
  `simctl launch --env` with `Invalid device: --env`.
- Codemagic now passes the validation flag with the supported
  `SIMCTL_CHILD_BETTAMIND_IOS_STORAGE_VALIDATION=1 xcrun simctl launch ...`
  form and stores `ios-storage-validation-launch.log` as an artifact.
- Codemagic `ios-simulator-unsigned` for commit `a3c6f39` reached the
  app-hosted validation code. The app reported
  `iOS Keychain read database key failed with OSStatus -34018`, which indicates
  the unsigned simulator app lacks the Keychain entitlement required by
  Security.framework.
- Codemagic still keeps the required unsigned `xcodebuild` simulator build.
  The validation step now copies that built app, generates a temporary
  test-only simulator Keychain entitlement, ad-hoc signs the copy only for
  simulator installation, and stores the signed entitlements in
  `ios-storage-validation-entitlements.log`.
- Codemagic `ios-simulator-unsigned` for commit `2a871c5` signed the validation
  copy, but the simulator denied launching the app through SpringBoard. The
  validation entitlements were too broad for ad-hoc simulator launch because
  they included fake `application-identifier` and team identifier values.
- The validation entitlement file now contains only the test
  `keychain-access-groups` entry. `IosKeychainStorageKeyManager` accepts an
  optional access group, and the app-hosted validation passes the same temporary
  Codemagic access group through
  `BETTAMIND_IOS_STORAGE_KEYCHAIN_ACCESS_GROUP`. Default production-style
  Keychain use remains unchanged when no access group is supplied.
- Codemagic `ios-simulator-unsigned` for commit `5425326` still failed before
  app-hosted validation code ran: SpringBoard denied launching the manually
  re-signed validation app. The workflow now preserves the required unsigned
  build, then performs a separate validation-only Xcode simulator build using
  `iosApp/StorageValidation.entitlements` so Xcode owns the ad-hoc simulator
  signing shape.
- The validation workflow extracts the signed app's first
  `keychain-access-groups` entitlement with `PlistBuddy`, records it in
  `ios-storage-validation-keychain-group.log`, and passes it to the app-hosted
  storage validator through
  `BETTAMIND_IOS_STORAGE_KEYCHAIN_ACCESS_GROUP`.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed after the
  Xcode-signed app-hosted iOS encrypted-storage validation update. Phase 3 is
  complete.
- Phase 5 shared knowledge-pack foundation was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/knowledge/`.
- Phase 5 installer requires non-empty signed manifests, the `Ed25519`
  algorithm label, SHA-256 payload checksum matches, injected signature
  verification, rollback/replay rejection and revocation policy.
- Phase 5 local retrieval indexes installed packs in memory and searches them
  offline without backend, network or AI.
- Common tests now cover SHA-256 vectors, checksum mismatch, invalid signature,
  unsupported algorithm, rollback/replay, signing-key revocation and offline
  retrieval.
- GitHub Actions mobile checks now run `phaseFiveCheck`.
- `docs/security/phase-5-signed-knowledge-packs.md` documents the Phase 5
  trust boundary and non-goals.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for commit
  `333f320` and approved proceeding to Phase 6.
- Common SHA-256 and manifest signature verification primitives now live under
  `shared/src/commonMain/kotlin/org/bettamind/shared/security/`.
- `UnavailableLocalAiRuntime` was added to preserve no-AI core operation.
- `LiteRtLmRuntimeAdapter` and `LiteRtLmBridge` were added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/ai/` so a future platform
  LiteRT-LM implementation sits behind the replaceable `LocalAiRuntime`
  interface.
- `ModelPackManager` was added under `shared/src/commonMain/kotlin/org/bettamind/shared/ai/`
  for optional model packs with signed Ed25519-labeled manifests, SHA-256
  artifact checksums, resumable chunk offsets, rollback/replay rejection,
  revocation policy and removable installed packs.
- No model weights, model downloads, cloud AI or Phase 7 response modes were
  added.
- Common tests now cover unavailable AI, LiteRT-LM adapter delegation, signed
  chunked model-pack installation, resume, offset rejection, checksum rejection,
  rollback/revocation and removal.
- GitHub Actions mobile checks now run `phaseSixCheck`.
- `docs/security/phase-6-ai-model-manager.md` documents the Phase 6 trust
  boundary and non-goals.
- Phase 6X audit archived the active implementation plan unchanged at
  `docs/planning/archive/implementation-plan-before-phase-6x.md`.
- `docs/planning/phase-6x-integration-audit.md` records existing and missing
  functions for Phase 6.4, Phase 6.5 and Phase 6.6.
- `docs/planning/phase-6x-integration-plan.md` defines the recommended order,
  scopes, acceptance criteria, migration implications and exact next prompt.
- `docs/planning/roadmap-amendment-phase-6x.md` amends the roadmap without
  editing the active implementation plan.

## Important files

- `AGENTS.md`
- `docs/specification/bettamind-locked-specification.md`
- `docs/planning/implementation-plan.md`
- `docs/planning/archive/implementation-plan-before-phase-6x.md`
- `docs/planning/phase-6x-integration-audit.md`
- `docs/planning/phase-6x-integration-plan.md`
- `docs/planning/roadmap-amendment-phase-6x.md`
- `docs/planning/requirements-traceability.md`
- `docs/planning/risk-register.md`
- `docs/design/brand-and-colour-decision.md`
- `docs/design/font-sources.md`
- `docs/working-notes/project-memory.md`
- `scripts/generate_brand_assets.py`
- `shared/src/commonMain/kotlin/org/bettamind/shared/App.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/design/BettamindTheme.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/design/BettamindColorTokens.kt`
- `shared/src/commonMain/composeResources/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/privacy/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/growth/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/knowledge/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/ai/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/security/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/privacy/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/growth/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/knowledge/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/ai/`
- `shared/src/androidMain/kotlin/org/bettamind/shared/privacy/`
- `shared/src/iosMain/kotlin/org/bettamind/shared/privacy/`
- `shared/src/iosTest/kotlin/org/bettamind/shared/privacy/`
- `shared/src/nativeInterop/cinterop/`
- `androidApp/src/main/res/`
- `iosApp/iosApp/Assets.xcassets/`
- `iosApp/iosApp.xcodeproj/project.pbxproj`
- `docs/security/phase-3-encrypted-storage-spike.md`
- `docs/security/phase-5-signed-knowledge-packs.md`
- `docs/security/phase-6-ai-model-manager.md`
- `codemagic.yaml`

## Commands that passed

- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --stacktrace`
- `.\gradlew.bat :androidApp:compileDebugKotlin --no-daemon --stacktrace`
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --stacktrace`
- `.\gradlew.bat phaseOneCheck --no-daemon --stacktrace`
- `.\gradlew.bat :androidApp:lintDebug --no-daemon --stacktrace`
- `.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --stacktrace`
- `.\gradlew.bat :shared:compileKotlinIosSimulatorArm64 --no-daemon --stacktrace`
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace`
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --rerun-tasks --no-daemon --stacktrace`
- `.\gradlew.bat :shared:compileKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  now succeeds on Windows by skipping disabled iOS cinterop targets.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  now succeeds on Windows by skipping disabled iOS cinterop targets.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  cinterop include-path fix.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  passed after the include-path fix; Windows still skips the iOS cinterop target.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  Kotlin/Native out-pointer and Foundation API fix.
- `.\gradlew.bat :shared:compileKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the fix, with iOS Native targets still disabled
  because cinterop cannot be processed on `mingw_x64`.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  `allocPointerTo` cinterop pointer-helper fix.
- `.\gradlew.bat :shared:compileKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the pointer-helper fix, with iOS Native targets
  still disabled because cinterop cannot be processed on `mingw_x64`.
- Temporary Kotlin/Native compiler probe:
  `kotlinc-native.bat .codex-scratch\CInteropPointerProbe.kt -target mingw_x64 -produce library -o .codex-scratch\probe`
  passed with `allocPointerTo<T>()`, `ptr` and `value`; the scratch files were
  removed and were not committed.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  explicit iOS `size_t` conversion fix.
- Temporary Kotlin/Native compiler probe:
  `kotlinc-native.bat .codex-scratch\CInteropPointerProbe.kt -target mingw_x64 -produce library -o .codex-scratch\probe`
  passed with explicit `size_t` conversion; the scratch files were removed and
  were not committed.
- `.\gradlew.bat :shared:compileKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the `size_t` fix, with iOS Native targets still
  disabled because cinterop cannot be processed on `mingw_x64`.
- Temporary Kotlin/Native compiler probe:
  `kotlinc-native.bat .codex-scratch\IosKeychainProbe.kt -target ios_simulator_arm64 -produce library -o .codex-scratch\ios-keychain-probe`
  passed with CoreFoundation type dictionary callbacks and
  `kSecUseDataProtectionKeychain`; the scratch files were removed and were not
  committed.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the iOS
  Keychain query-construction fix.
- `.\gradlew.bat :shared:compileKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the Keychain fix, with iOS Native targets still
  disabled because cinterop cannot be processed on `mingw_x64`.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  Keychain test diagnostic helper was added.
- `.\gradlew.bat :shared:compileKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the Keychain test diagnostic helper was added,
  with iOS Native targets still disabled because cinterop cannot be processed
  on `mingw_x64`.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  app-hosted iOS encrypted-storage validation route was added.
- `.\gradlew.bat :shared:compileKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the app-hosted validation update, with the iOS
  Native compile task still skipped on Windows.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  no-arg iOS `@Ignore` fix.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the no-arg iOS `@Ignore` fix, with the iOS Native
  test compile task still skipped on Windows.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  `SIMCTL_CHILD_` Codemagic validation-launch fix.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the `SIMCTL_CHILD_` Codemagic validation-launch
  fix, with the iOS Native test compile task still skipped on Windows.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  Codemagic ad-hoc signed iOS validation-copy update.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the Codemagic ad-hoc signed validation-copy update,
  with the iOS Native test compile task still skipped on Windows.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  minimal Codemagic Keychain access-group entitlement update.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the minimal Codemagic Keychain access-group
  entitlement update, with the iOS Native test compile task still skipped on
  Windows.
- `.\gradlew.bat phaseThreeCheck --no-daemon --stacktrace` passed after the
  Xcode-signed iOS validation-build update.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after the Xcode-signed iOS validation-build update, with
  the iOS Native test compile task still skipped on Windows.
- `backend\.venv\Scripts\ruff.exe check .`
- `backend\.venv\Scripts\mypy.exe app`
- `backend\.venv\Scripts\pytest.exe`
- From `backend/`: `.\.venv\Scripts\ruff.exe check .`
- From `backend/`: `.\.venv\Scripts\mypy.exe app`
- From `backend/`: `.\.venv\Scripts\pytest.exe`
- Phase 6X docs-only checks:
  `git diff --check` passed with normal Windows LF-to-CRLF warnings; archive
  SHA-256 comparison matched for
  `docs/planning/implementation-plan.md` and
  `docs/planning/archive/implementation-plan-before-phase-6x.md`.
- `git ls-files | rg "\.(tflite|litertlm|gguf|onnx|bin|safetensors|model|mlmodel|task)$"`
  found no tracked model-weight artifacts.
- docs/source placeholder scan found only Codemagic's intentional `events: []`
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --stacktrace`
  passed after the Phase 5 knowledge-pack tests were added.
- `.\gradlew.bat phaseFiveCheck --no-daemon --stacktrace` passed. On Windows,
  iOS Native targets remain disabled because SQLCipher cinterop requires macOS.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after Phase 5 changes, with the iOS Native compile/test
  tasks still skipped because cinterop cannot be processed on `mingw_x64`.
- From `backend/`: `.\.venv\Scripts\ruff.exe check .`
- From `backend/`: `.\.venv\Scripts\mypy.exe app`
- From `backend/`: `.\.venv\Scripts\pytest.exe`
- `git diff --check` reported no whitespace errors, only normal Windows
  LF-to-CRLF warnings.
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --stacktrace`
  passed after the Phase 6 AI/model-pack tests and shared security refactor.
- `.\gradlew.bat phaseSixCheck --no-daemon --stacktrace` passed. On Windows,
  iOS Native targets remain disabled because SQLCipher cinterop requires macOS.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --stacktrace`
  completed on Windows after Phase 6 changes, with the iOS Native compile/test
  tasks still skipped because cinterop cannot be processed on `mingw_x64`.
- From `backend/`: `.\.venv\Scripts\ruff.exe check .`
- From `backend/`: `.\.venv\Scripts\mypy.exe app`
- From `backend/`: `.\.venv\Scripts\pytest.exe`

## Known blockers and limitations

- iOS cannot be fully built locally on Windows. Every shared/iOS change still
  requires Codemagic `ios-simulator-unsigned`.
- Phase 6X is documentation-only, so it does not itself require Codemagic unless
  a later implementation commit changes shared/iOS code, Compose resources,
  Gradle iOS behavior, `iosApp` or Codemagic iOS workflow files.
- App privacy lock is not implemented yet. Existing encrypted storage is real,
  but Android/iOS storage keys are not currently bound to fresh local user
  authentication.
- Relational-boundary policy is not implemented yet. Existing copy says
  Bettamind does not contact anyone automatically, but there is no classifier
  or AI output contract.
- Deterministic daily tools are not implemented yet beyond the Phase 4 growth
  flow skeleton.
- Phase 4 does not yet persist narrative content. Storage status still reports
  encrypted storage unavailable until a separate approved pass wires the
  platform encrypted store into the growth flow. There is no unencrypted
  fallback.
- Phase 5 does not include production content packs, production signing keys or
  owner-approved trust anchors. Release work must provide those before real
  public packs are accepted.
- Phase 6 does not include production model packs, model weights, LiteRT native
  dependency, model licences, production signing keys or owner-approved model
  trust anchors.
- No canonical SVG source logo is present yet. Phase 2 assets are derived from
  the PNG fallback.
- The source PNG has a baked checkerboard background; generated assets use a
  documented mask to derive transparency.
- Locale packs are draft implementation foundations and require qualified human
  review before production use. Phase 4 non-English strings are draft fallback
  text and are not production-approved translations.
- `java` is not available on global `PATH`; current checks used JetBrains'
  bundled JBR at `C:\Program Files\JetBrains\PyCharm 2025.2.3\jbr`.
- Local Windows Gradle checks require a non-committed `local.properties` with
  `sdk.dir=C\:\\Users\\HP\\AppData\\Local\\Android\\Sdk`.

## Manual owner actions

- Run Codemagic `ios-simulator-unsigned` for pushed commits that change shared
  Kotlin, Compose resources, `iosApp`, Gradle configuration that can affect
  iOS, or Codemagic iOS workflow files.
- Before Phase 6.4 implementation, decide whether to approve a Bettamind PIN
  fallback, the default lock requirement, auto-lock timeout, screenshot/privacy
  default and local reset scope.
- Provide `brand/source/bettamind-logo-master.svg` if a vector master exists,
  then regenerate assets from that source in a later approved pass.
- Replace placeholder Android application ID and iOS bundle ID with owner-owned
  values before release work.
- Provide owner-approved production knowledge-pack trust anchors and content
  governance before accepting real public packs.
- Provide owner-approved production model choices, licences, trust anchors and
  delivery governance before accepting real model packs.
- Arrange qualified human review for production translations, especially any
  safety, crisis, legal, privacy or consent copy.

## Next approved task

Wait for explicit owner approval to implement Phase 6.4 from
`docs/planning/phase-6x-integration-plan.md`. Do not begin Phase 6.5, Phase 6.6
or Phase 7 automatically.
