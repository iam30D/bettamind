# Bettamind Project Memory

## Current phase

Phase 1 completed locally: monorepo and cross-platform build foundation.

## Locked decisions

- Mobile stack: Kotlin Multiplatform and Compose Multiplatform.
- Primary development environment: Windows.
- iOS validation: Codemagic macOS using the real `iosApp` Xcode project.
- Optional backend: FastAPI.
- Core mobile use remains offline, account-free, backend-independent and useful
  without AI.
- No Phase 2 branding assets, finished UI, encryption, AI models or product
  engines in Phase 1.

## Completed work

- Empty GitHub repository cloned into the workspace.
- Definitive setup prompt and duplicate Phase 0 prompt attachments were read.
- Baseline repository documentation was created because the cloned repository
  was empty.
- Phase 0 planning files were reconstructed from the definitive owner prompt.
- Kotlin Multiplatform and Compose Multiplatform foundation created.
- Android application target created with display name Bettamind.
- iOS targets and minimal `iosApp` Xcode project created.
- Optional FastAPI backend skeleton created.
- GitHub Actions and `codemagic.yaml` created.
- Android command-line tools and SDK packages installed locally under
  `C:\Users\HP\AppData\Local\Android\Sdk`.
- Backend virtual environment created under `backend/.venv` and dev
  dependencies installed there.
- Backend Ruff, mypy and pytest checks passed after fixing Alembic import
  ordering and SQLAlchemy session factory typing.
- GitHub Actions runner failure `./gradlew: Permission denied` was repaired by
  marking `gradlew` executable in Git and adding a defensive CI `chmod +x`
  step.
- Codemagic failure in step `Run shared tests` was repaired by replacing the
  broad `:shared:allTests` aggregate with the explicit iOS simulator test task
  `:shared:iosSimulatorArm64Test`. Device target compatibility remains covered
  by explicit iOS target compilation and the real `xcodebuild` simulator build.
- Codemagic then exposed a Kotlin/Native compile failure in
  `LocaleTag.kt`: `@JvmInline` was used in `commonMain`. Replaced the
  JVM-specific value class with a serializable common `data class`.
- A PNG source logo is present at `brand/source/bettamind-logo-master.png`; no
  generated brand assets were created.

## Important files

- `AGENTS.md`
- `docs/specification/bettamind-locked-specification.md`
- `docs/planning/implementation-plan.md`
- `docs/working-notes/project-memory.md`
- `codemagic.yaml`
- `androidApp/build.gradle.kts`
- `shared/build.gradle.kts`
- `iosApp/iosApp.xcodeproj/project.pbxproj`
- `backend/app/main.py`

## Commands that passed

- `git clone https://github.com/iam30D/bettamind.git .`
- `python --version`
- `.\gradlew.bat --version`
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --stacktrace`
- `.\gradlew.bat :shared:testDebugUnitTest --no-daemon --stacktrace`
- `.\gradlew.bat :androidApp:compileDebugKotlin --no-daemon --stacktrace`
- `.\gradlew.bat :androidApp:assembleDebug --no-daemon --stacktrace`
- `.\gradlew.bat phaseOneCheck --no-daemon --stacktrace`
- `.\gradlew.bat :shared:compileKotlinMetadata --no-daemon --stacktrace` after
  the `LocaleTag` Kotlin/Native repair
- `python -m compileall backend`
- `backend\.venv\Scripts\ruff.exe check .`
- `backend\.venv\Scripts\mypy.exe app`
- `backend\.venv\Scripts\pytest.exe`
- `git update-index --chmod=+x gradlew`
- docs unresolved citation placeholder scan

## Known blockers and limitations

- The cloned GitHub repository was empty, so Phase 0 planning files had to be
  reconstructed from the definitive owner prompt.
- No canonical SVG source logo is present yet under `brand/source/`; Phase 2 can
  use the present PNG fallback if the owner approves it as canonical.
- `java` is not available on global `PATH`; checks used Android Studio's bundled
  JBR at `C:\Program Files\Android\Android Studio\jbr`.
- iOS cannot be built locally on Windows and must be validated on Codemagic.
- `:shared:allTests` is not a Windows check because it may include native/iOS
  test work and should not be used in Codemagic Phase 1; Windows uses
  `phaseOneCheck`, while Codemagic runs `:shared:iosSimulatorArm64Test`, iOS
  target compilation and `xcodebuild`.
- Backend pytest passes with one upstream Starlette/FastAPI deprecation warning
  about `httpx` test client compatibility; it does not fail the Phase 1 check.

## Manual owner actions

- Confirm whether `brand/source/bettamind-logo-master.png` is the canonical
  fallback logo for Phase 2 or provide `brand/source/bettamind-logo-master.svg`.
- Replace placeholder Android application ID and iOS bundle ID with owner-owned
  values before release work.
- Connect the repository to Codemagic and run the unsigned iOS simulator
  workflow after Phase 1 is pushed.

## Next approved task

Rerun Codemagic `ios-simulator-unsigned` and repair any remaining Xcode build
failure before approving Phase 2.
