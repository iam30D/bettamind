# Bettamind Project Memory

## Current phase

Phase 12 performance, red-team and release-readiness foundation is implemented
and locally verified on Windows. Phases 0 through 12 and the local model-pack
recommendation/licence records are treated as implemented, with
owner-confirmed Codemagic `ios-simulator-unsigned` validation for Phase 12 on
2026-06-25. Real release evidence, including signed TestFlight installation and
physical-device smoke testing, is still required before production approval. A
manual Codemagic `ios-testflight-release` workflow has been added for signed
IPA build and App Store Connect upload after owner Apple/Codemagic setup. A
Codemagic release archive for commit `ff45a41` reached the signed IPA step but
failed during `xcode-project build-ipa`; the likely cause was the checked-in
iOS Xcode config still defaulting to `dev.bettamind.placeholder` while
Codemagic fetched the App Store profile for `com.corenovaness.bettamind`.
The iOS Debug and Release xcconfig bundle-ID defaults are now updated to
`com.corenovaness.bettamind`. A later Codemagic release build successfully
built and signed `Bettamind.ipa` with bundle ID `com.corenovaness.bettamind`
and version code 4, then failed only during App Store Connect publishing
because Xcode 16.4 produced an iOS 18.5 SDK build and App Store Connect now
requires iOS 26 SDK or later. Codemagic iOS workflows are now updated to Xcode
26.0, and Kotlin is updated to 2.2.21 to match JetBrains' documented Xcode
26.0 compatibility. The first installed TestFlight build then crashed
immediately on iPhone launch. The iOS Xcode project linked the SQLCipher Swift
Package but did not explicitly embed and codesign `SQLCipher.framework`, which
can cause an immediate dynamic-loader termination on device. A first attempted
Xcode copy-files embed phase was invalid because Xcode looked for a package
product path named `SQLCipher` instead of the concrete framework bundle. A
second manual run-script embed phase then failed under Xcode 26 because the
Swift Package integration already produces
`Bettamind.app/Frameworks/SQLCipher.framework`, causing a duplicate output. The
manual embed phase has been removed; the release workflow keeps the signed IPA
inspection step to prove the framework is present and expose `otool`
dependencies. The attached TestFlight crash report for build 8 showed
`EXC_CRASH`/`SIGABRT` from Bettamind itself, with
`SQLCipher.framework` already loaded and no dyld error message, so the next
release-candidate patch removes first-render Compose font and decorative image
resource loading while the iOS launch abort is isolated. Build 9 still crashed
on launch with the same `EXC_CRASH`/`SIGABRT` shape and no dyld error; the
workflow now collects and publishes zipped iOS dSYMs plus UUID logs so the
next crash report can be symbolicated. Build 10 was symbolicated with the
matching dSYM and traced to Compose Multiplatform
`androidx.compose.ui.uikit.PlistSanityCheck`, which aborts when
`Info.plist` lacks `CADisableMinimumFrameDurationOnPhone=true`. The iOS plist
now includes that key. A static public website has been added under
`apps/website` as an isolated Astro site for support, privacy, safety, AI
transparency, data deletion and brand pages; this did not modify mobile app,
backend, AI, sync or safety-system runtime code.
A later production-readiness app integration now makes the shared Compose app
more release-candidate-like: the header renders the Bettamind mark, production
facing scaffold copy is removed, Today saves encrypted check-ins through
Android/iOS SQLCipher-backed app services after local adult confirmation,
draft locale resource directories mirror English source text for the
English-only production scope, Settings exposes local platform integration
states, Grow exposes selectable concern prompts backed by deterministic
no-model AI growth fallbacks, and Support exposes deterministic local support
assessment. Qwen2.5 1.5B Instruct is the first optional model-pack target, but
production model-pack status now has an owner-supplied Qwen `.litertlm`
release candidate, app-compatible signed manifest and approved Ed25519 public
trust anchor. Public release still stays blocked until platform LiteRT-LM
runtime validation, Android/iOS device evidence, rollback/revocation review and
final owner release approval are complete. This does not complete
owner-controlled production gates. A later
Codemagic iOS simulator run for this integration reported
`NSDate.timeIntervalSince1970` as an unresolved Kotlin/Native reference in
`BettamindIosServices`; the iOS app-service clock now uses POSIX `time(null)`
to match the existing iOS SQLCipher store pattern. A later Codemagic run then
reported that direct POSIX `time` use requires
`@OptIn(ExperimentalForeignApi::class)`; the iOS clock now routes through a
small `currentEpochMillis()` helper with the opt-in scoped to that helper.

## Locked decisions

- Mobile stack: Kotlin Multiplatform and Compose Multiplatform.
- Primary development environment: Windows.
- iOS validation: Codemagic macOS using the real `iosApp` Xcode project.
- Owner-confirmed iOS bundle ID: `com.corenovaness.bettamind`.
- Current release toolchain: Kotlin 2.2.21, Compose Multiplatform 1.8.2,
  Android Gradle Plugin 8.11.1, Gradle 8.14.3 and Codemagic Xcode 26.0 for iOS
  workflows.
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
- Phase 6X inserted four completed phases before Phase 7: Phase 6.4 App
  Privacy Lock, Phase 6.5 Relational Boundaries, Phase 6.6 Deterministic Daily
  Tools and Phase 6.7 Harmful Intent and Dangerous Capability Safeguards.
- Phase 6.4 must be a real storage-key/privacy-lock implementation, not only a
  visual lock screen.
- Phase 6.5 must exist before Phase 7 AI response modes.
- Bettamind may be warm, compassionate, respectful and attentive, but must not
  present itself as a romantic or sexual partner, spouse, soulmate, exclusive
  companion, replacement for human relationships or sentient being with
  emotional needs.
- Phase 6.5 relational boundary policy must remain deterministic, local and
  model-free. Future AI, memory, export, sync, notification, voice and avatar
  surfaces must use the policy before user-visible or stored output.
- Phase 6.6 must keep daily tools deterministic, offline-first and encrypted.
- Phase 6.6 daily records must use `EncryptedRecordStore` only. There is no
  unencrypted fallback, backend sync by default, public ranking, manipulative
  streak or human-worth score.
- Phase 6.6 local reminders must use neutral previews, quiet hours, snooze and
  pause-all policy. System-calendar interaction is an explicit handoff and does
  not read broad calendar data by default.
- Phase 6.7 harmful-intent safeguards must remain deterministic, offline,
  local and model-free. Future AI response modes must call the policy before
  generation and validate generated output before display, storage, export,
  sync, notification, support sharing, voice or avatar use.
- Phase 6.7 refuses actionable harmful details for dangerous capability
  requests even when framed as education, fiction, safety research, curiosity,
  role-play or theory. Ambiguous intent must not be treated as guilt.
- Harmful, violent, criminal and self-harm narrative is excluded from permanent
  memory, export, sync and notifications by default. Only minimum encrypted
  metadata may be stored, and sensitive export/support sharing requires
  explicit preview plus local step-up authentication.
- Bettamind PIN/passphrase production storage must use the approved Argon2id
  KDF. Phase 6.4 adds the KDF boundary and tests but does not substitute a
  weaker production fallback.
- The active continuation plan is
  `docs/planning/phase-7-to-12-continuation-plan.md`. It preserves the original
  Phase 7 through Phase 12 objectives and adds required Phase 6.4 through Phase
  6.7 integration gates. Phase 11 is implemented in the current working tree;
  Phase 12 is not started.
- Phase 7 AI-assisted growth modes are optional, local and replaceable behind
  `LocalAiRuntime`. No cloud AI, model downloads, model weights, backend
  dependency, speech or sync implementation exists.
- Phase 7 keeps deterministic no-model fallback for Quick Guidance, Guided
  Reflection, Deep Exploration and Action-Only. Daily-tool context may enter a
  model prompt only when requested and explicitly consented.
- Phase 7 AI output must pass relational-boundary and harm-safety validation
  before display, memory/export/sync/notification eligibility, voice or avatar
  use. Permanent memory remains proposal-only, automatic writes are disabled
  and separate approval is required.
- Phase 7.5 adds compassionate safety redirection as a deterministic layer on
  top of Phase 6.5, Phase 6.7 and Phase 7. Safety boundaries must stay firm
  while avoiding shame, diagnosis and guilt assumptions for ambiguous intent.
- Phase 7.5 safety responses use localization-key templates for
  acknowledgement, boundary, human-growth redirect, practical next step and
  privacy notice. Better-human pathways can recommend grounding, breathing,
  delay, leaving the situation, contact support, emergency help, conflict
  reflection, repair planning, values-to-action, difficult conversation,
  consent and boundaries, self-compassion or no follow-up needed.
- Phase 7.5 AI metadata must expose safety boundary status and reason, user
  intent confidence, allowed discussion scope, better-human pathway,
  recommended deterministic tool, memory/export eligibility, step-up
  authentication and urgent-support requirement.
- Phase 7.5 generated output must be blocked before display/storage/export/
  sync/notification/voice/avatar use if it shames, diagnoses, assumes bad
  intent without evidence, encourages Bettamind dependency or skips a safe next
  step when a boundary is applied.
- Phase 8 safety-support bridge must remain deterministic and local. Support
  actions are voluntary and user-initiated only; Bettamind never automatically
  contacts anyone and never claims help was contacted unless the user completed
  that action.
- Phase 8 support summaries must use minimum necessary detail, exclude raw
  crisis or harmful narrative by default and require explicit preview plus
  local step-up authentication before sensitive sharing.
- Phase 9 encrypted export/sync must remain optional. Backend use is not
  required for core mobile use, sync is disabled by default and the user must
  explicitly approve encrypted sync before any backend payload is prepared.
