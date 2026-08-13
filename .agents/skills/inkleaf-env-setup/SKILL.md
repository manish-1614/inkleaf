---
name: inkleaf-env-setup
description: Automated setup of JDK 17, Android SDK Command-line tools, platforms API 34, build-tools 34.0.0, and environment variables for Inkleaf Android development.
---

# Inkleaf Environment & SDK Setup Skill

This skill guides the automated installation and environment verification for building Inkleaf on Windows.

## 1. Automated Script

Run the automated setup script in PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-environment.ps1
```

Or via CMD:

```cmd
scripts\setup-environment.cmd
```

## 2. Environment Verification Matrix

- **JDK Version**: OpenJDK 17 or JDK 17 (Java 17 required for Jetpack Compose & Android Gradle Plugin 8.2+).
- **ANDROID_HOME**: Set to `%LOCALAPPDATA%\Android\Sdk` (or custom path).
- **Android SDK Components**:
  - `cmdline-tools;latest`
  - `platforms;android-34`
  - `build-tools;34.0.0`
  - `platform-tools`
- **Node & pnpm**: Verified for tooling and asset bundlers (`node -v`, `pnpm -v`).

## 3. Manual Fallback

If automated download fails due to network proxies:
1. Download Command-line tools zip from [developer.android.com/studio#command-line-tools-only](https://developer.android.com/studio#command-line-tools-only).
2. Unpack into `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`.
3. Accept licenses via `sdkmanager.bat --licenses`.
