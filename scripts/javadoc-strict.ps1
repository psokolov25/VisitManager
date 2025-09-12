$ErrorActionPreference = 'Stop'

# Run strict Javadoc and save full log
Write-Host "Running strict Javadoc..."
# Строгий режим: падаем на предупреждениях Javadoc
mvn -s .mvn/settings.xml -DskipTests -Ddoclint=all -Dmaven.javadoc.failOnWarnings=true javadoc:javadoc *>& javadoc_strict.log
$exit = $LASTEXITCODE
Write-Host "Javadoc exit code:" $exit

# Print first 200 warnings
if (Test-Path javadoc_strict.log) {
  Write-Host "\nFirst warnings (up to 200):" -ForegroundColor Cyan
  Select-String -Path javadoc_strict.log -Pattern ": warning:" -SimpleMatch |
    Select-Object -First 200 |
    ForEach-Object { $_.Line }

  Write-Host "\nFiles with highest warning counts:" -ForegroundColor Cyan
  $grouped = Select-String -Path javadoc_strict.log -Pattern ": warning:" -SimpleMatch |
    ForEach-Object { ($_ -split ": warning:" )[0] } |
    ForEach-Object { ($_ -split ":\d+\:")[0] } |
    Group-Object |
    Sort-Object Count -Descending |
    Select-Object -First 20
  foreach($g in $grouped){ "{0,4}  {1}" -f $g.Count, $g.Name }
}

exit $exit