- Phase 9 backend payloads must be ciphertext-only versioned envelopes with
  nonce, key version, manifest version and SHA-256 ciphertext checksum. The
  optional backend must reject plaintext fields.
- Phase 9 export and sync decisions must require encrypted packages/envelopes
  and app-lock step-up. Daily-tool, relationally sensitive, harm-safety and
  support-summary content is excluded by default; sensitive export requires
  explicit selection and preview.
- Phase 9 conflict handling must be deterministic and non-destructive:
  divergent encrypted versions are kept for user review instead of silently
  overwriting local private data.
- Phase 9 device revocation must require explicit user action and local
  step-up authentication, and revocation records/manifests must be versioned.
- Local AI model packs remain optional recommendations only. Bettamind installs
  and runs with no model; Qwen2.5 1.5B Instruct is the first LiteRT-LM
  release-candidate pack, Gemma 4 E2B is deferred until device/storage evidence
  supports the larger pack, and every model install requires explicit user
  approval plus signed/checksum-verified removable packs.
- The Qwen2.5 1.5B Instruct release-candidate artifact is kept outside Git at
  `C:\bettamind-model-release\release-qwen2.5-1.5b-v1` with artifact filename
  `qwen2_5_1_5b_instruct_bettamind_v1.litertlm`, size `1597931520`, SHA-256
  `FAA60663B333290C1496C499828B21D3E3254A788CACD8CCE917CE0F761A2DC9`,
  app-compatible signed manifest SHA-256
  `69F1E42B6FF9F67C362FEC21A283DA86726603391FA5EF07D27792F73029E324` and
  signing key ID `bettamind-model-prod-2026-01`.
- `BettamindModelPackTrustPolicy.productionTrustAnchors` now includes the
  owner-approved Qwen model-pack Ed25519 public trust anchor. The private
  signing key remains outside Git and must not be committed.
- Owner licence acceptance and release records are required before packaging or
  distributing any production model artifact. User install consent does not
  replace publisher licence compliance.
- Without an installed model pack, `LocalAiRuntime` uses the unavailable
  runtime and deterministic fallback. It does not generate learned-model
  intelligence; it routes deterministic flows, local resources, optional
  signed knowledge retrieval and model-free safety policies.
- Phase 10 target locale resources must remain key-complete. The current
  production scope is English-only; non-English Compose resource directories
  intentionally mirror English source strings until qualified human review
  records exist for production locales, especially safety, crisis, legal,
  privacy, consent, relational-boundary, support, export/sync and daily-tool
  copy.
- Phase 10 accessibility support covers deterministic policy for RTL layout,
  script font fallbacks, large text, reduced motion, screen-reader labels,
  accessible typography and low-literacy mode. Platform-specific assistive
  technology testing still belongs in release validation.
- Phase 11 offline speech remains optional. Text-only use is complete without
  microphone access, speech packs or backend services.
- Phase 11 microphone use must be explicit and permission-scoped. No passive
  listening exists, and raw audio is not retained by default.
- Phase 11 spoken input and generated spoken output must route through the
  same relational-boundary and harm-safety policies as text before display,
  storage, export, sync, notification, support sharing, voice or avatar use.
- OS offline voices are preferred before local voice packs. Any production
  speech pack must be owner-approved for licence compliance, signed,
  SHA-256-verified, versioned, revocable, removable and installed only after
  explicit user approval. No speech artifacts are committed.
- Phase 12 is a release-readiness gate foundation, not a production-release
  approval. Repository checks can prove deterministic privacy and red-team
  contracts, but physical-device performance, battery, thermal, memory,
  Codemagic iOS, TestFlight, store metadata, rollback and qualified
  translation review remain owner-controlled release evidence.
- Phase 12 production readiness must stay blocked until all required
  `ReleaseReadinessPolicy` gates are passed or explicitly accepted with
  evidence; missing physical-device or store evidence cannot be treated as a
  passing automated check.
- The signed iOS release path is the manual Codemagic
  `ios-testflight-release` workflow. It uses Codemagic secure Apple signing and
  App Store Connect integration, fails on placeholder bundle IDs and uploads to
  App Store Connect without automatically submitting for external beta review
  or App Store review.
- `iosApp/Config/Debug.xcconfig` and `iosApp/Config/Release.xcconfig` default
  `BETTAMIND_IOS_BUNDLE_ID` to `com.corenovaness.bettamind` so archive signing
  can match the App Store provisioning profile. The Codemagic
  `bettamind-testflight` group should use the same value.
- Codemagic iOS workflows must use Xcode 26.0 or later for App Store Connect
  uploads. Kotlin 2.2.21 is the current project patch because JetBrains'
  Kotlin Multiplatform compatibility table documents Xcode 26.0 support for
  that version.
- SQLCipher must be embedded and codesigned in the iOS app bundle for device
  and TestFlight builds. The release workflow publishes `ipa-contents.log`,
  `ipa-bettamind-otool.log` and `ipa-shared-otool.log` to make launch-time
  framework dependencies inspectable.
- During the build 8 TestFlight launch-crash investigation, bundled Compose
  font families and the decorative header image were temporarily removed from
  first render. This keeps startup resource-free for the next smoke build; the
  brand mark and custom font stack should be restored only after iOS launch is
  stable or the failing resource path is symbolicated.
- Codemagic `ios-testflight-release` publishes
  `build/ios/dsyms/Bettamind-dSYMs.zip`, `dsym-files.log` and
  `dsym-uuids.log` after archive creation. Use those artifacts with the
  matching TestFlight `.ips` crash report to symbolicate iOS launch crashes.
- Compose Multiplatform 1.8.2 on iOS requires
  `CADisableMinimumFrameDurationOnPhone=true` in `Info.plist` unless strict
  plist sanity checks are disabled. Bettamind keeps the check enabled and adds
  the key to `iosApp/iosApp/Info.plist`.

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
- Phase 6.4 shared privacy-lock domain code was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/privacy/PrivacyLock.kt`.
- Phase 6.4 common tests were added under
  `shared/src/commonTest/kotlin/org/bettamind/shared/privacy/PrivacyLockTest.kt`.
- Android `BiometricPrompt` support was added through
  `AndroidPrivacyLockAuthenticator`, and Android app preview protection now
  uses `FLAG_SECURE`.
- iOS `LocalAuthentication` support was added through
  `IosPrivacyLockAuthenticator`, and the iOS SwiftUI host covers app content
  while inactive.
- Settings now exposes the privacy-lock timeout foundation and security
  explanation through Compose resources.
- `phaseSixFourCheck` was added and GitHub Actions now runs it for mobile
  checks.
- `docs/security/phase-6-4-app-privacy-lock.md` documents the Phase 6.4
  security boundary and remaining validation.
- Codemagic `ios-simulator-unsigned` for commit `67d15db` failed in
  `:shared:compileKotlinIosSimulatorArm64` because
  `IosPrivacyLockAuthenticator` imported and allocated unsupported
  `ObjCObjectVar` for an optional `NSError` out pointer. The capability check
  now passes `error = null` to `LAContext.canEvaluatePolicy`, avoiding that
  unsupported Kotlin/Native type.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the Phase 6.4
  fix commit `b110cf9` and approved proceeding to Phase 6.5.
- Phase 6.5 shared relational-boundary policy was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/safety/`.
- The policy adds deterministic relational risk levels, boundary signals,
  pre-generation input assessment, post-generation output validation,
  no-model fallback identifiers, minimal encrypted metadata and surface
  decisions for permanent memory, export, sync, notifications and voice/avatar
  use.
- Common tests now cover romantic attachment prompts, marriage and partner-role
  requests, soulmate and dependency language, jealousy and missing projections,
  sexual attraction, sexting, erotic role-play, repeated romantic requests,
  unavailability distress, social withdrawal, responsibility neglect, ordinary
  appreciation, allowed human relationship discussion, factual
  consent/sexuality discussion, self-harm tied to perceived AI rejection,
  invalid generated AI output, no-model fallback, encrypted minimal metadata,
  neutral notification copy and offline operation.
- Settings now exposes a concise relationship-boundary explanation through
  Compose resources. Non-English entries are source-English draft fallbacks and
  still require qualified human review before production use.
- `docs/safety/relational-boundaries.md` documents the Phase 6.5 safety
  contract, non-goals and verification boundary.
- `phaseSixFiveCheck` was added and GitHub Actions mobile checks now run it.
- Product, locked specification, AGENTS, Phase 6X plan, roadmap amendment,
  requirements traceability and risk register were updated for Phase 6.5.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the Phase 6.5
  commit `ba8a86d` and approved proceeding to Phase 6.6.
