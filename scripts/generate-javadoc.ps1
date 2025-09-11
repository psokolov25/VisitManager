param(
  [switch]$Open,
  [switch]$SkipTests
)

Write-Host "PS $($PSVersionTable.PSVersion) — генерация Javadoc" -ForegroundColor Cyan

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  Write-Warning "Java не найдена в PATH. Убедитесь, что установлен JDK 17 и задан JAVA_HOME. Пример: setx JAVA_HOME 'C:\\Program Files\\Java\\jdk-17'"
}

$mvnCmd = if (Test-Path "$PSScriptRoot/../mvnw.cmd") {
  "$PSScriptRoot/../mvnw.cmd"
} elseif (Test-Path "$PSScriptRoot/../mvnw.bat") {
  "$PSScriptRoot/../mvnw.bat"
} else {
  "mvn"
}
$args = @()
if ($SkipTests) { $args += '-DskipTests' }
$args += 'javadoc:javadoc'

& $mvnCmd @args
if ($LASTEXITCODE -ne 0) {
  throw "Сборка Javadoc завершилась с ошибкой (код $LASTEXITCODE)."
}

$apidocs = Join-Path $PSScriptRoot '..\target\site\apidocs\index.html' | Resolve-Path -ErrorAction SilentlyContinue
if (-not $apidocs) {
  Write-Warning "Файл apidocs не найден. Ожидали: target\\site\\apidocs\\index.html"
} else {
  Write-Host "Готово: $apidocs" -ForegroundColor Green
  if ($Open) { Start-Process $apidocs }
}
