# Bettamind Project Memory

## Current phase

Phase 2 implemented locally: brand, design system, navigation and localisation
foundation. Do not begin Phase 3 until the owner explicitly approves it after
Codemagic iOS validation.

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
- `androidApp/src/main/res/`
- `iosApp/iosApp/Assets.xcassets/`
- `iosApp/iosApp.xcodeproj/project.pbxproj`
- `codemagic.yaml`

## Commands that passed

- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --stacktrace`
- `.\gradlew.bat :androidApp:compileDebugKotlin --no-daemon --stacktrace`
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --stacktrace`
- `.\gradlew.bat phaseOneCheck --no-daemon --stacktrace`
- `.\gradlew.bat :androidApp:lintDebug --no-daemon --stacktrace`
- `backend\.venv\Scripts\ruff.exe check .`
- `backend\.venv\Scripts\mypy.exe app`
- `backend\.venv\Scripts\pytest.exe`
- docs placeholder/citation scan returned no matches

## Known blockers and limitations

- iOS cannot be built locally on Windows. Phase 2 iOS validation must run on
  Codemagic `ios-simulator-unsigned`.
- No canonical SVG source logo is present yet. Phase 2 assets are derived from
  the PNG fallback.
- The source PNG has a baked checkerboard background; generated assets use a
  documented mask to derive transparency.
- Locale packs are draft implementation foundations and require qualified human
  review before production use.
- `java` is not available on global `PATH`; checks used Android Studio's
  bundled JBR at `C:\Program Files\Android\Android Studio\jbr`.

## Manual owner actions

- Run Codemagic `ios-simulator-unsigned` for pushed commits that change shared
  Kotlin, Compose resources, `iosApp`, Gradle configuration that can affect
  iOS, or Codemagic iOS workflow files.
- Provide `brand/source/bettamind-logo-master.svg` if a vector master exists,
  then regenerate assets from that source in a later approved pass.
- Replace placeholder Android application ID and iOS bundle ID with owner-owned
  values before release work.
- Arrange qualified human review for production translations, especially any
  safety, crisis, legal, privacy or consent copy.

## Next approved task

Commit and push the Phase 2 cleanup, then wait for Codemagic iOS validation of
the Phase 2 shared/iOS/resource commit. Do not start Phase 3 until the owner
explicitly approves it.