- Phase 6.6 shared deterministic daily-tool foundation was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/daily/`.
- The daily module adds daily check-in records for mood, energy, stress and
  sleep; encrypted daily-record repository; encrypted backup/restore delegation;
  deterministic box-breathing and grounding catalogs; reusable timer recovery;
  local reminder quiet-hours, snooze and pause-all policy; neutral notification
  preview; private calendar handoff policy; deterministic worksheet templates;
  and local trend summaries without AI or human-worth scoring.
- Common tests now cover encrypted-only daily record persistence, encrypted
  backup/restore, no fallback/backend/AI/ranking/streak/worth-score policy,
  deterministic breathing and grounding, timer recovery, neutral reminders,
  quiet hours, pause-all, snooze, private calendar handoff, worksheet templates,
  local trend summaries and record validation.
- The Today surface now exposes the daily-tools foundation through Compose
  resources. Non-English Phase 6.6 entries are source-English draft fallbacks
  and require qualified human review before production use.
- `docs/product/phase-6-6-deterministic-daily-tools.md` documents the Phase
  6.6 scope, privacy boundary and verification.
- `phaseSixSixCheck` was added and GitHub Actions mobile checks now run it.
- Product, locked specification, AGENTS, assumptions/decisions, Phase 6X plan,
  roadmap amendment, requirements traceability and risk register were updated
  for Phase 6.6.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the Phase 6.6
  commit and approved proceeding with Phase 6.7 only.
- Phase 6.7 shared harmful-intent and dangerous-capability safeguards were
  added under `shared/src/commonMain/kotlin/org/bettamind/shared/safety/`.
- The harm policy adds required risk levels, categories, intent signals,
  capability signals, pre-generation planning, post-generation output
  validation, no-model fallback identifiers, minimal encrypted metadata,
  memory/export/sync/notification/support decisions, app-lock step-up mapping,
  daily-tool de-escalation decisions and relational-boundary combination.
- Common tests now cover dangerous capability requests, disguised educational
  and fictional framing, safety research framing, explosives, weapons,
  poisoning, evidence hiding, policy bypass, named threats, revenge, stalking,
  coercion, self-harm method requests, suicidal intent, intrusive thoughts
  without intent, anger without intent, ordinary conflict, emergency response,
  prevention, safe distance/disposal, historical or ethical discussion, invalid
  JSON, unsafe generated output, no-model fallback, offline operation,
  notifications, export defaults, support summaries, app-lock step-up, daily
  tools, relational overlap, localization review and RTL.
- Settings now exposes a concise harmful-intent safeguards explanation through
  Compose resources. Non-English Phase 6.7 entries are source-English draft
  fallbacks and require qualified human review before production use.
- `docs/safety/harmful-intent-and-dangerous-capability-policy.md` documents the
  Phase 6.7 safety contract, non-goals, privacy boundary and verification.
- `phaseSixSevenCheck` was added and GitHub Actions mobile checks now run it.
- AGENTS, locked specification, implementation plan, roadmap amendment,
  requirements traceability and risk register were updated for Phase 6.7.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed after the Phase
  6.7 commit.
- Roadmap reconciliation after Phase 6X completed as a docs-only planning pass;
  no production source code was changed and Phase 7 was not started.
- `docs/planning/phase-7-to-12-continuation-plan.md` was created to preserve
  original Phase 7 through Phase 12 objectives while adding Phase 6.4, Phase
  6.5, Phase 6.6 and Phase 6.7 integration gates.
- `docs/planning/implementation-plan.md`,
  `docs/planning/roadmap-amendment-phase-6x.md`,
  `docs/planning/requirements-traceability.md` and
  `docs/planning/risk-register.md` were reconciled for the remaining roadmap.
- Phase 7 shared AI growth-mode orchestration was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/ai/AiGrowthModes.kt`.
- `AiGrowthModeEngine` adds Quick Guidance, Guided Reflection, Deep
  Exploration and Action-Only, with pre-generation harm and relational
  classification, consent-filtered daily context, structured JSON model output
  parsing, post-generation validation and deterministic fallback localization
  keys.
- Common tests in
  `shared/src/commonTest/kotlin/org/bettamind/shared/ai/AiGrowthModesTest.kt`
  cover no-model fallback, consent filtering, app-lock step-up metadata,
  dangerous capability refusal before generation, relational redirection before
  generation, unsafe generated output blocking, malformed output fallback,
  memory/export eligibility and local runtime failure fallback.
- The shared Compose Grow surface now exposes a small Phase 7 foundation panel
  for the four modes. All user-facing Phase 7 strings live in Compose resource
  files; non-English entries are draft fallback copy pending human review.
- `phaseSevenCheck` was added and GitHub Actions mobile checks now run it.
- Implementation plan, continuation plan, requirements traceability and risk
  register were updated for Phase 7.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for pushed commit
  `621b3c2`.
- `BettamindLocalAiModelPolicy` originally recorded Gemma/Qwen tiered
  recommendations. The current release-candidate policy supersedes that with
  Qwen2.5 1.5B Instruct as the first model-pack target, no auto-install,
  explicit user approval before install and deterministic fallback when a model
  is unavailable, declined or removed.
- Local AI model-pack recommendation policy commit `540f733` was pushed to
  `main` after owner-confirmed Codemagic pass for `621b3c2`.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for latest pushed
  commit `d1811db`.
- `docs/operations/local-ai-model-pack-release.md` and
  `docs/operations/model-pack-manifest-template.json` document the owner
  licence gate, required release records, signed manifest shape and user
  install experience for future production model packs.
- `docs/architecture/local-ai-no-model-fallback.md` documents that no-model
  mode is deterministic fallback, not learned-model generation.
- `docs/operations/model-license-approval-records.md` provides owner-editable
  approval records for Gemma 4 E2B and Qwen2.5 1.5B Instruct, both currently
  marked as Apache-2.0 on source pages and owner-approved for licence use
  before artifact packaging.
- CORE-NOVANESS LIMITED approved Apache-2.0 licence use for both Gemma 4 E2B
  and Qwen2.5 1.5B Instruct on 2026-06-19, approved by OYINLOLA OLUSAYO. On
  2026-06-27 the owner supplied the final Qwen LiteRT-LM release-candidate
  artifact, source revision, byte size, SHA-256 checksum, app-compatible signed
  manifest and public trust anchor. Qwen remains
  `APPROVED_ARTIFACT_PENDING_DEVICE_TESTS_AND_RELEASE_GATES`; Gemma remains
  deferred.
- `docs/operations/litertlm-artifact-build-plan.md` documents when the later
  `.litertlm` artifact build occurs and the packaging/signing/test process.
- `docs/legal/model-third-party-notices.md` provides draft third-party notice
  text for optional model packs and release checklist items.
- `docs/legal/licenses/apache-2.0.txt` provides local Apache License, Version
  2.0 text for release notice bundles when current Gemma 4 E2B or Qwen2.5 1.5B
  recommendations are used.
- Phase 7.5 compassionate safety redirection was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/safety/CompassionateSafetyRedirection.kt`.
- `CompassionateSafetyRedirectionPolicy` composes existing harm and relational
  assessments into safety redirection mode, reason, better-human pathway,
  response keys, recommended deterministic tools, unsafe-reminder replacement,
  memory/export/app-lock metadata and urgent-support flags.
- `AiGrowthModeEngine` now includes Phase 7.5 metadata in structured response
  handling: safety boundary applied/reason, user intent confidence, allowed
  discussion scope, better-human pathway, recommended tool, memory/export
  eligibility, step-up authentication and urgent-support requirement.
- Phase 7.5 post-generation validation blocks output that shames, diagnoses,
  assumes bad intent without evidence, encourages dependency on Bettamind or
  omits a safe next step when a boundary is applied.
- Compose resources now include English source strings plus matching draft
  target-locale fallback entries for compassionate safety redirection. These
  strings require qualified human review before production use.
- `docs/safety/compassionate-safety-redirection.md` documents the Phase 7.5
  safety-redirection contract, non-goals, AI integration, daily-tool/reminder
  integration, privacy/export/app-lock rules, localisation rules and
  verification coverage.
- `phaseSevenFiveCheck` was added as the Windows verification task for this
  slice. It does not begin Phase 8.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the pushed
  Phase 7.5 commit `cf4b24060aaea42bee3ee8a990f5f848ffd448ff`.
- Phase 8 safety and support bridge was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/support/SafetySupportBridge.kt`.
- `SafetySupportBridgePolicy` composes harm-safety, relational-boundary,
  compassionate-redirection and daily-tool decisions into deterministic support
  risk levels, support needs, voluntary support actions, local resource
  metadata, minimum-detail summaries and explicit preview plus app-lock step-up
  metadata before sensitive support sharing.
- `LocalSupportResourceCatalog` exposes local emergency, crisis/community,
  trusted-person and professional-support resource types without personal-data
  use, storage, automatic contact or help-contact claims.
- Compose resources now include English source strings plus matching draft
  target-locale fallback entries for the Phase 8 safety-support bridge. These
  strings require qualified human review before production use.
- `docs/safety/safety-support-bridge.md` documents the Phase 8 support bridge
  contract, non-goals, daily-tool integration, resource metadata,
  localisation rules and verification coverage.
- `phaseEightCheck` was added as the Windows verification task for this slice.
  It does not begin Phase 9.
- `AGENTS.md` and `.gitignore` now explicitly prohibit committing AI model
  weights, converted model artifacts, production packages, signing private
  keys, certificates, credentials, database dumps, real logs with personal
  content and real user data. `.gitignore` includes `.kotlin/` and common model
  artifact extensions such as `.litertlm`, `.task`, `.gguf`, `.onnx`,
  `.safetensors`, `.bin`, `.pt`, `.pth`, `.ckpt`, `.mlmodel` and `.mlpackage`.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the pushed
  Phase 8 commit `dff65d3e7773c5ab2c7aeb8b0d133c8030717c08`.
- Phase 9 encrypted export and sync policy was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/sync/EncryptedExportSync.kt`.
- `BettamindExportSyncPolicy` reviews export and sync eligibility for daily
  tools, growth/AI records, relational-boundary metadata, harm-safety metadata,
  support summaries, encrypted backups and calendar handoff receipts.
- `EncryptedSyncEnvelope`, `SyncManifest`, `DeviceRevocationRecord`,
  `CiphertextOnlyBackendContract`, `SyncConflictResolver` and encrypted backup
  envelope helpers define the Phase 9 shared contract.
- The optional FastAPI backend now exposes `POST /sync/envelopes` and accepts
  only strict encrypted envelope payloads with valid base64 nonce/ciphertext and
  matching SHA-256 ciphertext checksum. Extra plaintext fields are rejected.
- Compose Settings now shows an encrypted export/sync foundation block. Sync is
  not enabled by the UI and no automatic backend setup exists.
- `docs/security/phase-9-encrypted-export-sync.md` documents Phase 9 scope,
  implemented controls, backend contract, non-goals and verification coverage.
- `phaseNineCheck` was added as the Windows verification task for this slice.
  It does not begin Phase 10.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the pushed
  Phase 9 commit `b39ac2b3ec2f5d1b1a8e4d73a2b5fd98b4233926`.
- Phase 10 global localisation and accessibility policy was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/accessibility/GlobalLocalizationAccessibility.kt`.
- `BettamindLocaleAccessibilityCatalog`,
  `LocalizationReadinessPolicy`, `LocaleFormattingPolicy` and
  `AccessibilityReadinessPolicy` now define target-locale script/RTL/font
  fallback support, translation key completeness and qualified-review gating,
  locale date/plural behaviour and accessibility preference treatment.
