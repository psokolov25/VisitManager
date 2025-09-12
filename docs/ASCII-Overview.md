# VisitManager — ASCII диаграммы и примеры

Ниже — компактные схемы в псевдографике и практичные примеры использования.

## Компоненты и связи

```
Client/UI
   │ HTTP
   ▼
Controllers (Micronaut)
   │
   ├─► VisitService ──► BranchService ──► Branch (in‑memory)
   │        │                  │
   │        ├─► SegmentationRule│
   │        └─► PrinterService  │
   │                             └─► EventService ──► Kafka/DataBus
   │
   └─► KeyCloackClient ──► Keycloak
```

## Данные отделения (Branch)

```
Branch
 ├─ Queues (id → Queue)
 ├─ Services (id → Service)
 ├─ ServicePoints (id → ServicePoint)
 ├─ WorkProfiles (id → WorkProfile)
 ├─ ServiceGroups (id → ServiceGroup)
 ├─ Users (login → User)
 └─ Reception / EntryPoints
```

## Жизненный цикл визита

```
CREATED → PLACED_IN_QUEUE → CALLED → STARTED → SERVED → END
           │                   │          │
           ├── transfer ───────┘          └── return/transfer → WAITING_*
```

## Поток вызова визита

```
UI ──POST /servicepoint/.../call──► ServicePointController
                               └──► VisitService.pick(...) ──► BranchService.updateVisit(...)
                                                           └──► EventService (VISIT_CALLED,...)
```

## Примеры curl (быстрый старт)

- Создать визит через терминал:
  ```bash
  curl -X POST "http://localhost:8080/entrypoint/branches/<BRANCH_ID>/entryPoints/<ENTRY_ID>/visit" \
    -H "Content-Type: application/json" \
    -d '["<SERVICE_ID>"]'
  ```

- Вызвать следующий визит на рабочем месте:
  ```bash
  curl -X POST "http://localhost:8080/servicepoint/branches/<BRANCH_ID>/servicePoints/<SP_ID>/call"
  ```

- Включить режим авто‑вызова отделения:
  ```bash
  curl -X PUT "http://localhost:8080/configuration/branches/<BRANCH_ID>/autocallModeOn"
  ```

Подробные примеры: [REST-Examples.md](REST-Examples.md) и [HTML‑шпаргалка](site/index.html).

