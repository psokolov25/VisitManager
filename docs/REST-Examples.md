# VisitManager REST Примеры

Ниже собраны примеры `curl` для основных REST‑эндпоинтов.

## Переменные окружения (реальные sample‑значения)

- Базовый URL: `BASE_URL=http://localhost:8080`
- Идентификаторы:
  - `BRANCH_ID=37493d1c-8282-4417-a729-dceac1f3e2b4`
  - `SERVICE_POINT_ID=a66ff6f4-4f4a-4009-8602-0dc278024cf2`
  - `QUEUE_ID=c211ae6b-de7b-4350-8a4c-cff7ff98104e`
  - `ENTRY_POINT_ID=2`
  - `PRINTER_ID=eb7ea46d-c995-4ca0-ba92-c92151473614`
  - `WORK_PROFILE_ID=d5a84e60-e605-4527-b065-f4bd7a385790`
  - `SERVICE_ID=c3916e7f-7bea-4490-b9d1-0d4064adbe8b`
  - `VISIT_ID=55da9b66-c928-4d47-9811-dbbab20d3780`
  - `OUTCOME_ID=462bac1a-568a-4f1f-9548-1c7b61792b4b`
  - `DELIVERED_SERVICE_ID=35d73fdd-1597-4d94-a087-fd8a99c9d1ed`
  - `MARK_ID=04992364-9e96-4ec9-8a05-923766aa57e7`
  - `USER_ID=f2fa7ddc-7ff2-43d2-853b-3b548b1b3a89`
  - `POOL_SERVICE_POINT_ID=a66ff6f4-4f4a-4009-8602-0dc278024cf2`
  - `USER_NAME=ivanov`
  - `STAFF_SID=SID-STAFF`

Установка переменных:
- Bash: `export BASE_URL=...` и т.д.
- PowerShell: `$env:BASE_URL="..."` и т.д.

В примерах ниже можно подставлять значения напрямую или использовать переменные.

## Зона обслуживания

- Свободные точки обслуживания:
  - `GET /servicepoint/branches/{branchId}/servicePoints/getFree`
  - Пример: `curl -X GET "$BASE_URL/servicepoint/branches/$BRANCH_ID/servicePoints/getFree"`

- Открыть точку обслуживания:
  - `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/workProfiles/{workProfileId}/users/{userName}/open`
  - Пример: `curl -X POST "$BASE_URL/servicepoint/branches/$BRANCH_ID/servicePoints/$SERVICE_POINT_ID/workProfiles/$WORK_PROFILE_ID/users/$USER_NAME/open"`

- Закрыть ТО / Выход:
  - `POST "$BASE_URL/servicepoint/branches/$BRANCH_ID/servicePoints/$SERVICE_POINT_ID/close"`
  - `POST "$BASE_URL/servicepoint/branches/$BRANCH_ID/servicePoints/$SERVICE_POINT_ID/logout"`

- Вызов визита (разные режимы):
  - По `visitId`: `curl -X POST "$BASE_URL/servicepoint/branches/$BRANCH_ID/visits/servicePoints/$SERVICE_POINT_ID/visits/$VISIT_ID/call"`
  - Максимальное ожидание: `curl -X POST "$BASE_URL/servicepoint/branches/$BRANCH_ID/servicePoints/$SERVICE_POINT_ID/call"`
  - Из списка очередей: `curl -X POST "$BASE_URL/servicepoint/branches/$BRANCH_ID/servicePoints/$SERVICE_POINT_ID/callfromQueues" -H "Content-Type: application/json" -d '["'$QUEUE_ID'"]'`
  - No‑show/confirm/recall — см. аналогичные пути `confirmed/*` в контроллере.

- Переводы визита:
  - ТО → очередь: `curl -X PUT "$BASE_URL/servicepoint/branches/$BRANCH_ID/visits/servicePoints/$SERVICE_POINT_ID/queue/$QUEUE_ID/visit/transferFromServicePoint?isAppend=true&transferTimeDelay=0" -d @visit.json -H "Content-Type: application/json"`
  - Очередь → очередь: `curl -X PUT "$BASE_URL/servicepoint/branches/$BRANCH_ID/visits/servicePoints/$SERVICE_POINT_ID/queue/$QUEUE_ID/visit/transferFromQueue?isAppend=true&transferTimeDelay=0" -d @visit.json -H "Content-Type: application/json"`
  - Очередь → пул ТО: `curl -X PUT "$BASE_URL/servicepoint/branches/$BRANCH_ID/visits/servicePoints/$SERVICE_POINT_ID/poolServicePoint/$POOL_SERVICE_POINT_ID/visit/transferFromQueue?isAppend=true&transferTimeDelay=0" -d @visit.json -H "Content-Type: application/json"`
  - Очередь → пул пользователя: `curl -X PUT "$BASE_URL/servicepoint/branches/$BRANCH_ID/users/$USER_ID" -d @visit.json -H "Content-Type: application/json"`

## Зона ожидания

- Доступные услуги: `curl -X GET "$BASE_URL/entrypoint/branches/$BRANCH_ID/services"`
- Все услуги: `curl -X GET "$BASE_URL/entrypoint/branches/$BRANCH_ID/services/all"`
- Создание визита через вход: `curl -X POST "$BASE_URL/entrypoint/branches/$BRANCH_ID/entryPoints/$ENTRY_POINT_ID/visit?printTicket=true" -H "Content-Type: application/json" -d '["'$SERVICE_ID'"]'`
- Создание визита (параметры): `curl -X POST "$BASE_URL/entrypoint/branches/$BRANCH_ID/entryPoints/$ENTRY_POINT_ID/visitWithParameters?printTicket=false" -H "Content-Type: application/json" -d '{"serviceIds":["'$SERVICE_ID'"],"parameters":{"sex":"male","age":"33"}}'`

## Конфигурация

- Обновить ветви: `curl -X POST "$BASE_URL/configuration/branches" -H "Content-Type: application/json" -d '{"'$BRANCH_ID'":{"id":"'$BRANCH_ID'","name":"Demo"}}'`
- Обновить услуги: `curl -X PUT "$BASE_URL/configuration/branches/$BRANCH_ID/services?checkVisits=true" -H "Content-Type: application/json" -d '{"'$SERVICE_ID'":{"id":"'$SERVICE_ID'","name":"Услуга"}}'`
- Удалить услуги: `curl -X DELETE "$BASE_URL/configuration/branches/$BRANCH_ID/services?checkVisits=false" -H "Content-Type: application/json" -d '["'$SERVICE_ID'"]'`
- Вкл/выкл авто‑вызов отделения: `curl -X PUT "$BASE_URL/configuration/branches/$BRANCH_ID/autocallModeOn"` / `curl -X PUT "$BASE_URL/configuration/branches/$BRANCH_ID/autocallModeOff"`

Больше примеров см. JavaDoc в контроллерах.

## 📘 Пример Micronaut HttpClient

```java
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.HttpRequest;

try (HttpClient client = HttpClient.create(new URL(BASE_URL))) {
    BlockingHttpClient blocking = client.toBlocking();
    HttpRequest<?> req = HttpRequest.GET("/managementinformation/branches");
    String json = blocking.retrieve(req);
    System.out.println(json);
}
```