- Compose Settings now exposes accessibility controls for readable typography,
  reduced motion and low-literacy copy, with screen-reader state descriptions.
- Compose resources now include matching Phase 10 accessibility keys across
  all target locale packs. Non-English entries remain draft fallbacks until
  qualified review records approve them for production.
- `docs/design/phase-10-localisation-accessibility.md` documents Phase 10
  scope, accessibility controls, localisation review boundary and verification
  coverage.
- `phaseTenCheck` was added as the Windows verification task for this slice.
  It does not begin Phase 11 speech.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the pushed
  Phase 10 commit `23674ca0492f8c37dc91b211856d568c05f0c70f`.
- Phase 11 optional offline speech policy was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/speech/OfflineSpeech.kt`.
- `OfflineSpeechPolicy` keeps text-only fallback available, models explicit
  microphone permission state, forbids raw-audio retention by default, requires
  app-lock metadata for sensitive transcripts and routes spoken input plus
  spoken output through relational-boundary and harm-safety decisions.
- `SpeechPackManager` requires user approval, publisher licence approval,
  approved licence identifiers, Ed25519-labeled signatures, SHA-256 artifact
  checksums, monotonic versions, revocation policy and removability for any
  optional local speech pack.
- Compose Settings now shows an optional offline speech foundation block.
  It does not request microphone permission, enable speech capture, install
  packs or add any cloud speech path.
- Compose resources now include matching Phase 11 speech keys across all
  target locale packs. Non-English entries remain draft fallbacks until
  qualified review records approve them for production.
- `docs/safety/phase-11-offline-speech.md` documents Phase 11 scope,
  implemented controls, non-goals, production requirements and verification
  coverage.
- `AGENTS.md` and `.gitignore` now also cover production speech packs and
  voice/audio pack artifacts. Raw audio file types are not blanket-ignored so
  they remain visible for review if they appear.
- `phaseElevenCheck` was added as the Windows verification task for this
  slice. It does not begin Phase 12 release readiness.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the pushed
  Phase 11 commit `5bba3dc74860aaa3077a53d542d15b4357f54f04`.
- Phase 12 release-readiness policy was added under
  `shared/src/commonMain/kotlin/org/bettamind/shared/release/ReleaseReadiness.kt`.
- `ReleaseReadinessPolicy` records required production gates for threat model,
  app-lock bypass, encryption-key protection, relational and harm red-team,
  reminders, notifications, timers, background privacy, calendar, export, sync,
  speech, localization/accessibility, low-resource performance, battery,
  thermal, memory, Android physical devices, Codemagic iOS, TestFlight, store
  metadata, rollback and artifact policy.
- `ReleaseRedTeamSuite` reuses existing deterministic policies to cover
  romantic attachment, sexualization, dangerous capability, self-harm,
  violence, jailbreak/policy bypass and unsafe spoken output cases.
- Compose Settings now shows a Phase 12 release-readiness foundation block and
  the app header reflects the Phase 12 status.
- Compose resources now include Phase 12 release-readiness keys across all
  target locale packs. Non-English entries remain draft fallbacks until
  qualified review records approve them for production.
- `docs/operations/phase-12-release-readiness.md` documents implemented
  gates, manual owner gates, artifact rules and verification scope.
- `phaseTwelveCheck` was added as the Windows verification task for this
  slice. It does not replace Codemagic, TestFlight or physical-device release
  evidence.
- Static website foundation added under `apps/website` with Astro and
  TypeScript, static output, required public pages, SEO/Open Graph metadata,
  sitemap, robots.txt, 404 route, Cloudflare Pages `_headers` and `_redirects`,
  and no analytics, ads, trackers, cookies, authentication, database or
  server-side rendering.
- Website pages created: Home, Features, Privacy Policy, Terms of Use, Safety,
  AI Transparency, Support, Data Deletion, Accessibility, Legal Notices, Brand,
  FAQ and 404.
- Website copy explicitly preserves Bettamind's account-free core use,
  offline-first core, optional AI, local encrypted storage, user-approved
  memory, no romantic/sexual companion positioning, no therapy/diagnosis/
  emergency/legal/financial-advice positioning and no fake account deletion
  flow.
- Shared Compose app UX integration now renders
  `shared/src/commonMain/composeResources/drawable/bettamind_mark.png` in the
  app header, replacing the plain circular placeholder while still leaving the
  canonical brand source untouched.
- Today now exposes interactive check-in metric controls, deterministic
  box-breathing steps, deterministic grounding steps and worksheet prompt
  selection. The current release-candidate app saves check-ins through
  encrypted app services after adult confirmation and does not add an
  unencrypted fallback.
- Grow now exposes Quick Guidance, Guided Reflection, Deep Exploration and
  Action-Only as selectable concern-prompt modes wired to `AiGrowthModeEngine`.
  With no installed local model, the UI shows deterministic fallback guidance,
  fallback reason, safety-boundary status and suggested next action without
  automatic memory, export, sync or notification.
- Support now exposes a deterministic concern assessment wired to
  `SafetySupportBridgeEngine`, showing local risk level, voluntary actions and
  local resource types while preserving no automatic contact.
- Compose source strings and matching draft fallback entries were added across
  all 10 locale resource files for the new app UX. Non-English strings remain
  draft fallbacks pending qualified human review.
- Production-facing scaffold/foundation/later-phase copy was removed from the
  shared Compose app source. Draft non-English Compose resource directories now
  mirror English source text for the English-only production scope until
  qualified review approves real locale strings.
- `BettamindAppServices` now wires Android and iOS app entry points to
  SQLCipher-backed encrypted daily-record services using Android Keystore and
  iOS Keychain key managers. Daily check-in persistence activates after local
  adult confirmation and does not create an unencrypted fallback.
- Settings now exposes platform integration states for local reminders,
  explicit calendar handoff, OS speech and optional signed model packs.
- Qwen2.5 1.5B Instruct is now the first optional local AI model-pack target.
  `BettamindModelPackTrustPolicy` now includes the owner-approved Qwen
  Ed25519 public trust anchor, while production model-pack availability remains
  blocked by runtime, device-evidence and final release gates.
- Owner evidence templates were added at
  `docs/operations/release-evidence-template.md` and
  `docs/operations/model-pack-owner-evidence-template.md`.
- The Qwen external release package was regenerated outside Git with an
  app-compatible camelCase `ModelPackManifest`, embedded base64 Ed25519
  signature, detached raw signature, regenerated checksums, Apache-2.0 licence
  copy and third-party notice at
  `C:\bettamind-model-release\release-qwen2.5-1.5b-v1`.
- `BettamindModelPackTrustPolicy` now records the approved Qwen Ed25519 public
  trust anchor and the shared Qwen recommendation points at
  `qwen2_5_1_5b_instruct_bettamind_v1.litertlm`.
- Codemagic `ios-simulator-unsigned` for commit `bdefd13` failed in
  `:shared:compileKotlinIosSimulatorArm64` because
  `BettamindIosServices.kt` used `NSDate().timeIntervalSince1970` as a
  property. The iOS source now calls `NSDate().timeIntervalSince1970()` so the
  Kotlin/Native macOS compiler can resolve the API.
- A later Codemagic `ios-simulator-unsigned` log failed in
  `:shared:compileKotlinIosSimulatorArm64` at `BettamindIosServices.kt:16:32`
  because `platform.posix.time` requires `ExperimentalForeignApi` opt-in. The
  iOS service now passes `nowEpochMillis = ::currentEpochMillis`, and
  `currentEpochMillis()` carries the scoped opt-in for `time(null)`.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for pushed commit
  `637b614` after the scoped iOS POSIX `ExperimentalForeignApi` opt-in fix.
- Optimized website image assets were generated from `brand/generated/`
  without overwriting `brand/source/bettamind-logo-master.png`.
- `docs/operations/website-cloudflare-pages.md` documents Cloudflare Pages
  build settings, DNS/custom-domain steps, public store URLs, security headers
  and policy alignment.
- Cloudflare website deploy log from 2026-06-25 showed the static build and
  verification passed, then deploy failed because `_redirects` used an
  absolute source URL and the configured deploy command was `npx wrangler
  deploy`, which treated the static Astro site as a Worker. `_redirects` now
  contains only relative path redirects, website verification checks
  `_redirects` source paths, and docs now say to leave the Cloudflare Pages
  deploy command blank for Git deployments.

## Important files

- `AGENTS.md`
- `.gitignore`
- `docs/specification/bettamind-locked-specification.md`
- `docs/architecture/local-ai-no-model-fallback.md`
- `docs/planning/implementation-plan.md`
- `docs/planning/archive/implementation-plan-before-phase-6x.md`
- `docs/planning/phase-6x-integration-audit.md`
- `docs/planning/phase-6x-integration-plan.md`
- `docs/planning/roadmap-amendment-phase-6x.md`
- `docs/planning/phase-7-to-12-continuation-plan.md`
- `docs/planning/requirements-traceability.md`
- `docs/planning/risk-register.md`
- `docs/design/brand-and-colour-decision.md`
- `docs/design/font-sources.md`
- `docs/working-notes/project-memory.md`
- `scripts/generate_brand_assets.py`
- `shared/src/commonMain/kotlin/org/bettamind/shared/App.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/BettamindAppServices.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/design/BettamindTheme.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/design/BettamindColorTokens.kt`
- `shared/src/commonMain/composeResources/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/privacy/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/privacy/PrivacyLockTest.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/growth/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/daily/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/knowledge/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/ai/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/ai/AiGrowthModes.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/ai/LocalAiModelRecommendation.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/ai/ModelPackTrustPolicy.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/safety/`
- `shared/src/commonMain/kotlin/org/bettamind/shared/safety/CompassionateSafetyRedirection.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/support/SafetySupportBridge.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/sync/EncryptedExportSync.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/accessibility/GlobalLocalizationAccessibility.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/speech/OfflineSpeech.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/release/ReleaseReadiness.kt`
- `shared/src/commonTest/kotlin/org/bettamind/shared/release/ReleaseReadinessTest.kt`
- `shared/src/commonMain/kotlin/org/bettamind/shared/security/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/privacy/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/growth/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/daily/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/knowledge/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/ai/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/ai/AiGrowthModesTest.kt`
- `shared/src/commonTest/kotlin/org/bettamind/shared/ai/LocalAiModelRecommendationTest.kt`
- `shared/src/commonTest/kotlin/org/bettamind/shared/safety/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/safety/CompassionateSafetyRedirectionTest.kt`
- `shared/src/commonTest/kotlin/org/bettamind/shared/support/SafetySupportBridgeTest.kt`
- `shared/src/commonTest/kotlin/org/bettamind/shared/sync/EncryptedExportSyncTest.kt`
- `shared/src/commonTest/kotlin/org/bettamind/shared/accessibility/GlobalLocalizationAccessibilityTest.kt`
- `shared/src/commonTest/kotlin/org/bettamind/shared/speech/OfflineSpeechTest.kt`
- `backend/app/api/sync.py`
- `backend/app/schemas/sync.py`
- `backend/tests/test_sync.py`
- `shared/src/androidMain/kotlin/org/bettamind/shared/privacy/`
- `shared/src/androidMain/kotlin/org/bettamind/shared/BettamindAndroidServices.kt`
- `shared/src/iosMain/kotlin/org/bettamind/shared/privacy/`
- `shared/src/iosMain/kotlin/org/bettamind/shared/BettamindIosServices.kt`
- `shared/src/iosTest/kotlin/org/bettamind/shared/privacy/`
- `shared/src/nativeInterop/cinterop/`
- `androidApp/src/main/res/`
- `iosApp/iosApp/Assets.xcassets/`
- `iosApp/iosApp.xcodeproj/project.pbxproj`
- `docs/security/phase-3-encrypted-storage-spike.md`
- `docs/security/phase-5-signed-knowledge-packs.md`
- `docs/security/phase-6-ai-model-manager.md`
- `docs/operations/local-ai-model-pack-release.md`
- `docs/operations/model-license-approval-records.md`
- `docs/operations/model-pack-owner-evidence-template.md`
- `docs/operations/release-evidence-template.md`
- `docs/operations/litertlm-artifact-build-plan.md`
- `docs/operations/model-pack-manifest-template.json`
- `docs/legal/model-third-party-notices.md`
- `docs/legal/licenses/apache-2.0.txt`
- `docs/security/phase-6-4-app-privacy-lock.md`
- `docs/safety/relational-boundaries.md`
- `docs/safety/harmful-intent-and-dangerous-capability-policy.md`
- `docs/safety/compassionate-safety-redirection.md`
- `docs/safety/safety-support-bridge.md`
- `docs/safety/phase-11-offline-speech.md`
- `docs/operations/phase-12-release-readiness.md`
- `docs/security/phase-9-encrypted-export-sync.md`
- `docs/design/phase-10-localisation-accessibility.md`
- `docs/product/phase-6-6-deterministic-daily-tools.md`
- `codemagic.yaml`
- `.github/workflows/phase-1-checks.yml`
- `apps/website/`
- `docs/operations/website-cloudflare-pages.md`

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
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --stacktrace --console=plain`
- `.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --stacktrace --console=plain`
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
- `.\gradlew.bat :androidApp:assembleDebug --no-daemon --no-configuration-cache --stacktrace --console=plain`
- `.\gradlew.bat :androidApp:lintDebug --no-daemon --no-configuration-cache --stacktrace --console=plain`
- `.\gradlew.bat phaseSixFourCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --stacktrace --console=plain`
  completed on Windows after Phase 6.4 changes, with iOS Native compile/test
  tasks still skipped because SQLCipher cinterop cannot be processed on
  `mingw_x64`.
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the Phase 6.5 relational-boundary policy and tests were added.
- `.\gradlew.bat phaseSixFiveCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed. On Windows, iOS Native targets remain disabled because SQLCipher
  cinterop requires macOS.
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the Phase 6.6 deterministic daily-tool foundation and tests
  were added.
- `.\gradlew.bat phaseSixSixCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed. On Windows, iOS Native targets remain disabled because SQLCipher
  cinterop requires macOS.
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the Phase 6.7 harmful-intent safeguards and tests were added.
- `.\gradlew.bat phaseSixSevenCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed. On Windows, iOS Native targets remain disabled because SQLCipher
  cinterop requires macOS.
- `git diff --check` reported no whitespace errors, only normal Windows
  LF-to-CRLF warnings.
- `git ls-files | rg "\.(tflite|litertlm|gguf|onnx|bin|safetensors|model|mlmodel|task)$"`
  found no tracked model-weight artifacts.
- `git diff --check` reported no whitespace errors, only normal Windows
  LF-to-CRLF warnings.
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
- Roadmap reconciliation docs-only checks:
  `git diff --check` passed with normal Windows LF-to-CRLF warnings;
  `git diff --name-only -- shared androidApp iosApp backend` produced no
  output; `git diff --exit-code --
  docs\planning\archive\implementation-plan-before-phase-6x.md` passed.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 7 changes.
- `.\gradlew.bat :shared:compileDebugKotlinAndroid --rerun-tasks --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 7 changes.
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the Phase 7 AI growth-mode tests were added.
- `.\gradlew.bat phaseSevenCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 7 changes.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --stacktrace --console=plain`
  completed on Windows after Phase 7 changes, with iOS Native compile/test
  tasks still skipped because SQLCipher cinterop cannot be processed on
  `mingw_x64`.
