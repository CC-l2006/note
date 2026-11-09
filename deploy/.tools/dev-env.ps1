# Point JAVA_HOME / PATH at the project-local JDK 17.
# Affects the CURRENT terminal session only: no system environment variables,
# no registry changes. Close the terminal and everything is back to normal.
#
# Usage (from the project root):
#   . .\deploy\.tools\dev-env.ps1
# Then:
#   cd server
#   .\mvnw.cmd clean package
#
# If script execution is blocked, either run once:
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
# or activate without dot-sourcing:
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\.tools\dev-env.ps1

# This script lives in <project>\deploy\.tools -- go up two levels for the root.
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$jdkHome = Join-Path $projectRoot 'deploy\.tools\jdk-17'
$javaExe = Join-Path $jdkHome 'bin\java.exe'

if (-not (Test-Path $javaExe)) {
    Write-Host "Project-local JDK not found: $javaExe" -ForegroundColor Red
    Write-Host "Run first: powershell -NoProfile -ExecutionPolicy Bypass -File .\deploy\.tools\fetch-jdk17.ps1" -ForegroundColor Yellow
    return
}

$env:JAVA_HOME = $jdkHome
if ($env:PATH -notlike "*$jdkHome\bin*") {
    $env:PATH = (Join-Path $jdkHome 'bin') + ';' + $env:PATH
}

Write-Host "Project-local JDK 17 activated (current terminal only)" -ForegroundColor Green
Write-Host "  JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Green
java -version
