# Bettamind

Bettamind is a private, mostly offline Human Growth Operating System for
Android and iOS. The application is built with Kotlin Multiplatform and Compose
Multiplatform, with an optional FastAPI backend that is not required for core
mobile use.

## Current phase

Phase 1 is the cross-platform build foundation:

- Kotlin Multiplatform shared module;
- Compose Multiplatform shared UI foundation;
- Android application target;
- iOS simulator and device targets;
- valid minimal `iosApp` Xcode project;
- optional FastAPI backend skeleton;
- GitHub Actions and Codemagic validation.

Do not implement Phase 2 brand assets, finished navigation, encrypted storage,
AI, or product engines until Phase 1 passes.

## Local prerequisites

- Android Studio with its bundled JDK.
- Android SDK platform installed for the configured compile SDK.
- Python 3.12 or newer for the optional backend.
- macOS/Xcode is not required locally; iOS validation runs on Codemagic.

If `java` is not on `PATH` on Windows, point `JAVA_HOME` to Android Studio's
bundled JBR before running Gradle:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Checks

```powershell
.\gradlew.bat phaseOneCheck
python -m compileall backend
```

Run the Codemagic `ios-simulator-unsigned` workflow for the required Xcode
simulator validation.