- From `backend/`: `.\.venv\Scripts\ruff.exe check .`
- From `backend/`: `.\.venv\Scripts\mypy.exe app`
- From `backend/`: `.\.venv\Scripts\pytest.exe`
- `git diff --check` reported no whitespace errors, only normal Windows
  LF-to-CRLF warnings.
- `git ls-files | rg "\.(tflite|litertlm|gguf|onnx|bin|safetensors|model|mlmodel|task)$"`
  found no tracked model-weight artifacts.
- `rg -l "ai_growth_modes_title" shared\src\commonMain\composeResources | Measure-Object`
  reported 10 resource files with the Phase 7 string key.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the local AI model-pack recommendation policy and artifact
  governance updates.
- `.\gradlew.bat :shared:testDebugUnitTest --tests org.bettamind.shared.ai.LocalAiModelRecommendationTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the recommendation policy tests were added.
- `.\gradlew.bat phaseSevenCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the local AI model-pack recommendation policy and AGENTS
  artifact-governance updates. On Windows, iOS Native targets remain disabled
  because SQLCipher cinterop requires macOS.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 7.5 Kotlin and Compose resource changes.
- `.\gradlew.bat :shared:testDebugUnitTest --tests org.bettamind.shared.safety.CompassionateSafetyRedirectionTest --tests org.bettamind.shared.ai.AiGrowthModesTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the Phase 7.5 safety-redirection and AI metadata tests were
  added.
- Locale resource parity check found no missing string keys across the 10
  Compose `strings.xml` files.
- `.\gradlew.bat phaseSevenFiveCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 7.5 changes.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --stacktrace --console=plain`
  completed on Windows after Phase 7.5 changes, with iOS Native compile/test
  tasks still skipped because SQLCipher cinterop cannot be processed on
  `mingw_x64`.
- `git diff --check` reported no whitespace errors, only normal Windows
  LF-to-CRLF warnings.
- `git ls-files | rg "\.(tflite|litertlm|gguf|onnx|bin|safetensors|model|mlmodel|task|pt|pth|ckpt|mlpackage)$"`
  found no tracked model-weight artifacts.
- `.\gradlew.bat --version --no-daemon --no-configuration-cache --console=plain`
  passed during Phase 8 diagnostics.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 8 changes, with expected Windows iOS cinterop target
  skips.
- `.\gradlew.bat :shared:testDebugUnitTest --tests org.bettamind.shared.support.SafetySupportBridgeTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 8 support-bridge tests were added.
- XML string-resource parity check parsed all 10 Compose `strings.xml` files
  and found no source string keys missing.
- `.\gradlew.bat phaseEightCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 8 changes.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --stacktrace --console=plain`
  completed on Windows after Phase 8 changes, with iOS Native compile/test
  tasks still skipped because SQLCipher cinterop cannot be processed on
  `mingw_x64`.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 9 changes, with expected Windows iOS cinterop target
  skips.
- `.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 9 changes.
- `.\gradlew.bat :shared:testDebugUnitTest --tests org.bettamind.shared.sync.EncryptedExportSyncTest --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after the Phase 9 encrypted export/sync tests were added. Two earlier
  short-timeout attempts exceeded the tool timeout and were terminated with
  `taskkill` before the successful longer-timeout run.
