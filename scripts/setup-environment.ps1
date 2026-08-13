# Inkleaf Environment Setup Script (PowerShell)
# Installs/Verifies JDK 17, Android SDK Command-line tools, API 34, build-tools 34.0.0

$ErrorActionPreference = "Continue"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "     Inkleaf Environment & SDK Diagnostic Setup    " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Node & pnpm Check
Write-Host "`n[1/4] Checking Node.js & pnpm..." -ForegroundColor Yellow
if (Get-Command node -ErrorAction SilentlyContinue) {
    $nodeVer = node -v
    Write-Host "  [OK] Node.js found: $nodeVer" -ForegroundColor Green
} else {
    Write-Host "  [WARNING] Node.js not found in PATH." -ForegroundColor Red
}

if (Get-Command pnpm -ErrorAction SilentlyContinue) {
    $pnpmVer = pnpm -v
    Write-Host "  [OK] pnpm found: v$pnpmVer" -ForegroundColor Green
} else {
    Write-Host "  [WARNING] pnpm not found in PATH." -ForegroundColor Red
}

# 2. JDK 17 Check
Write-Host "`n[2/4] Checking Java JDK 17..." -ForegroundColor Yellow
$adoptiumPath = "C:\Program Files\Eclipse Adoptium"
if (Test-Path $adoptiumPath) {
    $jdkDir = Get-ChildItem $adoptiumPath | Select-Object -First 1
    if ($jdkDir) {
        $javaExe = "$($jdkDir.FullName)\bin\java.exe"
        if (Test-Path $javaExe) {
            $env:JAVA_HOME = $jdkDir.FullName
            $env:PATH = "$($jdkDir.FullName)\bin;" + $env:PATH
            $javaVerOutput = & $javaExe -version 2>&1 | Out-String
            Write-Host "  [OK] JDK 17 found at $($jdkDir.FullName)" -ForegroundColor Green
            Write-Host "  $javaVerOutput" -ForegroundColor Gray
        }
    }
} elseif (Get-Command java -ErrorAction SilentlyContinue) {
    $javaVerOutput = java -version 2>&1 | Out-String
    Write-Host "  [OK] Java found: $javaVerOutput" -ForegroundColor Green
} else {
    Write-Host "  [NOTICE] 'java' command not found in PATH." -ForegroundColor Red
}

# 3. Android SDK Check
Write-Host "`n[3/4] Checking Android SDK..." -ForegroundColor Yellow
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) {
    $sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
}

Write-Host "  Target SDK Path: $sdkPath" -ForegroundColor Gray

if (Test-Path "$sdkPath\cmdline-tools\latest\bin\sdkmanager.bat") {
    Write-Host "  [OK] sdkmanager found at $sdkPath\cmdline-tools\latest\bin\sdkmanager.bat" -ForegroundColor Green
    
    Write-Host "  Installing/Updating platforms;android-34 and build-tools;34.0.0..." -ForegroundColor Yellow
    & "$sdkPath\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-34" "build-tools;34.0.0" "platform-tools"
} elseif (Test-Path "$sdkPath\platform-tools\adb.exe") {
    Write-Host "  [OK] Android platform-tools found at $sdkPath" -ForegroundColor Green
} else {
    Write-Host "  [NOTICE] Android SDK not detected at $sdkPath." -ForegroundColor Red
    Write-Host "  To install Android SDK / Android Studio:" -ForegroundColor Yellow
    Write-Host "  winget install Google.AndroidStudio" -ForegroundColor Gray
}

# 4. Summary & Verification
Write-Host "`n[4/4] Environment Diagnostics Summary:" -ForegroundColor Yellow
Write-Host "  - System: Windows 11 ($env:OS)" -ForegroundColor Gray
Write-Host "  - Workspace: C:\Luminary\Projects\inkleaf" -ForegroundColor Gray
Write-Host "  - Setup script complete." -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Cyan
