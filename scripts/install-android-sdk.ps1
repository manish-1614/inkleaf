# Automated Android SDK Command-line tools installer
$sdkDir = "$env:LOCALAPPDATA\Android\Sdk"
$cmdlineToolsDir = "$sdkDir\cmdline-tools\latest"

Write-Host "Setting up Android SDK at: $sdkDir" -ForegroundColor Cyan

# Configure JDK 17 paths for subshell execution
$adoptiumDir = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
if (Test-Path $adoptiumDir) {
    $env:JAVA_HOME = $adoptiumDir
    $env:PATH = "$adoptiumDir\bin;" + $env:PATH
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $adoptiumDir, 'User')
    Write-Host "JAVA_HOME configured: $env:JAVA_HOME" -ForegroundColor Green
}

if (-not (Test-Path "$sdkDir\cmdline-tools")) {
    New-Item -ItemType Directory -Force -Path "$sdkDir\cmdline-tools" | Out-Null
}

$zipUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$zipPath = "$env:TEMP\commandlinetools-win.zip"

if (-not (Test-Path "$cmdlineToolsDir\bin\sdkmanager.bat")) {
    Write-Host "Downloading Android Command-line tools from Google..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath -UseBasicParsing

    Write-Host "Extracting Command-line tools..." -ForegroundColor Yellow
    $tempExtract = "$env:TEMP\cmdline-temp"
    if (Test-Path $tempExtract) { Remove-Item -Recurse -Force $tempExtract }
    Expand-Archive -Path $zipPath -DestinationPath $tempExtract -Force

    if (-not (Test-Path $cmdlineToolsDir)) {
        New-Item -ItemType Directory -Force -Path $cmdlineToolsDir | Out-Null
    }

    Copy-Item -Path "$tempExtract\cmdline-tools\*" -Destination $cmdlineToolsDir -Recurse -Force
    Remove-Item -Force $zipPath -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force $tempExtract -ErrorAction SilentlyContinue
    Write-Host "Android Command-line tools extracted to $cmdlineToolsDir" -ForegroundColor Green
} else {
    Write-Host "Command-line tools already extracted at $cmdlineToolsDir" -ForegroundColor Green
}

# Set ANDROID_HOME environment variable
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $sdkDir, 'User')
$env:ANDROID_HOME = $sdkDir

# Accept SDK licenses & install packages
$sdkManager = "$cmdlineToolsDir\bin\sdkmanager.bat"
if (Test-Path $sdkManager) {
    Write-Host "Accepting SDK licenses and installing API 34..." -ForegroundColor Yellow
    $env:JAVA_HOME = $adoptiumDir
    cmd.exe /c "set JAVA_HOME=$adoptiumDir&& set PATH=$adoptiumDir\bin;%PATH%&& (for %i in (1 2 3 4 5 6 7 8 9 10) do @echo y) | `"$sdkManager`" --licenses --sdk_root=`"$sdkDir`""
    cmd.exe /c "set JAVA_HOME=$adoptiumDir&& set PATH=$adoptiumDir\bin;%PATH%&& `"$sdkManager`" `"platforms;android-34`" `"build-tools;34.0.0`" `"platform-tools`" --sdk_root=`"$sdkDir`""
    Write-Host "Android SDK API 34 & Build Tools installed successfully!" -ForegroundColor Green
} else {
    Write-Host "Error: sdkmanager.bat not found at $sdkManager" -ForegroundColor Red
}
