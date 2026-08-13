# Download KaTeX vendor assets for offline math rendering
$destDir = "app\src\main\assets\katex"
if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Force -Path $destDir | Out-Null
}

$jsUrl = "https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"
$cssUrl = "https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css"

Write-Host "Downloading KaTeX JS..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $jsUrl -OutFile "$destDir\katex.min.js" -UseBasicParsing

Write-Host "Downloading KaTeX CSS..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $cssUrl -OutFile "$destDir\katex.min.css" -UseBasicParsing

Write-Host "KaTeX assets downloaded successfully!" -ForegroundColor Green
