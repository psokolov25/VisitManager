param(
    [switch]$SkipTests
)

Write-Host "PS $($PSVersionTable.PSVersion) — генерация Javadoc в Markdown" -ForegroundColor Cyan

$mvnCmd = 'mvn'
$args = @('-s', '.mvn/settings.xml')
if ($SkipTests) { $args += '-DskipTests' }
$args += 'javadoc:javadoc'

& $mvnCmd @args
if ($LASTEXITCODE -ne 0) {
    throw "Сборка Javadoc завершилась с ошибкой (код $LASTEXITCODE)."
}

$apidocs = Join-Path $PSScriptRoot '..\target\reports\apidocs\index.html'
$out = Join-Path $PSScriptRoot '..\docs\JAVADOC.md'
if (-not (Test-Path $apidocs)) {
    throw "Файл не найден: $apidocs"
}
if (-not (Get-Command pandoc -ErrorAction SilentlyContinue)) {
    throw 'Pandoc не найден. Установите pandoc и повторите.'
}

pandoc $apidocs -f html -t gfm -o $out
Write-Host "Готово: $out" -ForegroundColor Green
