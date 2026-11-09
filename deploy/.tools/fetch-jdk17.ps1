# Project-local JDK 17 installer.
# Installs into <project>\deploy\.tools\jdk-17 -- it does NOT touch system
# environment variables, PATH or the registry.
#
# Usage (run from the project root; Windows PowerShell 5.1 is enough):
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\.tools\fetch-jdk17.ps1
$ErrorActionPreference = 'Stop'

# This script lives in <project>\deploy\.tools -- go up two levels for the root.
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$toolsDir    = Join-Path $projectRoot 'deploy\.tools'
$jdkHome     = Join-Path $toolsDir 'jdk-17'

$fileName = 'OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip'
$baseUrl  = 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/windows/'
$url      = $baseUrl + $fileName
$sha256   = 'e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0'
$size     = 190817615

New-Item -ItemType Directory -Path $toolsDir -Force | Out-Null
$zip = Join-Path $toolsDir $fileName

if (Test-Path $jdkHome) {
    Write-Host "[SKIP] $jdkHome already exists" -ForegroundColor Yellow
    exit 0
}

# 1) Download (about 182 MB)
if ((Test-Path $zip) -and (Get-Item $zip).Length -eq $size) {
    Write-Host "[1/3] Archive already present and size is correct, skip download" -ForegroundColor Yellow
} else {
    if (Test-Path $zip) { Remove-Item $zip -Force }
    Write-Host "[1/3] Downloading JDK 17 (~182 MB) ..." -ForegroundColor Cyan
    $sw = [Diagnostics.Stopwatch]::StartNew()
    try {
        Start-BitsTransfer -Source $url -Destination $zip -DisplayName 'JDK 17' -ErrorAction Stop
    } catch {
        Write-Host "      BITS failed ($($_.Exception.Message)), falling back to curl" -ForegroundColor Yellow
        & curl.exe -sS -L -A 'Mozilla/5.0' --retry 3 --retry-delay 2 --max-time 1800 -o $zip $url
        if ($LASTEXITCODE -ne 0) { throw "curl failed with exit code $LASTEXITCODE" }
    }
    $sw.Stop()
    $got = (Get-Item $zip).Length
    Write-Host ("      done: {0:N1} MB in {1:N1}s" -f ($got / 1MB), $sw.Elapsed.TotalSeconds)
    if ($got -ne $size) { throw "Size mismatch: expected $size bytes, got $got bytes" }
}

# 2) Verify SHA256
Write-Host "[2/3] Verifying SHA256 ..." -ForegroundColor Cyan
$actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLower()
if ($actual -ne $sha256) { throw "SHA256 mismatch.`n expected $sha256`n actual   $actual" }
Write-Host "      OK $actual"

# 3) Extract and normalize the directory name
Write-Host "[3/3] Extracting ..." -ForegroundColor Cyan
$tmp = Join-Path $toolsDir '_tmp_extract'
if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
Expand-Archive -LiteralPath $zip -DestinationPath $tmp -Force

$extracted = Get-ChildItem $tmp -Directory | Where-Object { $_.Name -like 'jdk-17*' } | Select-Object -First 1
if (-not $extracted) { throw "No jdk-17* directory found in the archive" }
Move-Item -Path $extracted.FullName -Destination $jdkHome
Remove-Item $tmp -Recurse -Force

Write-Host ""
Write-Host "JDK 17 installed at: $jdkHome" -ForegroundColor Green
& (Join-Path $jdkHome 'bin\java.exe') -version
Write-Host ""
Write-Host "Activate for the current terminal only:  . .\deploy\.tools\dev-env.ps1" -ForegroundColor Green
