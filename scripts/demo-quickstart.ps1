# Quickstart сценарий (PowerShell): открытие СП → создание визита → вызов (confirm) → итог → завершение → закрытие СП
# Требования: запущен VisitManager на http://localhost:8080 с профилем local-no-docker

$BASE = $env:BASE; if (-not $BASE) { $BASE = 'http://localhost:8080' }
$BR = $env:BR; if (-not $BR) { $BR = '37493d1c-8282-4417-a729-dceac1f3e2b4' }
$SP = $env:SP; if (-not $SP) { $SP = 'a66ff6f4-4f4a-4009-8602-0dc278024cf2' }
$WP = $env:WP; if (-not $WP) { $WP = 'd5a84e60-e605-4527-b065-f4bd7a385790' }
$ENTRY = $env:ENTRY; if (-not $ENTRY) { $ENTRY = '2' }
$USER = $env:USER; if (-not $USER) { $USER = 'psokolov' }

# Демо: услуги/итоги
$SVC = $env:SVC; if (-not $SVC) { $SVC = 'c3916e7f-7bea-4490-b9d1-0d4064adbe8b' }
$OUTCOME = $env:OUTCOME; if (-not $OUTCOME) { $OUTCOME = '462bac1a-568a-4f1f-9548-1c7b61792b4b' }

Write-Host "[1/6] Открытие точки обслуживания для $USER"
Invoke-RestMethod -Method Post -Uri "$BASE/servicepoint/branches/$BR/servicePoints/$SP/workProfiles/$WP/users/$USER/open" | Out-Null

Write-Host "[2/6] Создание визита через точку входа (entryPoint=$ENTRY)"
Invoke-RestMethod -Method Post -Uri "$BASE/entrypoint/branches/$BR/entryPoints/$ENTRY/visit" -ContentType 'application/json' -Body "[`"$SVC`"]" | Out-Null

Write-Host "[3/6] Вызов визита с подтверждением"
$visit = Invoke-RestMethod -Method Post -Uri "$BASE/servicepoint/branches/$BR/servicePoints/$SP/confirmed/visits/call"
if (-not $visit.id) { throw 'Не удалось вызвать визит — возможно очередь пуста' }
$VISIT_ID = $visit.id
Write-Host "  VISIT_ID=$VISIT_ID"

Write-Host "[4/6] Подтверждение прихода"
Invoke-RestMethod -Method Post -Uri "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/confirmed/confirm/$VISIT_ID" | Out-Null

Write-Host "[5/6] Добавление итога услуги ($OUTCOME)"
Invoke-RestMethod -Method Post -Uri "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/outcome/$OUTCOME" | Out-Null

Write-Host "[6/6] Завершение обслуживания"
Invoke-RestMethod -Method Put -Uri "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/visit/end" | Out-Null

Write-Host "Готово. Сценарий успешно выполнен."