- From `backend/`: `.\.venv\Scripts\ruff.exe check .` passed after Phase 9
  backend sync endpoint changes.
- From `backend/`: `.\.venv\Scripts\mypy.exe app` passed after Phase 9 backend
  sync endpoint changes.
- From `backend/`: `.\.venv\Scripts\pytest.exe` passed after Phase 9 backend
  sync endpoint changes.
- `.\gradlew.bat phaseNineCheck --no-daemon --no-configuration-cache --stacktrace --console=plain`
  passed after Phase 9 changes.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --stacktrace --console=plain`
  completed on Windows after Phase 9 changes, with iOS Native compile/test
  tasks still skipped because SQLCipher cinterop cannot be processed on
  `mingw_x64`.
- XML string-resource parity check parsed all 10 Compose `strings.xml` files
  and found no source string keys missing after Phase 9 strings were added.
- `git diff --check` reported no whitespace errors after Phase 9 changes, only
  normal Windows LF-to-CRLF warnings.
- `git ls-files | rg "\.(tflite|litertlm|gguf|onnx|bin|safetensors|model|mlmodel|task|pt|pth|ckpt|mlpackage)$"`
  found no tracked model-weight artifacts after Phase 9 changes.
- `.\gradlew.bat --version --no-daemon --console=plain` passed during Phase
  10 verification after earlier long Gradle compile attempts were diagnosed.
- `.\gradlew.bat :shared:tasks --all --no-daemon --no-configuration-cache --max-workers=1 --console=plain`
  passed during Phase 10 verification and showed the expected Windows iOS
  cinterop target skips.
- `.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after Phase 10 shared Kotlin and Compose resource changes.
- `.\gradlew.bat :shared:testDebugUnitTest --tests org.bettamind.shared.accessibility.GlobalLocalizationAccessibilityTest --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after Phase 10 localisation/accessibility policy tests were added.
- XML string-resource parity check parsed all 10 Compose `strings.xml` files
  and found no source string keys missing after Phase 10 strings were added.
- `.\gradlew.bat phaseTenCheck --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after Phase 10 changes.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  completed on Windows after Phase 10 changes, with iOS Native compile/test
  tasks still skipped because SQLCipher cinterop cannot be processed on
  `mingw_x64`.
- `.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after Phase 11 shared Kotlin and Compose resource changes.
- `.\gradlew.bat :shared:testDebugUnitTest --tests org.bettamind.shared.speech.OfflineSpeechTest --tests org.bettamind.shared.accessibility.GlobalLocalizationAccessibilityTest --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after Phase 11 speech tests and updated localisation review tests were
  added. An earlier run caught an overbroad safe-output test phrase, which was
  corrected before the passing rerun.
- XML string-resource parity check parsed all 10 Compose `strings.xml` files
  and found no source string keys missing after Phase 11 strings were added.
- `.\gradlew.bat phaseElevenCheck --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after Phase 11 changes.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  completed on Windows after Phase 11 changes, with iOS Native compile/test
  tasks still skipped because SQLCipher cinterop cannot be processed on
  `mingw_x64`.
- `git diff --check` reported no whitespace errors after Phase 11 changes,
  only normal Windows LF-to-CRLF warnings.
- `git ls-files | rg "\.(tflite|litertlm|gguf|onnx|bin|safetensors|model|mlmodel|task|pt|pth|ckpt|mlpackage|speechpack|voicepack)$"`
  found no tracked model-weight, model-pack or speech-pack artifacts after
  Phase 11 changes.
- `.\gradlew.bat --no-daemon --stacktrace --console=plain :shared:compileDebugKotlinAndroid`
  passed after Phase 12 shared Kotlin and Compose resource changes.
- `.\gradlew.bat --no-daemon --stacktrace --console=plain :shared:testDebugUnitTest`
  passed after Phase 12 release-readiness and localisation-review tests were
  added.
- `.\gradlew.bat --no-daemon --stacktrace --console=plain :shared:testDebugUnitTest --tests org.bettamind.shared.release.ReleaseReadinessTest --rerun-tasks`
  explicitly executed and passed the Phase 12 release-readiness test class.
- `.\gradlew.bat --no-daemon --stacktrace --console=plain phaseTwelveCheck`
  passed after Phase 12 changes.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for the Phase 12
  release-readiness foundation on 2026-06-25.
- `codemagic.yaml` now includes a manual `ios-testflight-release` workflow for
  App Store signing, signed IPA build and App Store Connect upload after
  owner-managed Apple/Codemagic setup.
- `iosApp/iosApp/Info.plist` now uses Xcode version build settings for
  `CFBundleShortVersionString` and `CFBundleVersion`; Codemagic stamps concrete
  TestFlight values during the release workflow.
- `docs/operations/testflight-readiness.md` documents the required secure
  Codemagic setup and internal TestFlight smoke checklist.
- `git diff --check` reported no whitespace errors after Phase 12 changes,
  only normal Windows LF-to-CRLF warnings.
- `rg --files --glob '!**/.git/**' --glob '!**/build/**' | rg "\.(litertlm|tflite|task|gguf|onnx|safetensors|bin|pt|pth|ckpt|mlmodel|mlpackage|keystore|p12|mobileprovision|cer|env|db|sqlite|aab|ipa|xcarchive|wav|mp3|m4a|flac)$"`
  found no model, signing, secret, database, audio-pack or store-archive
  artifacts in the working tree after Phase 12 changes.
- `python apps\website\scripts\generate-assets.py` generated optimized public
  website brand assets from `brand/generated/`.
- From `apps/website/`: `npm install --package-lock-only --ignore-scripts
  --no-audit --no-fund` generated `package-lock.json`.
- From `apps/website/`: `npm ci --ignore-scripts --no-audit --no-fund`
  completed after deleting a corrupted partial `node_modules` install.
- From `apps/website/`: `npm run build` passed with Astro check reporting 0
  errors, 0 warnings and 0 hints, then built 13 static pages and the sitemap.
- From `apps/website/`: `npm run verify` passed, rebuilding the site and
  verifying 13 HTML files, required routes, sitemap, internal links and
  wording guards.
- From `apps/website/`: `npm run verify` passed after the Cloudflare
  `_redirects` fix and deploy-command documentation update.
- `rg -n "[^\x00-\x7F]" apps\website docs\operations\website-cloudflare-pages.md .gitignore`
  returned no non-ASCII matches.
- Website UI-label typography was refined so buttons, navigation labels,
  eyebrow labels, store badges, mobile menu labels and footer links use a
  website-served Atkinson Hyperlegible stack instead of inheriting the
  body-first Noto Sans stack or relying on Windows-installed fonts. Long-form
  website copy remains on the existing readable Noto Sans fallback stack.
- From `apps/website/`: `npm run verify` passed after the website-served
  Atkinson Hyperlegible UI-label typography refinement.
- Owner confirmed the iOS bundle identifier
  `com.corenovaness.bettamind`. `iosApp/Config/Debug.xcconfig`,
  `iosApp/Config/Release.xcconfig`, release-readiness docs and planning docs
  now use that identifier.
- `codemagic.yaml` now uses `com.corenovaness.bettamind` for the
  `ios-simulator-unsigned` default iOS bundle ID as well.
- Codemagic `ios-testflight-release` built and signed `Bettamind.ipa` for
  bundle ID `com.corenovaness.bettamind` and version code 4, then App Store
  Connect rejected the upload because it was built with Xcode 16.4/iOS 18.5
  SDK. This proved signing was configured but the upload SDK requirement was
  not met.
- `codemagic.yaml` now pins both iOS workflows to Xcode 26.0.
- `gradle/libs.versions.toml` now pins Kotlin to 2.2.21 for documented Xcode
  26.0 compatibility.
- After the Xcode 26.0/Kotlin 2.2.21 toolchain fix, `git diff --check`
  reported no whitespace errors, only normal Windows LF-to-CRLF warnings.
- After the Xcode 26.0/Kotlin 2.2.21 toolchain fix, the restricted artifact
  scan found no model, signing, secret, database, audio-pack or store-archive
  artifacts.
- Local Windows Gradle verification after the Xcode 26.0/Kotlin 2.2.21 bump
  timed out for both `.\gradlew.bat :shared:testDebugUnitTest --no-daemon
  --no-configuration-cache --max-workers=1 --stacktrace --console=plain` and
  `.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon
  --no-configuration-cache --max-workers=1 --stacktrace --console=plain`.
  Gradle daemons were stopped cleanly with `.\gradlew.bat --stop`. Required
  proof for this release-toolchain change is Codemagic macOS using Xcode 26.0.
- After the first TestFlight install crashed immediately on iPhone launch,
  `codemagic.yaml` gained a signed IPA framework inspection step. A manual
  SQLCipher embed phase was tried and then removed after Xcode 26 reported
  duplicate output for `Bettamind.app/Frameworks/SQLCipher.framework`, proving
  the Swift Package integration already produces that framework.
- `git diff --check` reported no whitespace errors after the iOS bundle-ID
  config fix, only normal Windows LF-to-CRLF warnings.
- `rg --files --glob '!**/.git/**' --glob '!**/build/**' | rg
  "\.(litertlm|tflite|task|gguf|onnx|safetensors|bin|pt|pth|ckpt|mlmodel|mlpackage|keystore|p12|mobileprovision|cer|env|db|sqlite|aab|ipa|xcarchive|wav|mp3|m4a|flac)$"`
  found no model, signing, secret, database, audio-pack or store-archive
  artifacts after the iOS bundle-ID config fix.
- `.\gradlew.bat :shared:testDebugUnitTest --tests
  org.bettamind.shared.release.ReleaseReadinessTest --no-daemon
  --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the iOS bundle-ID config fix.
