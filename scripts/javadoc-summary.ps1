$ErrorActionPreference = 'Stop'

param(
  [string]$LogPath = 'javadoc_strict.log'
)

if (-not (Test-Path $LogPath)) {
  Write-Host "Log file not found: $LogPath" -ForegroundColor Yellow
  exit 1
}

Write-Host "Summarizing warnings in $LogPath" -ForegroundColor Cyan

$warnings = Select-String -Path $LogPath -Pattern ": warning:" -SimpleMatch
if (-not $warnings) {
  Write-Host "No warnings found." -ForegroundColor Green
  exit 0
}

$byFile = $warnings |
  ForEach-Object {
    $prefix = ($_ -split ": warning:" )[0]
    ($prefix -split ":\d+\:")[0]
  } |
  Group-Object |
  Sort-Object Count -Descending

Write-Host "Top files by warnings:" -ForegroundColor Cyan
$byFile | Select-Object -First 30 | ForEach-Object {
  "{0,4}  {1}" -f $_.Count, $_.Name
}

Write-Host "\nExample entries (first 100):" -ForegroundColor Cyan
$warnings | Select-Object -First 100 | ForEach-Object { $_.Line }

exit 0

