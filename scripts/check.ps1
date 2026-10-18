param([switch]$Integration)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
function Assert-NativeResult([string]$step) {
    if ($LASTEXITCODE -ne 0) { throw "$step failed with exit code $LASTEXITCODE" }
}
Push-Location $repoRoot
try {
    node scripts/check-contracts.mjs
    Assert-NativeResult 'Contract checks'
    Push-Location backend
    try {
        $maven = if ($IsWindows) { '.\mvnw.cmd' } else { './mvnw' }
        if ($Integration) {
            foreach ($key in 'TEST_DATABASE_URL', 'TEST_DATABASE_USERNAME', 'TEST_DATABASE_PASSWORD') {
                if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key))) {
                    throw "Integration checks require $key and a dedicated PostgreSQL database."
                }
            }
            & $maven -B -ntp -Pintegration verify
        } else {
            & $maven -B -ntp test
        }
        Assert-NativeResult 'Backend checks'
    } finally { Pop-Location }
    Push-Location frontend
    try {
        npm ci
        Assert-NativeResult 'Frontend dependency installation'
        npm test
        Assert-NativeResult 'Frontend tests'
        npm run build
        Assert-NativeResult 'Frontend build'
    } finally { Pop-Location }
    Write-Output 'All requested checks passed.'
} finally { Pop-Location }
