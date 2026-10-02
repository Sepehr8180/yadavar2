$ErrorActionPreference = 'Stop'

# Locate keytool from PATH or JAVA_HOME.
$keytoolPath = $null
$keytoolCmd = Get-Command keytool -ErrorAction SilentlyContinue
if ($keytoolCmd) {
    $keytoolPath = $keytoolCmd.Source
}

if (-not $keytoolPath -and $env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
    if (Test-Path $candidate) {
        $keytoolPath = $candidate
    }
}

if (-not $keytoolPath) {
    throw 'keytool was not found. Install a JDK and make sure JAVA_HOME or PATH is configured.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$keystore = Join-Path $projectRoot 'yadavar-release.jks'
$base64File = Join-Path $projectRoot 'yadavar-keystore-base64.txt'

if (Test-Path $keystore) {
    throw "The file '$keystore' already exists. Do not delete it or generate a new key unless you intentionally want a different signing identity."
}

Write-Host 'The next step will ask for a Keystore password and a Key password.'
Write-Host 'Keep both passwords in a safe place. They are required for future app updates.'
Write-Host ''

& $keytoolPath -genkeypair -v `
    -keystore $keystore `
    -alias yadavar `
    -keyalg RSA `
    -keysize 4096 `
    -validity 10000

if (-not (Test-Path $keystore)) {
    throw 'The keystore was not created.'
}

$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore))
Set-Content -Path $base64File -Value $base64 -NoNewline -Encoding ascii

Write-Host ''
Write-Host "Created: $keystore"
Write-Host "Created: $base64File"
Write-Host ''
Write-Host 'Create these four GitHub repository secrets:'
Write-Host '  YADAVAR_KEYSTORE_BASE64 = contents of yadavar-keystore-base64.txt'
Write-Host '  YADAVAR_STORE_PASSWORD  = Keystore password'
Write-Host '  YADAVAR_KEY_ALIAS       = yadavar'
Write-Host '  YADAVAR_KEY_PASSWORD    = Key password'
Write-Host ''
Write-Host 'IMPORTANT: Never commit yadavar-release.jks to GitHub.'