- `.\gradlew.bat :androidApp:assembleDebug :androidApp:lintDebug
  --no-daemon --no-configuration-cache --max-workers=1 --stacktrace
  --console=plain` passed after the iOS bundle-ID config fix.
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon
  --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the iOS bundle-ID config fix.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --console=plain`
  passed after the build 8 TestFlight launch-crash resource-loading patch.
  Windows still disabled iOS native cinterop targets, as expected.
- `git diff --check` reported no whitespace errors after the build 8
  TestFlight launch-crash resource-loading patch, only normal Windows
  LF-to-CRLF warnings.
- PowerShell XML parsing confirmed `iosApp/iosApp/Info.plist` contains
  `CADisableMinimumFrameDurationOnPhone` after the build 10 symbolicated
  Compose plist-sanity fix.
- `git diff --check` reported no whitespace errors after the build 10
  Compose plist-sanity fix, only normal Windows LF-to-CRLF warnings.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the app UX integration, with expected Windows iOS cinterop
  target skips.
- `.\gradlew.bat :shared:compileDebugKotlinAndroid --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the app UX integration.
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the app UX integration.
- `.\gradlew.bat :androidApp:assembleDebug --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the app UX integration.
- `.\gradlew.bat :androidApp:lintDebug --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the app UX integration.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  completed on Windows after the app UX integration, with iOS Native
  compile/test tasks still skipped because SQLCipher cinterop cannot be
  processed on `mingw_x64`.
- `.\gradlew.bat phaseTwelveCheck --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the app UX integration.
- String resource parity check reported all source string keys present across
  10 Compose `strings.xml` files after the app UX integration.
- Restricted artifact scan found no model, signing, secret, database,
  audio-pack or store-archive artifacts after the app UX integration.
- Hardcoded visible-string scan for `Text("...")`, direct text-field labels and
  direct placeholders in `App.kt` returned no matches after the app UX
  integration.
- `git diff --check` reported no whitespace errors after the app UX
  integration, only normal Windows LF-to-CRLF warnings.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the production-readiness app integration, with expected Windows
  iOS cinterop target skips.
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the production-readiness app integration.
- `.\gradlew.bat :androidApp:assembleDebug :androidApp:lintDebug --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the production-readiness app integration.
- `.\gradlew.bat phaseTwelveCheck --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the production-readiness app integration.
- `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  completed after the production-readiness app integration, with iOS native
  compile/test tasks still skipped on Windows because SQLCipher cinterop
  cannot be processed on `mingw_x64`.
- PowerShell XML parsing confirmed all `values-*` Compose resource folders
  contain the complete English source key set after the English-only
  production-scope change.
- Restricted artifact scan found no model, signing, secret, database,
  audio-pack or store-archive artifacts outside ignored build outputs after
  the production-readiness app integration.
- `git diff --check` reported no whitespace errors after the
  production-readiness app integration, only normal Windows LF-to-CRLF
  warnings.
- Qwen external manifest verification parsed
  `C:\bettamind-model-release\release-qwen2.5-1.5b-v1\qwen2_5_1_5b_instruct_bettamind_v1.manifest.json`
  as strict JSON and verified the embedded Ed25519 signature against the
  app-canonical unsigned `ModelPackManifest` bytes.
- The regenerated Qwen release package has artifact SHA-256
  `FAA60663B333290C1496C499828B21D3E3254A788CACD8CCE917CE0F761A2DC9`,
  signed manifest SHA-256
  `69F1E42B6FF9F67C362FEC21A283DA86726603391FA5EF07D27792F73029E324` and
  detached signature SHA-256
  `704937BDA6B9F68B1018562FDBFD58EEA12E2AEE46357FEDEB147800C77D3078`.
- `.\gradlew.bat :shared:testDebugUnitTest --tests
  org.bettamind.shared.ai.ModelPackTrustPolicyTest --tests
  org.bettamind.shared.ai.LocalAiModelRecommendationTest --no-daemon
  --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  exceeded the tool timeout after writing test XML reports; the reports showed
  6 tests, 0 failures and 0 errors for the targeted Qwen trust/recommendation
  tests. A stale Java worker from the first timeout was stopped before reruns.
- `.\gradlew.bat :androidApp:assembleDebug :androidApp:lintDebug --no-daemon
  --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the Qwen trust-anchor and release-record update, with expected
  Windows iOS cinterop target skips.
- `.\gradlew.bat phaseTwelveCheck --no-daemon --no-configuration-cache
  --max-workers=1 --stacktrace --console=plain` passed after the Qwen
  trust-anchor and release-record update, with expected Windows iOS cinterop
  target skips.
- `git diff --check` reported no whitespace errors after the Qwen
  trust-anchor and release-record update, only normal Windows LF-to-CRLF
  warnings.
- Restricted artifact scan found no model, signing, secret, database,
  audio-pack or store-archive artifacts inside the repository after the Qwen
  trust-anchor and release-record update.
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the iOS POSIX `ExperimentalForeignApi` opt-in fix, with expected
  Windows iOS cinterop target skips.
