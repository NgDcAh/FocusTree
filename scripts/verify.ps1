param(
    [ValidateSet("fast", "full")]
    [string]$Scope = "full"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$wrapper = Join-Path $projectRoot "gradlew.bat"

if (-not (Test-Path -LiteralPath $wrapper)) {
    throw "Gradle wrapper is missing. Implement FT-FND-001 before product features."
}

$tasks = @("ktlintCheck", "detekt", "testDebugUnitTest")
if ($Scope -eq "full") {
    $tasks += @("lintDebug", "assembleDebug", ":server:test", ":server:postgresIntegrationTest")
}

Push-Location $projectRoot
try {
    Write-Host "Running Focus Tree $Scope verification: $($tasks -join ', ')"
    & $wrapper @tasks --stacktrace
    if ($LASTEXITCODE -ne 0) {
        throw "Verification failed with exit code $LASTEXITCODE."
    }
    Write-Host "Verification passed. Record this command and result as feature evidence."
} finally {
    Pop-Location
}
