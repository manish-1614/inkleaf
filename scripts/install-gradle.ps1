# PowerShell Script to download and set up Gradle 8.4
$gradleDir = "C:\gradle-8.4"

if (Test-Path "$gradleDir\bin\gradle.bat") {
    Write-Host "Gradle already installed at $gradleDir" -ForegroundColor Green
    exit 0
}

$zipUrl = "https://services.gradle.org/distributions/gradle-8.4-bin.zip"
$zipPath = "$env:TEMP\gradle.zip"

Write-Host "Downloading Gradle 8.4 from services.gradle.org..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath -UseBasicParsing

Write-Host "Extracting Gradle zip to C:\..." -ForegroundColor Yellow
Expand-Archive -Path $zipPath -DestinationPath "C:\" -Force
Remove-Item -Force $zipPath

Write-Host "Gradle 8.4 installed successfully at $gradleDir" -ForegroundColor Green