- `.\gradlew.bat phaseTwelveCheck --no-daemon --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  passed after the iOS POSIX `ExperimentalForeignApi` opt-in fix, with expected
  Windows iOS cinterop target skips.
- `git diff --check` reported no whitespace errors after the iOS POSIX
  `ExperimentalForeignApi` opt-in fix, only normal Windows LF-to-CRLF
  warnings.
- Restricted artifact scan found no model, signing, secret, database,
  audio-pack or store-archive artifacts inside the repository after the iOS
  POSIX `ExperimentalForeignApi` opt-in fix.
- `Get-Process -Name java,gradle -ErrorAction SilentlyContinue` returned no
  leftover Java or Gradle worker processes after the iOS POSIX
  `ExperimentalForeignApi` opt-in fix.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for pushed commit
  `637b614` after the iOS POSIX `ExperimentalForeignApi` opt-in fix.

## Known blockers and limitations

- iOS cannot be fully built locally on Windows. Every shared/iOS change still
  requires Codemagic `ios-simulator-unsigned`.
- Owner confirmed Codemagic `ios-simulator-unsigned` passed for Phase 7 and
  local model recommendation policy through commit `d1811db`, and for the
  Phase 7.5 safety-redirection commit `cf4b240`.
- Windows cannot validate the iOS `LocalAuthentication` adapter or SwiftUI
  inactive-scene shield.
- The local Windows
  `.\gradlew.bat :shared:compileTestKotlinIosSimulatorArm64 --no-daemon --no-configuration-cache --stacktrace --console=plain`
  check timed out during Phase 6.5 verification and was terminated with no
  remaining Java/Gradle processes. The required iOS proof remains Codemagic.
- Production Bettamind PIN/passphrase setup still needs an audited Argon2id
  provider for Android and iOS before being enabled for real users. The shared
  verifier and rate limiter are present, and tests use a fake Argon2id-labeled
  deriver only.
- Existing Android Keystore and iOS Keychain adapters remain the Phase 3
  storage-key stores. Phase 6.4 gates release through local authentication but
  does not yet add platform OS-level auth-bound key attributes to those
  existing key records.
- Phase 6.5 relational-boundary policy is a deterministic heuristic foundation.
  It needs owner, safety and localization review before production use and
  before Phase 7 response-mode prompts rely on it.
- Phase 6.6 deterministic daily tools are a shared foundation. App-level
  encrypted check-in persistence is now wired to Android and iOS SQLCipher
  services after adult confirmation. Platform reminder scheduling, full
  calendar handoff behavior, daily-record delete/export UI and device evidence
  remain release hardening work.
- Phase 6.7 harmful-intent safeguards are a deterministic heuristic foundation.
  They need owner, safety, legal and localization review before production use
  and before Phase 7 response-mode prompts rely on them.
- Phase 7 is implemented against the existing `LocalAiRuntime` boundary and
  unavailable-runtime/no-model path. The recommendation policy now names
  Qwen2.5 1.5B Instruct as the first release-candidate pack and defers Gemma 4
  E2B, but exact artifacts, final licence re-check, checksums, trust anchors,
  signing keys, delivery governance and prompt/output review remain future
  owner/release work.
- Phase 7.5 is a deterministic heuristic foundation. Compassionate
  safety-redirection categories, fallback keys, reminder replacements and AI
  metadata should receive owner, safety, legal and qualified localization
  review before production use.
- Phase 11 shared Kotlin, Compose resources and Gradle task changes cannot be
  fully validated for iOS on Windows. The future pushed Phase 11 commit will
  require Codemagic `ios-simulator-unsigned`.
- Phase 9 is a contract foundation. Production sync still needs durable
  encrypted backend persistence, account/device provisioning if offered,
  device key exchange, retention/deletion policy, deployment secrets,
  operational monitoring and release security review. None of those secrets or
  runtime databases should be committed.
- Phase 10 is a localisation/accessibility foundation. Target locale key
  coverage is complete at the resource level. Production scope is English-only
  for now, and non-English Compose resource directories mirror English source
  text until qualified human review approves production translations for
  safety, crisis, legal, privacy, consent, relational-boundary, support,
  export/sync and daily-tool copy.
- Phase 11 is an optional offline speech foundation. Production speech still
  needs platform STT/TTS adapters, microphone permission copy, OS voice support
  review, speech-pack licence approvals, signed artifacts, device tests and
  store privacy-label review before real release.
- Phase 12 is a repository-side release-readiness foundation. Production
  release remains blocked until owner evidence exists for Android physical
  devices, low-resource performance, battery/thermal/memory behavior,
  signed TestFlight installation and smoke testing, store metadata, privacy
  labels, screenshots, support/safety claims, qualified translation review and
  rollback.
- The signed TestFlight workflow cannot pass until the owner confirms the
  App Store Connect app record, Codemagic integration
  `bettamind-app-store-connect`, matching App Store signing identities and the
  `bettamind-testflight` variable group all use
  `com.corenovaness.bettamind`.
- Initial Phase 12 Gradle verification hit a Kotlin compile-cache delete
  failure and a short no-daemon timeout. `.\gradlew.bat --stop` stopped two
  daemons; reruns with longer timeouts passed compile, shared tests and
  `phaseTwelveCheck`.
- A later local `.\gradlew.bat phaseTwelveCheck --no-daemon
  --no-configuration-cache --max-workers=1 --stacktrace --console=plain`
  run after the iOS bundle-ID config fix exceeded the 10-minute tool timeout.
  The remaining Java worker exited on its own. Targeted release-readiness
  tests, full shared Android unit tests, Android assemble and Android lint
  passed afterward.
- Local Windows Gradle commands after the Kotlin 2.2.21 patch did not return
  within the tool timeout. This is unresolved locally and must be validated by
  Codemagic because the actual release requirement is an iOS 26 SDK archive on
  macOS.
- After the build 8 TestFlight launch-crash resource-loading patch, local
  Windows `:androidApp:assembleDebug` and
  `:shared:compileDebugKotlinAndroid` attempts timed out without useful Gradle
  output. Stale Java processes from those attempts were terminated. Android and
  iOS release validation still need Codemagic or a stable local Gradle rerun.
- The first TestFlight build installed but crashed on launch on a physical
  iPhone. A missing embedded SQLCipher dynamic framework was suspected, but
  Xcode 26 later reported duplicate output for the SQLCipher framework when a
  manual embed phase was added. If the next build passes IPA inspection but
  still crashes, collect the device/TestFlight crash report and inspect the
  first `Exception Type`, `Termination Reason` and `Dyld Error Message` lines.
- During Phase 8 local verification, initial short-timeout Gradle task runs
  exceeded the tool timeout and left stale Java/Gradle workers, which were
  terminated with `taskkill`. Re-running with longer timeouts passed targeted
  tests, `phaseEightCheck` and the Windows-side iOS simulator test-compile
  task.
- Phase 4 does not yet persist narrative content. Storage status still reports
  encrypted storage unavailable until a separate approved pass wires the
  platform encrypted store into the growth flow. There is no unencrypted
  fallback.
- The new daily-tool UI now saves check-ins through encrypted app services
  after adult confirmation. Worksheet/timer/calendar record persistence,
  delete/export UI and physical-device evidence still need completion before
  production release.
- The new Grow concern prompt runs through `AiGrowthModeEngine`, but real local
  AI remains unavailable in the app until the platform LiteRT-LM bridge,
  install/load/generate/remove flow, Android physical-device evidence, iOS
  TestFlight evidence, low-storage/interrupted-import behavior,
  battery/thermal/memory observations and rollback/revocation records pass for
  the signed Qwen package.
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
- Locale packs remain key-complete, but production scope is English-only.
  Non-English resource directories mirror English source text until qualified
  human review approves production translations.
- `java` is not available on global `PATH`; current checks used JetBrains'
  bundled JBR at `C:\Program Files\JetBrains\PyCharm 2025.2.3\jbr`.
- Local Windows Gradle checks require a non-committed `local.properties` with
  `sdk.dir=C\:\\Users\\HP\\AppData\\Local\\Android\\Sdk`.
- Website production deployment is not complete until the owner creates the
  Cloudflare Pages project, configures `www.bettamind.com`, verifies HTTPS and
  uses the deployed public URLs in store metadata.
- Website Cloudflare Pages Git deployment should use build command
  `npm ci && npm run verify`, output directory `dist`, root directory
  `apps/website` and no deploy command. The apex `bettamind.com` to
  `www.bettamind.com` redirect must be configured as a Cloudflare zone redirect
  rule, not in `_redirects`.

## Manual owner actions

- Run Codemagic `ios-simulator-unsigned` for pushed commits that change shared
  Kotlin, Compose resources, `iosApp`, Gradle configuration that can affect
  iOS, or Codemagic iOS workflow files.
- Run Codemagic `ios-simulator-unsigned` after any future pushed commit that
  changes shared Kotlin, Compose resources, `iosApp`, Gradle configuration that
  can affect iOS, or Codemagic iOS workflow files.
- Configure the owner Apple Developer, App Store Connect and Codemagic secure
  setup documented in `docs/operations/testflight-readiness.md`.
- Run Codemagic `ios-testflight-release` against the pushed release-candidate
  commit, then record the uploaded App Store Connect build number and
  TestFlight smoke-test evidence.
- Review Phase 7.5 compassionate safety-redirection reasons, fallback copy,
  unsafe-reminder replacements, AI metadata semantics and post-generation
  validator categories before production localization or store review.
- Review Phase 8 support-bridge risk levels, support action labels, local
  resource labels, minimum-detail summary preview and no-auto-contact wording
  before production localization or store review.
- Review Phase 9 export/sync settings copy, backend envelope contract,
  sensitive export preview semantics, daily/relational/harm default exclusions,
  conflict handling and device revocation flow before production sync work.
- Review Phase 10 accessibility settings copy, low-literacy mode semantics,
  reduced-motion treatment, screen-reader state descriptions, script font
  fallback plan and locale readiness gating before production localization or
  store review.
- Review Phase 11 offline speech copy, microphone permission language,
  no-raw-audio-retention rule, sensitive-transcript app-lock behavior,
  spoken-output persona boundary and speech-pack licence/signing governance
  before production speech work.
- Complete Phase 12 production release evidence before any production release:
  Android physical-device matrix, low-resource startup/memory behavior,
  battery/thermal review, TestFlight installation/smoke testing, store
  metadata and privacy labels, screenshots, support/safety claims, qualified
  human review for any non-English production locale strings and
  rollback/revocation process. Use
  `docs/operations/release-evidence-template.md` as the record format.
- Review Phase 6.5 relational-boundary categories and fallback copy before
  production localization or Phase 7 AI response-mode prompts.
- Review Phase 6.6 daily-tool copy, reminder defaults, quiet-hours defaults
  and calendar handoff policy before production localization or store review.
- Review Phase 6.7 harmful-intent categories, refusal/fallback identifiers,
  export/support policy and safety copy before production localization or Phase
  7 AI response-mode prompts.
- Decide whether production Bettamind PIN/passphrase setup should be enabled in
  a later hardening pass after an audited Argon2id provider is selected.
- Provide `brand/source/bettamind-logo-master.svg` if a vector master exists,
  then regenerate assets from that source in a later approved pass.
- Replace the placeholder Android application ID with an owner-owned value
  before Android release work. The iOS bundle ID is already owner-confirmed as
  `com.corenovaness.bettamind`.
- Provide owner-approved production knowledge-pack trust anchors and content
  governance before accepting real public packs.
- Complete Qwen production model device evidence, runtime validation,
  rollback/revocation review and final release approval before accepting real
  model packs publicly. Use
  `docs/operations/model-pack-owner-evidence-template.md` as the record
  format.
- Provide owner-approved production speech adapters, speech-pack choices,
  licences, trust anchors, signing keys and device-test results before
  accepting or offering real speech packs.
- Reconfirm the exact Qwen2.5 1.5B Instruct licence and source revision under
  the publishing entity before the first model artifact is packaged, signed,
  uploaded or offered to users. Revisit Gemma 4 E2B only after Qwen device and
  storage evidence passes.
- Complete `docs/operations/model-license-approval-records.md` and review
  `docs/legal/model-third-party-notices.md` before packaging or offering any
  optional local AI model artifact.
- Keep the final Qwen `.litertlm` artifact and signing private key outside Git,
  then complete Android/iOS device-test records, runtime validation,
  rollback/revocation review and final owner release approval before setting
  any model to `APPROVED_FOR_RELEASE`.
- Review Phase 7 AI-growth fallback identifiers, prompt boundaries, model
  schema and production model-output governance before enabling a real local
  model broadly.
- Arrange qualified human review for all Phase 7.5 safety-redirection strings
  in every production locale before release.
- Arrange qualified human review for all Phase 8 safety-support, crisis,
  emergency, consent and support-sharing strings in every production locale
  before release.
- Arrange qualified human review before enabling any non-English production
  locale, especially safety, crisis, legal, privacy, consent,
  relational-boundary or daily-tool copy.
- Create the Cloudflare Pages project `bettamind-website` with root directory
  `apps/website`, build command `npm ci && npm run verify`, output directory
  `dist`, no deploy command, production branch `main` and custom domain
  `www.bettamind.com`.
- Configure DNS for `www.bettamind.com`, redirect the apex domain to the
  canonical `www` host if used, confirm HTTPS and then use the deployed
  support, privacy and data-deletion URLs in App Store and Google Play
  metadata.
- Review website legal, privacy, safety, AI transparency and support copy
  against the exact shipped app and store disclosures before public release.

## Next approved task

After the owner-approved Qwen model-pack release-candidate metadata and
trust-anchor update is pushed, run Codemagic `ios-simulator-unsigned` against
that pushed commit because shared Kotlin changed. After that, prepare the first
internal TestFlight run and record evidence in
`docs/operations/release-evidence-template.md`. The first real local AI pass is
Qwen2.5 1.5B Instruct; the artifact, app-compatible signed manifest and public
trust anchor are prepared, and the remaining blockers are platform LiteRT-LM
bridge validation, Android/iOS device evidence, low-storage/interrupted-import
testing, battery/thermal/memory observations, rollback/revocation review and
final owner release approval recorded in
`docs/operations/model-pack-owner-evidence-template.md`. Deploying the static
website through Cloudflare Pages remains a separate open owner action.
