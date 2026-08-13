# Download Mermaid.js vendor bundle for offline WebView rendering
$destDir = "app\src\main\assets\mermaid"
if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
}

$url = "https://cdn.jsdelivr.net/npm/mermaid@10.9.0/dist/mermaid.min.js"
$destPath = "$destDir\mermaid.min.js"

Write-Host "Downloading Mermaid.js vendor bundle to $destPath..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $url -OutFile $destPath -UseBasicParsing
Write-Host "Mermaid.js downloaded successfully!" -ForegroundColor Green
