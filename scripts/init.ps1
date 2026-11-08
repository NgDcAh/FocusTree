param()

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location $projectRoot
try {
    Write-Host "Focus Tree harness initialization"
    Write-Host "Project root: $projectRoot"

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Warning "Java was not found on PATH. Install the JDK required by the pinned Android Gradle Plugin before implementing FT-FND-001."
    } else {
        java -version
    }

    $wrapper = Join-Path $projectRoot "gradlew.bat"
    if (-not (Test-Path -LiteralPath $wrapper)) {
        Write-Warning "Gradle wrapper is not present yet. This is expected until FT-FND-001 is implemented."
    } else {
        & $wrapper --version
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle wrapper check failed with exit code $LASTEXITCODE."
        }
    }

    if (Get-Command git -ErrorAction SilentlyContinue) {
        Write-Host "Git status:"
        git status --short
    }

    Write-Host "Read AGENTS.md, docs/PRODUCT.md, docs/ARCHITECTURE.md, feature_list.json, progress.md, and session-handoff.md before editing."
} finally {
    Pop-Location
}
