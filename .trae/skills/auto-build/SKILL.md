---
name: "auto-build"
description: "Automatically compiles the Android project and copies the APK to the project root after any file modification. Invoke when user asks to auto-build after changes or when any code edit is made."
---

# Auto Build Skill

## Purpose

Every time a file is modified in the project, automatically run the build and place the APK in the project root directory.

## Trigger Conditions

- User explicitly asks: "auto build", "compile after changes", "build after modification"
- Any file edit operation completes (SearchReplace, Write, DeleteFile)
- User says: "每次修改后自动编译" or similar requests

## Build Steps

1. Run Gradle build:
   ```
   .\gradlew.bat assembleDebug
   ```

2. If build succeeds, copy APK to project root:
   ```
   copy /Y app\build\outputs\apk\debug\app-debug.apk .\app-debug.apk
   ```

3. Report build result to user with:
   - Build status (success/failure)
   - APK location
   - Any errors if build failed

## Notes

- Always use `assembleDebug` for faster builds
- Do NOT run `clean` to preserve incremental compilation speed
- Copy APK to project root so user can easily find it
- If build fails, show the error details and stop
