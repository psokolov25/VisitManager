# VisitManager: curl-примеры (local-no-docker)

Ниже — практичные примеры для профиля `local-no-docker`. Перед началом задайте переменные окружения.

## Переменные окружения (bash/zsh)

```
BASE=http://localhost:8080
BR=37493d1c-8282-4417-a729-dceac1f3e2b4
SP=a66ff6f4-4f4a-4009-8602-0dc278024cf2
WP=d5a84e60-e605-4527-b065-f4bd7a385790
Q_OPH=c211ae6b-de7b-4350-8a4c-cff7ff98104e
Q_CASH=8eee7e6e-345a-4f9b-9743-ff30a4322ef5
POOL_SP=099c43c1-40b5-4b80-928a-1d4b363152a8
USER=psokolov
S_HIR=c3916e7f-7bea-4490-b9d1-0d4064adbe8b
OUTCOME_REPEAT=462bac1a-568a-4f1f-9548-1c7b61792b4b
DS_CONSULT=35d73fdd-1597-4d94-a087-fd8a99c9d1ed
```

PowerShell (Windows):

```
$BASE='http://localhost:8080'
$BR='37493d1c-8282-4417-a729-dceac1f3e2b4'
$SP='a66ff6f4-4f4a-4009-8602-0dc278024cf2'
$WP='d5a84e60-e605-4527-b065-f4bd7a385790'
$Q_OPH='c211ae6b-de7b-4350-8a4c-cff7ff98104e'
$Q_CASH='8eee7e6e-345a-4f9b-9743-ff30a4322ef5'
$POOL_SP='099c43c1-40b5-4b80-928a-1d4b363152a8'
$USER='psokolov'
$S_HIR='c3916e7f-7bea-4490-b9d1-0d4064adbe8b'
$OUTCOME_REPEAT='462bac1a-568a-4f1f-9548-1c7b61792b4b'
$DS_CONSULT='35d73fdd-1597-4d94-a087-fd8a99c9d1ed'
```

## Открытие рабочего места

- Открыть СП: `curl -X POST "$BASE/servicepoint/branches/$BR/servicePoints/$SP/workProfiles/$WP/users/$USER/open"`
- Смена рабочего профиля: `curl -X PUT "$BASE/servicepoint/branches/$BR/servicePoints/$SP/workProfiles/$WP"`

## Вызов/подтверждение/повторный вызов

- Вызов с подтверждением: `curl -s -X POST "$BASE/servicepoint/branches/$BR/servicePoints/$SP/confirmed/visits/call"`
- Получить `visitId` (bash + jq): `VISIT_ID=$(curl -s -X POST "$BASE/servicepoint/branches/$BR/servicePoints/$SP/confirmed/visits/call" | jq -r '.id')`
- Подтверждение: `curl -X POST "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/confirmed/confirm/$VISIT_ID"`
- Повторный вызов: `curl -X POST "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/confirmed/recall/$VISIT_ID"`

PowerShell без jq:

```
$VISIT_ID=(Invoke-RestMethod -Method Post -Uri "$BASE/servicepoint/branches/$BR/servicePoints/$SP/confirmed/visits/call").id
Invoke-RestMethod -Method Post -Uri "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/confirmed/confirm/$VISIT_ID"
```

## Заметки, итоги, услуги

- Заметка: `curl -X POST "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/notes" -H 'Content-Type: text/plain' --data 'Заметка'`
- Итог услуги: `curl -X POST "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/outcome/$OUTCOME_REPEAT"`
- Фактическая услуга + итог: `curl -X POST "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/deliveredService/$DS_CONSULT/outcome/$OUTCOME_REPEAT"`

## Переводы визита (transfer)

- Между очередями (в конец): `curl -X PUT "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/queue/$Q_OPH/visit/transferFromQueue/$VISIT_ID?isAppend=true&transferTimeDelay=0"`
- Между очередями (позиция 0): `curl -X PUT "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/queue/$Q_OPH/visit/transferFromQueue/$VISIT_ID?index=0&transferTimeDelay=0"`
- В пул точки (текущий визит): `curl -X PUT "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/poolServicePoint/$POOL_SP/visit/transfer?transferTimeDelay=0"`
- В пул точки (по id визита): `curl -X PUT "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/poolServicePoint/$POOL_SP/visits/$VISIT_ID/transferFromQueue?isAppend=true&transferTimeDelay=0"`
- В пул сотрудника (по id визита): `curl -X PUT "$BASE/servicepoint/branches/$BR/users/$USER_ID/visits/$VISIT_ID?isAppend=true&transferTimeDelay=0"`
- Внешней службой → очередь: `curl -X PUT "$BASE/servicepoint/branches/$BR/queue/$Q_OPH/visits/$VISIT_ID/externalService/transfer?isAppend=true&transferTimeDelay=0" -H 'Content-Type: application/json' -d '{"source":"reception"}'`
- Внешней службой → пул точки: `curl -X PUT "$BASE/servicepoint/branches/$BR/servicePoint/$SP/pool/visits/$VISIT_ID/externalService/transfer?isAppend=true&transferTimeDelay=0" -H 'Content-Type: application/json' -d '{"source":"reception"}'`

Получить userId по логину:

```
USER_ID=$(curl -s "$BASE/servicepoint/branches/$BR/users/user/$USER" | jq -r '.id')
```

PowerShell:

```
$USER_ID=(Invoke-RestMethod -Uri "$BASE/servicepoint/branches/$BR/users/user/$USER").id
```

## Возвраты визита (put_back)

- Вызванный визит → очередь: `curl -X PUT "$BASE/servicepoint/branches/$BR/visits/$VISIT_ID/put_back?returnTimeDelay=60"`
- Текущий визит из СП → очередь: `curl -X PUT "$BASE/servicepoint/branches/$BR/servicePoints/$SP/visit/put_back?returnTimeDelay=60"`
- Текущий визит из СП → пул точки: `curl -X PUT "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/poolServicePoint/$POOL_SP/visit/put_back?returnTimeDelay=60"`
- Текущий визит из СП → пул сотрудника: `curl -X PUT "$BASE/servicepoint/branches/$BR/servicePoints/$SP/users/$USER_ID/put_back?returnTimeDelay=60"`

## Завершение, закрытие, перерыв

- Завершить визит: `curl -X PUT "$BASE/servicepoint/branches/$BR/visits/servicePoints/$SP/visit/end"`
- Закрыть СП: `curl -X POST "$BASE/servicepoint/branches/$BR/servicePoints/$SP/close?isBreak=false"`
- Перерыв: `curl -X POST "$BASE/servicepoint/branches/$BR/servicePoints/$SP/close?isBreak=true&breakReason=LUNCH"`
- Logout: `curl -X POST "$BASE/servicepoint/branches/$BR/servicePoints/$SP/logout?isForced=false"`

