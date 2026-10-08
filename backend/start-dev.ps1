[CmdletBinding()]
param([switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
$path = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path -LiteralPath $path)) {
    throw 'Fichier backend/.env absent : copier .env.example et renseigner la configuration locale.'
}
$allowed = @('JAVA_HOME','DB_URL','DB_USERNAME','DB_PASSWORD','JWT_SECRET','JWT_ISSUER','JWT_TTL','JWT_ALGORITHM','PASSWORD_MIN_LENGTH','BCRYPT_STRENGTH','CORS_ALLOWED_ORIGINS')
$values = @{}
$lineNumber = 0
foreach ($line in Get-Content -LiteralPath $path -Encoding UTF8) {
    $lineNumber++
    if ($line -match '^\s*(#|$)') { continue }
    if ($line -notmatch '^\s*([A-Z_]+)\s*=(.*)$') { throw "Format .env invalide, ligne $lineNumber (valeur non affichee)." }
    $name = $Matches[1]
    $value = $Matches[2].Trim()
    if ($name -notin $allowed -or $values.ContainsKey($name)) { throw "Variable inconnue ou dupliquee, ligne $lineNumber." }
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) { $value = $value.Substring(1, $value.Length - 2) }
    $values[$name] = $value
}
foreach ($name in $allowed | Where-Object { $_ -ne 'JAVA_HOME' }) {
    if ([string]::IsNullOrWhiteSpace($values[$name])) { throw "Variable $name absente ou vide dans .env (valeur non affichee)." }
}
if ($values['JAVA_HOME']) {
    $java = Join-Path $values['JAVA_HOME'] 'bin/java.exe'
    if (-not (Test-Path -LiteralPath $java)) { throw 'JAVA_HOME ne pointe pas vers un JDK disponible.' }
} else { $java = (Get-Command java -ErrorAction Stop).Source }
# Capture stderr sans NativeCommandError dans Windows PowerShell 5.1.
$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = $java
$startInfo.Arguments = '-version'
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$process = New-Object System.Diagnostics.Process
$process.StartInfo = $startInfo
try {
    [void]$process.Start()
    $version = $process.StandardOutput.ReadToEnd() + $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0 -or $version -notmatch 'version "21\.') { throw 'Java 21 est requis : renseigner JAVA_HOME dans .env.' }
} finally { $process.Dispose() }
if ($CheckOnly) { Write-Output 'Configuration locale lisible et complete ; Java 21 disponible. Aucun serveur demarre.'; return }
foreach ($name in $allowed) {
    if ($values.ContainsKey($name) -and $values[$name] -ne '') { [Environment]::SetEnvironmentVariable($name, $values[$name], 'Process') }
}
if ($values['JAVA_HOME']) { $env:Path = "$($values['JAVA_HOME'])\bin;$env:Path" }
Push-Location $PSScriptRoot
try { & .\mvnw.cmd spring-boot:run; if ($LASTEXITCODE -ne 0) { throw 'Le backend a termine en erreur ; consulter les messages precedents.' } }
finally { Pop-Location }
