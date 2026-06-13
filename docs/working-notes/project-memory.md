# Bettamind Project Memory

## Current phase

iOS SQLCipher completion slice is being validated after the owner confirmed
Codemagic passed for the Phase 4 commit and approved continuing the Phase 3
storage work. Phase 4 remains intentionally in-memory for personal narrative
content until encrypted storage is fully validated. Phase 3 now has a selected
iOS SQLCipher route and adapter source, but completion still requires
Codemagic `ios-simulator-unsigned` validation on macOS after the latest
Kotlin/Native cinterop pointer-helper fix.

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
- Phase 4 deterministic growth flow is allowed to run without storage, but
  narrative persistence remains disabled unless encrypted storage is available.
- Phase 4 adult gate is self-declared and records no exact date of birth or
  identity document.

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

## Important files

- `AGENTS.md`
- `docs/specification/bettamind-locked-specification.md`
- `docs/planning/implementation-plan.md`
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
- `shared/src/commonTest/kotlin/org/bettamind/shared/privacy/`
- `shared/src/commonTest/kotlin/org/bettamind/shared/growth/`
- `shared/src/androidMain/kotlin/org/bettamind/shared/privacy/`
- `shared/src/iosMain/kotlin/org/bettamind/shared/privacy/`
- `shared/src/iosTest/kotlin/org/bettamind/shared/privacy/`
- `shared/src/nativeInterop/cinterop/`
- `androidApp/src/main/res/`
- `iosApp/iosApp/Assets.xcassets/`
- `iosApp/iosApp.xcodeproj/project.pbxproj`
- `docs/security/phase-3-encrypted-storage-spike.md`
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
- `backend\.venv\Scripts\ruff.exe check .`
- `backend\.venv\Scripts\mypy.exe app`
- `backend\.venv\Scripts\pytest.exe`
- From `backend/`: `.\.venv\Scripts\ruff.exe check .`
- From `backend/`: `.\.venv\Scripts\mypy.exe app`
- From `backend/`: `.\.venv\Scripts\pytest.exe`
- docs/source placeholder scan found only Codemagic's intentional `events: []`

## Known blockers and limitations

- iOS cannot be fully built locally on Windows. Every shared/iOS change still
  requires Codemagic `ios-simulator-unsigned`.
- Phase 3 remains pending Codemagic validation because iOS SQLCipher cinterop
  cannot be compiled or executed on Windows. Kotlin/Native disables the iOS
  targets on `mingw_x64` once cinterop is present.
- Phase 4 does not persist narrative content. Storage status intentionally
  reports encrypted storage unavailable until the platform encrypted store is
  complete.
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
- Run Codemagic `ios-simulator-unsigned` for the next pushed iOS SQLCipher
  commit because it changes shared iOS Kotlin and must be proven by macOS
  Kotlin/Native compilation plus the Xcode simulator build.
- Provide `brand/source/bettamind-logo-master.svg` if a vector master exists,
  then regenerate assets from that source in a later approved pass.
- Replace placeholder Android application ID and iOS bundle ID with owner-owned
  values before release work.
- Arrange qualified human review for production translations, especially any
  safety, crisis, legal, privacy or consent copy.

## Next approved task

Commit and push the Kotlin/Native `allocPointerTo` pointer-helper fix, then
have the owner rerun Codemagic `ios-simulator-unsigned`. If Codemagic passes,
update memory to mark Phase 3 encrypted-storage proof complete. If it fails,
fix the next macOS native compile/link/test issue before continuing. Do not
begin Phase 5 automatically.
