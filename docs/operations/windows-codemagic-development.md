# Windows and Codemagic Development

## Windows

Windows is the primary development environment. Android builds and shared tests
run locally when Java, Android SDK and Gradle dependencies are available.

If `java` is not on `PATH`, use Android Studio's bundled JBR:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

The local Android SDK used for Phase 1 checks is:

```powershell
$env:ANDROID_HOME = "C:\Users\HP\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"
```

Recommended Phase 1 checks:

```powershell
.\gradlew.bat --version
.\gradlew.bat phaseOneCheck
python -m compileall backend
```

Optional backend local setup and checks:

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -e ".[dev]"
.\.venv\Scripts\ruff.exe check .
.\.venv\Scripts\mypy.exe app
.\.venv\Scripts\pytest.exe
```

## Codemagic

The `ios-simulator-unsigned` workflow is the required Phase 1 iOS validation.
It must:

- run on macOS;
- use explicit Xcode selection;
- validate the Gradle wrapper;
- run shared tests;
- compile Kotlin iOS targets;
- run `xcodebuild` against `iosApp/iosApp.xcodeproj`;
- disable code signing for the simulator build;
- collect logs and reports.

Do not add Apple signing credentials during Phase 1.
