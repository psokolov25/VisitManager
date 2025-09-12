param(
  [ValidateSet('none','missing','all')]
  [string]$Mode = 'missing',
  [switch]$SkipTests,
  [switch]$OpenLog
)

Write-Host "Проверка Javadoc (doclint=$Mode)" -ForegroundColor Cyan

$mvn = 'mvn'
$args = @('-s', '.mvn/settings.xml', '-B')
if ($SkipTests) { $args += '-DskipTests' } else { $args += '-DskipTests' }
$args += "-Ddoclint=$Mode"
$args += 'javadoc:javadoc'

$logDir = Join-Path $PSScriptRoot '..\target'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir ("javadoc-" + $Mode + ".log")

& $mvn @args 2>&1 | Tee-Object -FilePath $logFile

if ($LASTEXITCODE -ne 0) {
  Write-Warning "Сборка Javadoc завершилась с ошибкой (код $LASTEXITCODE). См. $logFile"
} else {
  Write-Host "Javadoc успешно сгенерирован. Лог: $logFile" -ForegroundColor Green
}

# Краткая сводка по предупреждениям/ошибкам
$content = Get-Content $logFile
$warnings = ($content | Select-String -Pattern '\[WARNING\]|\bjavadoc\b|warning').Count
$errors = ($content | Select-String -Pattern '\[ERROR\]|\berror\b').Count
Write-Host ("Summary: warnings={0} errors={1}" -f $warnings, $errors)

if ($OpenLog) { Start-Process $logFile }

