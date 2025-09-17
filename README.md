# VisitManager

Служба управления визитами и клиентопотоком, построенная на Micronaut 4.7.6 и Java 17. Сервис управляет очередями, точками обслуживания, сотрудниками и визитами клиентов в отделениях.

![Java](https://img.shields.io/badge/Java-17-007396)
![Micronaut](https://img.shields.io/badge/Micronaut-4.7.6-1C1C1C)
![Build](https://img.shields.io/badge/Build-Maven-blue)
[![Tests](https://img.shields.io/badge/tests-351%20passing-brightgreen)](#-тестирование)
[![Docs](https://img.shields.io/badge/Docs-Use%20Cases-blue)](docs/use-cases.md)
[![Coverage](https://img.shields.io/badge/Coverage-43.8%25-orange)](#-тестирование)
![Docker](https://img.shields.io/badge/Docker-ready-blue)
[![License: Named User](https://img.shields.io/badge/License-Простая%20Named%20User-blue)](#-лицензия)
[![Contributing](https://img.shields.io/badge/Contributing-guidelines-blue)](#-contributing)

## 📑 Содержание
- [🧾 Обзор](#-обзор)
- [⚙️ Настройка окружения](#-настройка-окружения)
- [🏗️ Архитектура](#-архитектура)
- [🔄 Логика работы](#-логика-работы)
- [🗂️ Структура проекта](#-структура-проекта)
- [👥 Инструкции по ролям](#-инструкции-по-ролям)
  - [🛠️ DevOps](#-devops)
  - [🏛️ Архитектор](#-архитектор)
  - [📊 Аналитик](#-аналитик)
  - [🧪 Тестировщик](#-тестировщик)
  - [💻 Front End разработчик](#-front-end-разработчик)
  - [🧰 Back End разработчик](#-back-end-разработчик)
  - [🔗 Интегратор](#-интегратор)
- [📡 REST API](#-rest-api)
- [🕹️ Работа пульта оператора](#-работа-пульта-оператора)
- [🏧 Работа приемной(ресепшен)](#-работа-приемной-ресепшен)
- [🏧 Работа терминала клиента](#-работа-терминале-клиента)
- [📦 Примеры кода](#-примеры-кода)
- [📊 Диаграммы](#-диаграммы)
- [🧑‍💼 Сценарии работы сотрудника](#-сценарии-работы-сотрудника)
- [🤖 Документация для автотестеров](#-документация-для-автотестеров)
- [🧪 Тестирование](#-тестирование)
  - [Модульные тесты](#модульные-тесты)
  - [Интеграционные тесты](#интеграционные-тесты)
- [🌐 Переменные окружения](#-переменные-окружения)
- [🔗 Полезные ссылки](#-полезные-ссылки)
- [🤝 Contributing](#-contributing)
- [📄 Лицензия](#-лицензия)

## 🧾 Обзор
VisitManager предоставляет REST‑интерфейсы для создания визитов, управления очередями и обслуживания клиентов. Сервис обрабатывает события асинхронно через Kafka, кеширует конфигурацию в Redis и использует Keycloak для аутентификации. Поддерживает горизонтальное масштабирование и расширяемую модель домена.

## ⚙️ Настройка окружения

### Требования
- JDK 17
- Maven 3 (локально установленный `mvn`, поддерживающий сохранение настроек и работу через прокси)
- Подключение к Maven Central или зеркалу
- Docker 20+ и Docker Compose для локального стенда

### Сборка и запуск
```bash
# полная сборка
JAVA_TOOL_OPTIONS='-Djava.net.preferIPv4Stack=true' mvn -s .mvn/settings.xml clean verify
# запуск приложения
java -jar target/visitmanager.jar
# запуск в Docker
docker compose -f docker-compose.local.yml up -d --build
```

### Micronaut профиль `local-no-docker`
Используется для локальной разработки без Docker и внешних сервисов. Подменяет интеграции заглушками.
```bash
MICRONAUT_ENVIRONMENTS=local-no-docker \
JAVA_TOOL_OPTIONS='-Djava.net.preferIPv4Stack=true' mvn -s .mvn/settings.xml mn:run
```


### Работа за прокси
Проект уже содержит файл `.mvn/settings.xml` с настройками прокси, поэтому достаточно запускать Maven с опцией `-s .mvn/settings.xml`.
Если нужно переопределить настройки, добавьте в `~/.m2/settings.xml`:
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <proxies>
    <proxy>
      <id>http</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy</host>
      <port>8080</port>
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
    <proxy>
      <id>https</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>proxy</host>
      <port>8080</port>
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
```

### Временные файлы
JVM может создавать файлы `hs_err_pid*.log` и другие временные дампы при аварийных остановках.
Они исключены через `.gitignore` и не должны попадать в репозиторий.

## 🏗️ Архитектура
Сервис построен по слоистой архитектуре:

- **API** (`ru.aritmos.api`) — REST‑контроллеры.
- **Сервисный слой** (`ru.aritmos.service`) — бизнес‑логика.
- **Модели** (`ru.aritmos.model`) — доменные объекты.
- **Интеграции** (`ru.aritmos.clients`, `ru.aritmos.keycloack`, `ru.aritmos.events`) — взаимодействие с внешними сервисами.

Внешние компоненты: Keycloak, Redis, Kafka, PrinterService, DataBus.

Запросы проходят через контроллеры, затем бизнес‑логика обращается к сервисам и события публикуются в Kafka. Кэширование конфигурации и визитов выполняется в Redis, что позволяет уменьшить нагрузку на базовые системы.


## 🔄 Логика работы
1. **Создание визита** — контроллер `EntrypointController` проверяет доступные услуги и создаёт визит через `VisitService`.
2. **Обслуживание** — `ServicePointController` управляет вызовом, подтверждением, переводом и завершением визитов, фиксируя результат в Redis.
3. **Конфигурация** — `ConfigurationController` обновляет настройки отделений и синхронизирует их между узлами через Kafka.
4. **Мониторинг** — `ManagementController` предоставляет сведения об отделениях, очередях и пользователях, формирует отчёты.
5. **События** — `EventService` публикует изменения визитов в Kafka, планирует отложенные события и отправляет нотификации внешним системам.

Подробный разбор сценариев приведён в файле [docs/use-cases.md](docs/use-cases.md).

## 🗂️ Структура проекта
Основные каталоги и их назначение:
```
src/
  main/java/ru/aritmos/
    api/           REST-контроллеры
    clients/       HTTP-клиенты конфигурации, печати и шины данных
    config/        профильные бины и заглушки (например, local-no-docker)
    docs/          генераторы документации и вспомогательные утилиты
    events/        модели, клиенты и сервисы событий
    exceptions/    бизнес- и системные исключения
    handlers/      регистрация обработчиков событий
    keycloack/     интеграция с Keycloak (клиенты, модели, безопасность)
    model/         доменные сущности (Branch, Service, Visit и т.д.)
    service/       бизнес-логика и правила вызова
  main/resources/  конфигурация Micronaut, схемы GraphQL, настройки логирования
  test/            модульные и интеграционные тесты
docs/              документация, диаграммы, curl-примеры
scripts/           демо-сценарии и утилиты для локального запуска
```

## 👥 Инструкции по ролям

### 🛠️ DevOps
- Используйте `docker-compose.yml` для запуска зависимостей (Redis, Kafka, Keycloak).
- Параметры среды задаются через переменные окружения и `.env.*` файлы.
- Для CI выполняйте `mvn -s .mvn/settings.xml clean verify` и публикуйте артефакт `visitmanager.jar`.
- Логи пишутся через Logback; для централизованного сбора можно использовать Loki (`loki.properties`).
- Мониторинг метрик и здоровья сервисов ведите через Prometheus/Grafana.

### 🏛️ Архитектор
- Архитектура модульна: контроллеры → сервисы → модели.
- Расширение функциональности выполняется через добавление сервисов и контроллеров.
- Правила вызова и сегментации реализованы как внедряемые стратегии (`ru.aritmos.service.rules`).
- При изменениях обновляйте диаграммы в [docs/diagrams](docs/diagrams) и следите за совместимостью API.

### 📊 Аналитик
- Доменные объекты находятся в `ru.aritmos.model`.
- Бизнес‑процессы: создание визита, обслуживание, перевод, завершение.
- Дополнительные артефакты: [REST-Examples.md](docs/REST-Examples.md), [ASCII-Overview.md](docs/ASCII-Overview.md).
- Документируйте новые сценарии и отчёты в [docs/use-cases.md](docs/use-cases.md).

### 🧪 Тестировщик
- Тесты: `JAVA_TOOL_OPTIONS='-Djava.net.preferIPv4Stack=true' mvn -s .mvn/settings.xml test`.
- Интеграционные тесты с внешними службами: `mvn -s .mvn/settings.xml -Pit-external verify`.
- Интеграционные тесты с тестовыми ресурсами: `mvn -s .mvn/settings.xml -Pit-resources verify`.
- Для ручной проверки используйте примеры curl из раздела [REST API](#-rest-api).
- При сложных сценариях применяйте Testcontainers или мок‑сервисы.

### 💻 Front End разработчик
- Swagger UI доступен по `/swagger-ui`.
- Используйте REST API для отображения очередей, создания и обслуживания визитов.
- Аутентификация через Keycloak (OIDC). Токен передаётся в `Authorization: Bearer`.
- Моки и регрессионное тестирование можно выполнять в Postman или через WireMock.

### 🧰 Back End разработчик
- Код контроллеров в `ru.aritmos.api`.
- Бизнес‑логика — `ru.aritmos.service`.
- Перед добавлением функциональности пишите модульные тесты и обновляйте REST‑примеры.
- Используйте Lombok и реактивные клиенты Micronaut при необходимости.

### 🔗 Интегратор
- Внешние вызовы: `ConfigurationClient`, `PrinterClient`, `KeyCloackClient`, `DataBusClient`.
- События публикуются в Kafka (топики конфигурируются в `application.yml`).
- Для stub‑режима используйте профиль `local-no-docker`, который подменяет клиенты заглушками.
- Перед выкладкой проверяйте интеграции в тестовом стенде и документируйте контракты.

## 📡 REST API
Обзор основных контроллеров и типичных вызовов.

### Варианты ответа сервера
Все контроллеры возвращают стандартные HTTP-коды:


- `200 OK` — успешный запрос.
- `204 No Content` — успешный запрос без содержимого.
- `207 Multi-Status` — режим автоматического вызова уже активен или отключён.
- `400 Bad Request` — некорректный запрос.
- `401 Unauthorized` — пользователь не авторизован.
- `403 Forbidden` — доступ запрещён.
- `404 Not Found` — ресурс не найден.
- `405 Method Not Allowed` — HTTP-метод не поддерживается для ресурса.
- `409 Conflict` — конфликт данных или состояния.
- `415 Unsupported Media Type` — неподдерживаемый тип данных.
- `500 Internal Server Error` — ошибка сервера.


### ManagementController
```bash
# список отделений
curl http://localhost:8080/managementinformation/branches
# короткий список
curl http://localhost:8080/managementinformation/branches/tiny
```

### EntrypointController
```bash
# создание визита
curl -X POST \
  'http://localhost:8080/entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit' \
  -H 'Content-Type: application/json' \
  -d '["serviceId1","serviceId2"]'
```

### ServicePointController
```bash
# открыть точку обслуживания
curl -X POST 'http://localhost:8080/servicepoint/branches/{branchId}/servicePoints/{spId}/workProfiles/{wpId}/users/{user}/open'

# вызов визита и подтверждение
VISIT_ID=$(curl -s -X POST 'http://localhost:8080/servicepoint/branches/{branchId}/servicePoints/{spId}/confirmed/visits/call' | jq -r '.id')
curl -X POST "http://localhost:8080/servicepoint/branches/{branchId}/visits/servicePoints/{spId}/confirmed/confirm/${VISIT_ID}"
```

### Дополнительные примеры
- добавление заметки: `POST /servicepoint/branches/{branchId}/visits/servicePoints/{spId}/notes`
- завершение визита: `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{spId}/visit/end`
- перевод в очередь: `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{spId}/queue/{queueId}/visit/transferFromQueue/{visitId}`

Полный список запросов см. в [docs/curl-examples.md](docs/curl-examples.md) и Swagger UI.

## 🏧 Работа терминале клиента

Терминал самообслуживания использует ограниченный набор REST-запросов для оформления визита и отображения времени ожидания. Все обращения выполняются от имени авторизованного клиента терминала; при отсутствии сессии система вернёт `401 Unauthorized` или `403 Forbidden`.

1. **POST `/entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit`** — создаёт визит по выбранным услугам и опционально печатает талон.
    - Контроллер: `EntrypointController#createVisit` 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L123-L198】
    - Ответы:
        - `200 OK` — визит создан и возвращён в ответе.
        - `400 Bad Request` — переданы некорректные параметры (например, пустой список услуг).
        - `404 Not Found` — не найдены отделение, выбранные услуги, точка терминала или очередь для услуг.
        - `500 Internal Server Error` — внутренняя ошибка сервера.

2. **POST `/entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visitWithParameters`** — создаёт визит с передачей пользовательских параметров (анкеты, сегментация).
    - Контроллер: `EntrypointController#createVisit` (перегрузка с параметрами) 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L201-L285】
    - Ответы:
        - `200 OK` — визит создан с указанными параметрами.
        - `400 Bad Request` — нарушены требования к телу запроса.
        - `404 Not Found` — не найдены отделение, услуги, точка терминала, очередь или правило сегментации и его данные.
        - `500 Internal Server Error` — внутренняя ошибка сервера.

3. **GET `/servicepoint/branches/{branchId}/queues/{queueId}/visits/`** — возвращает визиты в выбранной очереди, упорядоченные по времени ожидания.
    - Контроллер: `ServicePointController#getVisits` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L647-L676】
    - Ответы:
        - `200 OK` — список ожидающих визитов.
        - `404 Not Found` — отделение или очередь отсутствуют.
        - `500 Internal Server Error` — ошибка сервера при получении данных.

4. **GET `/entrypoint/branches/{branchId}/services`** — выдаёт перечень услуг, доступных для оформления талона в отделении.
    - Контроллер: `EntrypointController#getAllAvailableServices` 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L410-L433】
    - Ответы:
        - `200 OK` — доступные услуги возвращены в ответе.
        - `404 Not Found` — отделение отсутствует или недоступно.
        - `500 Internal Server Error` — ошибка сервера при получении услуг.
## 🕹️ Работа пульта оператора

Пульт оператора объединяет данные о сотрудниках, очередях и визитах, обеспечивая полный цикл обслуживания клиента: от открытия рабочего места до завершения визита. Ниже перечислены основные действия и используемые REST‑точки. Для каждого запроса указаны варианты ответов (HTTP-коды), которые важно обрабатывать на фронтенде; при отсутствии авторизации возможны ответы `401 Unauthorized` или `403 Forbidden`.

### Подготовка рабочего места
- `GET /managementinformation/branches/tiny` — компактный список отделений.
  - `200 OK` — массив `{ id, name }` доступных отделений.
  - `400 Bad Request` — некорректный фильтр или параметры запроса.
  - `500 Internal Server Error` — ошибка сервиса управления отделениями.
- `GET /managementinformation/branches/{id}` — подробная информация об отделении.
  - `200 OK` — состояние отделения и его конфигурация.
  - `404 Not Found` — отделение не найдено.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/workProfiles` — перечень рабочих профилей отделения.
  - `200 OK` — список профилей для выбора.
  - `404 Not Found` — отделение отсутствует.
  - `500 Internal Server Error` — ошибка сервиса.
- `GET /servicepoint/branches/{branchId}/servicePoints` — доступные точки обслуживания и их статус.
  - `200 OK` — список точек с признаком доступности.
  - `404 Not Found` — отделение не найдено.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/servicePoints/detailed` — расширенные данные точек обслуживания (включая пулы и занятость).
  - `200 OK` — подробная информация о точках.
  - `404 Not Found` — отделение не найдено.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/servicePoints/user/{userName}` — поиск точки обслуживания сотрудника в отделении.
  - `200 OK` — точка найдена.
  - `404 Not Found` — отделение или сотрудник отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/servicePoints/user/{userName}` — глобальный поиск точки обслуживания по логину.
  - `200 OK` — точка найдена.
  - `404 Not Found` — сотрудник не найден.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/workProfiles/{workProfileId}/users/{userName}/open` — закрепление рабочего места за сотрудником.
  - `200 OK` — рабочее место открыто.
  - `404 Not Found` — отделение, рабочий профиль или точка обслуживания отсутствуют.
  - `409 Conflict` — точка уже занята другим сотрудником (в ответе приходит информация о занятой станции).
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/workProfile/{workProfileId}/services` — услуги, доступные по выбранному профилю.
  - `200 OK` — массив услуг профиля.
  - `404 Not Found` — отделение или рабочий профиль не найдены.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /entrypoint/branches/{branchId}/services` — клиентские услуги отделения для карточки визита.
  - `200 OK` — список доступных услуг.
  - `404 Not Found` — отделение отсутствует.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /entrypoint/branches/{branchId}/services/all` — полный каталог услуг отделения.
  - `200 OK` — расширенный список услуг.
  - `404 Not Found` — отделение не найдено.
  - `500 Internal Server Error` — ошибка сервера.

### Мониторинг сотрудников и очередей
- `GET /servicepoint/branches/{branchId}/workingusers` — состав смены и статусы сотрудников.
  - `200 OK` — список активных сотрудников.
  - `404 Not Found` — отделение отсутствует.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/queues` — очереди отделения.
  - `200 OK` — массив очередей.
  - `404 Not Found` — отделение не найдено.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/queues/{queueId}/visits/` — талоны внутри очереди.
  - `200 OK` — список визитов, отсортированный по времени ожидания.
  - `404 Not Found` — отделение или очередь отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/queues` — очереди, доступные конкретной точке обслуживания.
  - `200 OK` — доступные очереди.
  - `403 Forbidden` — точка недоступна или сотрудник не авторизован.
  - `404 Not Found` — отделение или точка обслуживания отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/servicePoints/{servicePointId}` — состояние точки обслуживания (учитывает сотрудника на перерыве).
  - `200 OK` — возвращены актуальные данные точки.
  - `404 Not Found` — отделение или точка не найдены.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /configuration/branches/{branchId}/break/reasons` — причины перерыва.
  - `200 OK` — словарь причин.
  - `404 Not Found` — отделение отсутствует.
  - `500 Internal Server Error` — ошибка сервера.

### Управление визитами и очередями
- `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/call` — вызов талона с максимальным временем ожидания.
  - `200 OK` — визит вызван и возвращён в теле ответа.
  - `204 No Content` — очередь пуста.
  - `207 Multi-Status` — режим автоматического вызова уже включён.
  - `403 Forbidden` — сотрудник не авторизован или точка недоступна.
  - `404 Not Found` — отделение не найдено.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/call` — вызов талона с подтверждением прихода.
  - `200 OK` — визит вызван.
  - `204 No Content` — очередь пуста.
  - `207 Multi-Status` — режим автоматического вызова активен.
  - `403 Forbidden` — нет доступа к точке обслуживания.
  - `404 Not Found` — отделение не найдено.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/confirmed/call/visit` — вызов выбранного визита (объект `Visit` в теле) с ожиданием подтверждения.
  - `200 OK` — визит переведён в статус вызова.
  - `207 Multi-Status` — автоматический вызов уже работает.
  - `403 Forbidden` — нет доступа к точке обслуживания.
  - `404 Not Found` — отделение или визит не найдены.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/call/{visitId}` — вызов по идентификатору с подтверждением прихода.
  - `200 OK` — визит вызван.
  - `207 Multi-Status` — авто-вызов активен.
  - `403 Forbidden` — нет доступа к точке обслуживания.
  - `404 Not Found` — отделение, визит или точка обслуживания отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/visits/{visitId}/call` — вызов конкретного талона без подтверждения.
  - `200 OK` — визит вызван и переведён в статус CALLED.
  - `404 Not Found` — отделение, очередь или точка обслуживания отсутствуют.
  - `409 Conflict` — визит уже вызван.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/confirm/{visitId}` — подтверждение прихода клиента.
  - `200 OK` — визит переведён в статус CONFIRMED.
  - `404 Not Found` — визит или точка обслуживания отсутствуют.
  - `409 Conflict` — визит уже подтверждён.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/recall/{visitId}` — повторный вызов клиента.
  - `200 OK` — повторное оповещение отправлено.
  - `404 Not Found` — визит или точка обслуживания отсутствуют.
  - `409 Conflict` — визит уже вызван.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/noshow/{visitId}` — отметка о неявке клиента.
  - `200 OK` — визит переведён в статус NO_SHOW.
  - `404 Not Found` — отделение или визит отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/visit/put_back` — вернуть текущий визит в очередь.
  - `200 OK` — визит возвращён.
  - `404 Not Found` — отделение или точка обслуживания отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/visits/{visitId}/put_back` — вернуть вызванный визит в очередь.
  - `200 OK` — визит возвращён с задержкой `returnTimeDelay`.
  - `404 Not Found` — отделение или визит отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/postpone` — отложить визит в текущей точке.
  - `200 OK` — визит отложен.
  - `404 Not Found` — отделение или точка обслуживания отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `DELETE /servicepoint/branches/{branchId}/visits/{visitId}` — отменить визит.
  - `204 No Content` — визит удалён.
  - `404 Not Found` — визит отсутствует.
  - `409 Conflict` — визит нельзя удалить (например, уже обслуживается).
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/servicePoins/{servicePointId}/cancelAutoCall` — отключить автоматический вызов.
  - `200 OK` — авто-вызов остановлен.
  - `207 Multi-Status` — режим уже был отключён.
  - `404 Not Found` — отделение или точка обслуживания отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end` — завершение обслуживания визита.
  - `200 OK` — визит завершён.
  - `404 Not Found` — отделение или точка обслуживания отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.

### Переводы и ручное распределение
- `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromQueueToStartOrToEnd/{visitId}` — перенос талона между очередями с выбором позиции.
  - `200 OK` — визит перемещён.
  - `404 Not Found` — отделение, очередь, визит или сотрудник отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/users/{userId}/visits/{visitId}` — перенос визита в пул сотрудника.
  - `200 OK` — визит добавлен в пул.
  - `404 Not Found` — отделение, сотрудник или визит отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/users/{userId}/transfer` — вернуть визит из точки обслуживания в пул сотрудника.
  - `200 OK` — визит возвращён в пул.
  - `404 Not Found` — отделение, точка или сотрудник отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visits/{visitId}/transferFromQueue` — забрать визит из очереди в пул точки обслуживания.
  - `200 OK` — визит добавлен в пул точки.
  - `404 Not Found` — отделение, точка, пул или визит отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/transfer` — отправить визит из точки обслуживания в пул.
  - `200 OK` — визит перемещён в пул.
  - `404 Not Found` — отделение или точка отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromServicePoint` — вернуть визит из точки обслуживания в конкретную очередь.
  - `200 OK` — визит перемещён в очередь.
  - `404 Not Found` — отделение, очередь или точка отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.

### Работа с услугами и результатами
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/services` — пакетное добавление услуг в визит.
  - `200 OK` — услуги добавлены.
  - `404 Not Found` — отделение, визит или точка отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/services/{serviceId}` — добавление отдельной услуги.
  - `200 OK` — услуга добавлена.
  - `404 Not Found` — отделение, услуга, визит или точка отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredservice/{deliveredServiceId}` — назначение фактической услуги.
  - `200 OK` — фактическая услуга добавлена.
  - `404 Not Found` — отделение, услуга, точка или конфигурация фактической услуги отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/services/{serviceId}/deliveredServices` — возможные фактические услуги для выбранной услуги.
  - `200 OK` — список доступных фактических услуг.
  - `404 Not Found` — отделение или услуга отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `DELETE /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredServices/{deliveredServiceId}` — удаление фактической услуги из визита.
  - `200 OK` — фактическая услуга удалена.
  - `404 Not Found` — отделение, услуга или точка отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/outcome/{outcomeId}` — итог по текущей услуге.
  - `200 OK` — итог сохранён.
  - `404 Not Found` — отделение, услуга, визит или точка отсутствуют.
  - `409 Conflict` — итог недоступен для текущей услуги.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredService/{deliveredServiceId}/outcome/{outcomeId}` — итог по фактической услуге.
  - `200 OK` — итог сохранён.
  - `404 Not Found` — отделение, услуга, визит, точка или фактическая услуга отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `DELETE /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/service/{serviceId}/outcome` — удаление итога текущей услуги.
  - `200 OK` — итог удалён.
  - `404 Not Found` — отделение, услуга, визит или точка отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `DELETE /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredServices/{deliveredServiceId}/outcome` — удаление итога фактической услуги.
  - `200 OK` — итог удалён.
  - `404 Not Found` — отделение, визит, точка или фактическая услуга отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `GET /servicepoint/branches/{branchId}/visits/{visitId}/notes` — заметки визита.
  - `200 OK` — список заметок.
  - `404 Not Found` — отделение или визит отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/notes` — добавление текстовой заметки.
  - `200 OK` — заметка добавлена.
  - `404 Not Found` — отделение или визит отсутствуют.
  - `500 Internal Server Error` — ошибка сервера.

### Завершение смены
- `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/close` — закрыть рабочую станцию.
  - `200 OK` — точка закрыта.
  - `404 Not Found` — отделение или точка отсутствуют.
  - `409 Conflict` — точка уже закрыта.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/logout` — закрыть точку и завершить сессию сотрудника.
  - `200 OK` — точка закрыта, пользователь разлогинен.
  - `404 Not Found` — отделение или точка отсутствуют.
  - `409 Conflict` — точка уже закрыта.
  - `500 Internal Server Error` — ошибка сервера.
- `POST /entrypoint/branches/{branchId}/servicePoint/{servicePointId}/virtualVisit` — создание визита без печати талона (виртуальный талон).
  - `200 OK` — визит создан.
  - `404 Not Found` — отделение, услуги или очередь отсутствуют.
  - `409 Conflict` — визит уже создан (дублирование параметров).
  - `500 Internal Server Error` — ошибка сервера.

### Типовые сценарии
1. **Создание виртуального талона и завершение обслуживания**
   1. `POST /entrypoint/branches/{branchId}/servicePoint/{servicePointId}/virtualVisit` — оператор создаёт талон.
   2. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end` — визит закрывается после обслуживания.
2. **Создание виртуального талона с итогами и дополнительными услугами**
   1. `POST /entrypoint/branches/{branchId}/servicePoint/{servicePointId}/virtualVisit` — формирование визита.
   2. `GET /servicepoint/branches/{branchId}/services/{serviceId}/deliveredServices` — выбор фактических услуг.
   3. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/outcome/{outcomeId}` — фиксация результата основной услуги.
   4. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredservice/{deliveredServiceId}` — добавление фактической услуги.
   5. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredService/{deliveredServiceId}/outcome/{outcomeId}` — итог по фактической услуге.
   6. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end` — завершение визита.
3. **Вызов талона по времени ожидания и завершение**
   1. `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/call` — выбор талона с максимальным временем ожидания.
   2. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end` — закрытие визита после обслуживания.

## 🖥️ Работа приемной (ресепшен)

Ниже приведены REST-запросы, которые использует приемная (ресепшен) в менеджере визитов. Для каждого запроса указан контроллер Micronaut, который его обрабатывает, и основные варианты ответов сервера. Все вызовы защищены авторизацией; при отсутствии токена ожидайте `401 Unauthorized` или `403 Forbidden`.


1. **GET `/managementinformation/branches/tiny`** — компактный список отделений для выбора филиала.
   - Контроллер: `ManagementController#getTinyBranches` 【F:src/main/java/ru/aritmos/api/ManagementController.java†L127-L142】
   - Ответы:
     - `200 OK` — JSON-массив `{ id, name }`.
     - `400 Bad Request` или `500 Internal Server Error` при ошибках.
2. **GET `/servicepoint/branches/{branchId}/servicePoints`** — список точек обслуживания отделения для выбора рабочего места.
   - Контроллер: `ServicePointController#getServicePoints` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L168-L193】
   - Ответы:
     - `200 OK` — точки с признаком доступности.
     - `404 Not Found` или `500 Internal Server Error` при ошибках.
3. **GET `/servicepoint/branches/{branchId}/users`** — возвращает сотрудников отделения и их статусы.
   - Контроллер: `ServicePointController#getUsersOfBranch` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L268-L291】
   - Ответы:
     - `200 OK` — массив сотрудников.
     - `404 Not Found` или `500 Internal Server Error` при ошибках.
4. **GET `/entrypoint/branches/{branchId}/services/all`** — полный каталог услуг отделения для фильтров и карточек визита.
   - Контроллер: `EntrypointController#getAllServices` 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L442-L457】
   - Ответы:
     - `200 OK` — список услуг.
     - `404 Not Found` или `500 Internal Server Error` при ошибках.
5. **GET `/servicepoint/branches/{branchId}/queues/full`** — подробные данные по очередям отделения.
   - Контроллер: `ServicePointController#getFullQueues` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L150-L165】
   - Ответы:
     - `200 OK` — очереди и талоны.
     - `404 Not Found` или `500 Internal Server Error` при ошибках.
6. **DELETE `/servicepoint/branches/{branchId}/visits/{visitId}`** — отменяет визит по идентификатору.
   - Контроллер: `ServicePointController#deleteVisit` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2334-L2363】
   - Ответы:
     - `204 No Content` — успешная отмена.
     - `404 Not Found`, `409 Conflict` или `500 Internal Server Error` при ошибках.
7. **GET `/servicepoint/branches/{branchId}/printers`** — список доступных принтеров отделения.
   - Контроллер: `ServicePointController#getPrinters` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L102-L117】
   - Ответы:
     - `200 OK` — перечень устройств.
     - `404 Not Found` или `500 Internal Server Error` при ошибках.
8. **GET `/servicepoint/branches/{branchId}`** — возвращает сводные данные отделения (делегирует `ManagementController#getBranch`).
   - Контроллер: `ManagementController#getBranch` 【F:src/main/java/ru/aritmos/api/ManagementController.java†L65-L71】
   - Ответы:
     - `200 OK` — JSON отделения.
     - `404 Not Found` или `500 Internal Server Error` при ошибках.
9. **POST `/entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=true`** — создаёт визит с печатью талона на выбранном принтере.
   - Контроллер: `EntrypointController#createVisitFromReception` 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L302-L372】
   - Ответы:
     - `200 OK` — созданный визит.
     - `400 Bad Request`, `404 Not Found` или `500 Internal Server Error` при ошибках.
10. **POST `/entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=false`** — создаёт виртуальный визит без печати талона.
    - Контроллер: `EntrypointController#createVisitFromReception` 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L302-L372】
    - Ответы:
      - `200 OK` — созданный визит.
      - `400 Bad Request`, `404 Not Found` или `500 Internal Server Error` при ошибках.
11. **PUT `/servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=true`** — переносит визит внешней службой в конец очереди.
    - Контроллер: `ServicePointController#visitTransferFromQueue` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2724-L2764】
    - Ответы:
      - `200 OK` — обновлённый визит.
      - `404 Not Found` или `500 Internal Server Error` при ошибках.
12. **PUT `/servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=false`** — переносит визит внешней службой в начало очереди.
    - Контроллер: `ServicePointController#visitTransferFromQueue` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2724-L2764】
    - Ответы:
      - `200 OK` — обновлённый визит.
      - `404 Not Found` или `500 Internal Server Error` при ошибках.
13. **PUT `/servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=true`** — добавляет визит в конец пула выбранной точки обслуживания.
    - Контроллер: `ServicePointController#visitTransferFromQueueToServicePointPool` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2994-L3034】
    - Ответы:
      - `200 OK` — визит в пуле.
      - `404 Not Found` или `500 Internal Server Error` при ошибках.
14. **PUT `/servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=false`** — помещает визит в начало пула точки обслуживания.
    - Контроллер: `ServicePointController#visitTransferFromQueueToServicePointPool` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2994-L3034】
    - Ответы:
      - `200 OK` — визит в пуле.
      - `404 Not Found` или `500 Internal Server Error` при ошибках.
15. **PUT `/servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=true`** — переносит визит в конец пула сотрудника.
    - Контроллер: `ServicePointController#visitTransferFromQueueToUserPool` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L3295-L3321】
    - Ответы:
      - `200 OK` — визит в пуле сотрудника.
      - `404 Not Found` или `500 Internal Server Error` при ошибках.
16. **PUT `/servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=false`** — переносит визит в начало пула сотрудника.
    - Контроллер: `ServicePointController#visitTransferFromQueueToUserPool` 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L3295-L3321】
    - Ответы:
      - `200 OK` — визит в пуле сотрудника.
      - `404 Not Found` или `500 Internal Server Error` при ошибках.





## 📦 Примеры кода

### Использование сервиса
```java
import jakarta.inject.Inject;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.VisitService;

class VisitFacade {

    @Inject
    VisitService visitService;

    Visit load(String branchId, String visitId) {
        return visitService.getVisit(branchId, visitId);
    }

    void disableAutoCall(String branchId, String servicePointId) {
        visitService.cancelAutoCallModeOfServicePoint(branchId, servicePointId);
    }
}
```

### REST‑клиент Micronaut
Зависимость: `io.micronaut:micronaut-http-client`
```java
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import java.util.List;
import ru.aritmos.model.visit.Visit;

@Client("/")
interface VisitClient {
    @Post("/entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit")
    Visit create(String branchId, String entryPointId, List<String> services);
}
```
```java
import jakarta.inject.Inject;
import java.util.List;
import ru.aritmos.clients.VisitClient;
import ru.aritmos.model.visit.Visit;

class VisitCreator {

    @Inject
    VisitClient visitClient;

    Visit createVisit() {
        return visitClient.create("001", "01", List.of("serviceId1"));
    }
}
```

### Работа с `HttpClient`
```java
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;


@Slf4j
class HttpExample {

    void fetchBranches() throws MalformedURLException {
        try (HttpClient client = HttpClient.create(new URL("http://localhost:8080"))) {
            BlockingHttpClient blocking = client.toBlocking();
            HttpRequest<?> req = HttpRequest.GET("/managementinformation/branches");
            String body = blocking.retrieve(req);
            log.info("Ответ: {}", body);
        }
    }
}
```

## 📊 Диаграммы

Диаграммы доступны в формате SVG (исходники — в `docs/diagrams/*.puml`):

![Кейсы использования](docs/diagrams/use-cases.svg)
![Архитектура](docs/diagrams/architecture.svg)
![Последовательность: создание визита](docs/diagrams/sequence-create-visit.svg)
![Последовательность: завершение визита](docs/diagrams/sequence-update-visit.svg)

Дополнительные диаграммы и анализ сценариев см. в [docs/use-cases.md](docs/use-cases.md).

## 🧑‍💼 Сценарии работы сотрудника

![Рабочий процесс сотрудника](docs/diagrams/employee-workflow.svg)

### 1. Открытие рабочего места
1. `GET /managementinformation/branches/tiny` — пульт загружает компактный список отделений (`200 OK` + массив `{ id, name }`; `400/500` при ошибках). 【F:src/main/java/ru/aritmos/api/ManagementController.java†L127-L142】
2. `GET /managementinformation/branches/{branchId}` — актуализирует состояние выбранного отделения перед началом смены (`200 OK`; `404/500` при проблемах). 【F:src/main/java/ru/aritmos/api/ManagementController.java†L65-L71】
3. `GET /servicepoint/branches/{branchId}/servicePoints` — показывает доступные рабочие места и их статус (`200 OK`; `404/500` при ошибках). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L168-L193】
4. `GET /servicepoint/branches/{branchId}/users` — отображает сотрудников отделения и их занятость (`200 OK`; `404/500`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L268-L291】
5. `GET /servicepoint/branches/{branchId}/printers` — перечень принтеров для выбора устройства печати (`200 OK`; `404/500`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L102-L117】
6. `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/workProfiles/{workProfileId}/users/{userName}/open` — оператор занимает точку обслуживания (`200 OK`; `404/409` при конфликтах). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L492-L511】


### 2. Вызов визита
1. `GET /servicepoint/branches/{branchId}/queues/full` — обновляет состояние очередей в дашборде (`200 OK`; `404/500` при сбое). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L150-L165】
2. `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/call` — запрос следующего визита (`200 OK` + JSON талона, `204 No Content` если очередь пуста, `207 Multi-Status` при активном авто-вызове). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L973-L982】


### 3. Начало обслуживания визита
1. `GET /entrypoint/branches/{branchId}/services/all` — обновляет полный список услуг и их параметров перед началом обслуживания (`200 OK`; `404/500` при ошибках). 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L442-L457】
2. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/confirm/{visitId}` — подтверждение прихода клиента (`200 OK`; `404 Not Found`; `409 Conflict`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L1386-L1434】
3. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredservice/{deliveredServiceId}` — добавление фактической услуги. 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L1700-L1741】
4. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/deliveredService/{deliveredServiceId}/outcome/{outcomeId}` — фиксация исхода фактической услуги. 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2055-L2093】
5. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/outcome/{outcomeId}` — итог обслуженной услуги. 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L1939-L1969】


### 4. Перевод и возвращение визита
1. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromServicePoint?isAppend=true` — перевод визита в другую очередь (`200 OK`; `404/500`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2366-L2414】
2. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/transfer` — отправка визита в пул точки обслуживания. 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2468-L2517】
3. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/put_back` — возврат визита из пула. 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2418-L2465】
4. `PUT /servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=false|true` — внешняя служба (ресепшен, MI) меняет позицию визита в очереди, передавая `serviceInfo` и `transferTimeDelay` (`200 OK`; `404/500`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2724-L2764】
5. `PUT /servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=false|true` — перевод визита во внешний пул точки обслуживания с учетом `serviceInfo` и `sid`. 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2994-L3034】
6. `PUT /servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=false|true` — помещение визита во внешний пул конкретного сотрудника (`200 OK`; `404/500`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L3295-L3321】


### 5. Повторный вызов визита
1. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/recall/{visitId}` — повторное приглашение клиента (`ServicePointController#visitReCallForConfirm`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L1292-L1360】

### 6. Завершение визита
1. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end?isForced=false` — фиксация завершения обслуживания (`ServicePointController#visitEnd`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L3138-L3163】

### 7. Закрытие рабочего места
- Перерыв: `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/close?isBreak=true&breakReason=...` (метод `ServicePointController#closeServicePoint`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L540-L551】
- Выход из системы: `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/logout` (метод `ServicePointController#logoutUser`). 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L560-L592】

## 🤖 Документация для автотестеров

### Файлы настроек
- `src/test/resources/application.yml` — базовая конфигурация Micronaut для тестов;
- `src/test/resources/application-test.yml` — профиль `test` для автотестов;
- `src/test/resources/application-dev.yml` — конфигурация dev при локальном запуске;
- `src/test/resources/logback-test.xml` — настройки логирования в тестах;
- `src/test/resources/loki.properties` — параметры отправки логов;
- `src/test/resources/keycloak.json` — конфигурация Keycloak для тестов.

### Кейсы клиентов

- **Создание визита**
  - Запрос: `POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit`.
  - Описание: клиент регистрирует талон на выбранные услуги через пульт или терминал.
  - Ответы:
    - `200 OK` — JSON визита.
    - `400 Bad Request` — некорректное тело запроса (например, пустой набор услуг).
    - `404 Not Found` — отделение, точка создания визита, услуга или очередь не найдены.
    - `500 Internal Server Error` — ошибка сервера.
- **Визит на несколько услуг**
  - Запрос: `POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit`.
  - Описание: в теле запроса передаются несколько идентификаторов услуг — `["serviceId1","serviceId2"]`.
  - Ответы:
    - `200 OK` — JSON визита со списком услуг.
    - `400 Bad Request` — некорректные данные запроса.
    - `404 Not Found` — отделение, точка создания визита, услуга или очередь не найдены.
    - `500 Internal Server Error` — ошибка сервера.
- **Визит с параметрами**
  - Запрос: `POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visitWithParameters`.
  - Описание: создание талона с передачей объекта `{ "serviceIds": [], "parameters": {} }`.
  - Ответы:
    - `200 OK` — JSON визита с указанными параметрами.
    - `400 Bad Request` — некорректные данные или пустой список услуг.
    - `404 Not Found` — отделение, точка создания визита, услуга, очередь или правило сегментации не найдены, либо отсутствуют данные для выбранного правила сегментации.
    - `500 Internal Server Error` — ошибка сервера.
- **Печать талона**
  - Запрос: `POST /entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=true`.
  - Описание: регистрация визита с одновременной печатью талона на выбранном принтере.
  - Ответы:
    - `200 OK` — JSON визита, талон отправлен на принтер.
    - `400 Bad Request` — некорректные данные (например, пустой список услуг).
    - `404 Not Found` — отделение, услуга, очередь или правило сегментации не найдены, либо отсутствуют данные для выбранного правила сегментации.
    - `500 Internal Server Error` — ошибка сервера.
- **Отмена визита**
  - Запрос: `DELETE /servicepoint/branches/{branchId}/visits/{visitId}`.
  - Описание: клиент или оператор удаляет талон по идентификатору.
  - Ответы:
    - `204 No Content` — визит удалён.
    - `404 Not Found` — визит не найден.
    - `409 Conflict` — нарушено бизнес-ограничение.
    - `500 Internal Server Error` — ошибка сервера.
- **Статус визита**
  - Запрос: `GET /servicepoint/branches/{branchId}/visits/{visitId}`.
  - Описание: запрос актуального состояния визита по идентификатору.
  - Ответы:
    - `200 OK` — JSON визита.
    - `404 Not Found` — отделение или визит не найдены.
    - `500 Internal Server Error` — ошибка сервера.
- **Невалидная услуга**
  - Запрос: `POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit`.
  - Описание: в теле указана услуга, отсутствующая в конфигурации отделения.
  - Ответы:
    - `404 Not Found` — услуга не найдена.
    - `400 Bad Request` — некорректное тело запроса.
    - `500 Internal Server Error` — ошибка сервера.
- **Пустой список услуг**
  - Запрос: `POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit`.
  - Описание: передан пустой массив услуг `[]`.
  - Ответы:
    - `400 Bad Request` — необходимо указать хотя бы одну услугу.
    - `404 Not Found` — отделение, точка создания визита, услуга или очередь не найдены.
    - `500 Internal Server Error` — ошибка сервера.
- **Отмена чужого визита**
  - Запрос: `DELETE /servicepoint/branches/{branchId}/visits/{visitId}`.
  - Описание: попытка удалить визит, который закреплён за другим пользователем.
  - Ответы:
    - `409 Conflict` — визит нельзя удалить (нарушено бизнес-ограничение, визит закреплён за другим оператором).
    - `404 Not Found` — визит не найден.
    - `500 Internal Server Error` — ошибка сервера.


### Кейсы операторов

Ниже приведены проверки интерфейса оператора. В описании каждого кейса указано, какой контроллер Micronaut обрабатывает соответствующий REST-запрос.


- **Выбор отделения** (`ManagementController#getTinyBranches`)
  - Запрос: `GET /managementinformation/branches/tiny`.
  - Описание: загружает компактный список отделений при входе оператора.
  - Ответы:
    - `200 OK` — JSON с полями `id`, `name`.
    - `400 Bad Request` — неверные параметры фильтрации.
    - `500 Internal Server Error` — сбой сервера.
- **Загрузка рабочих мест** (`ServicePointController#getServicePoints`)
  - Запрос: `GET /servicepoint/branches/{branchId}/servicePoints`.
  - Описание: возвращает точки обслуживания отделения и их доступность.
  - Ответы:
    - `200 OK` — список объектов с признаками занятости.
    - `404 Not Found` — отделение отсутствует.
    - `500 Internal Server Error` — ошибка сервиса.
- **Загрузка сотрудников** (`ServicePointController#getUsersOfBranch`)
  - Запрос: `GET /servicepoint/branches/{branchId}/users`.
  - Описание: показывает сотрудников отделения и их текущий статус.
  - Ответы:
    - `200 OK` — массив пользователей.
    - `404 Not Found` — отделение не найдено.
    - `500 Internal Server Error` — ошибка сервера.
- **Каталог услуг** (`EntrypointController#getAllServices`)
  - Запрос: `GET /entrypoint/branches/{branchId}/services/all`.
  - Описание: загружает полный перечень услуг и их параметров.
  - Ответы:
    - `200 OK` — массив услуг.
    - `404 Not Found` — отделение недоступно.
    - `500 Internal Server Error` — ошибка интеграций.
- **Картина очередей** (`ServicePointController#getFullQueues`)
  - Запрос: `GET /servicepoint/branches/{branchId}/queues/full`.
  - Описание: отображает очереди отделения, их талоны и метрики.
  - Ответы:
    - `200 OK` — список очередей с визитами.
    - `404 Not Found` — отделение отсутствует.
    - `500 Internal Server Error` — ошибка сервера.
- **Аннулирование визита** (`ServicePointController#deleteVisit`)
  - Запрос: `DELETE /servicepoint/branches/{branchId}/visits/{visitId}`.
  - Описание: удаляет талон по идентификатору в интерфейсе оператора.
  - Ответы:
    - `204 No Content` — визит удалён.
    - `404 Not Found` — визит не найден.
    - `409 Conflict` — нарушено бизнес-правило.
    - `500 Internal Server Error` — ошибка сервера.
- **Список принтеров** (`ServicePointController#getPrinters`)
  - Запрос: `GET /servicepoint/branches/{branchId}/printers`.
  - Описание: показывает доступные устройства печати талонов.
  - Ответы:
    - `200 OK` — перечень принтеров.
    - `404 Not Found` — отделение отсутствует.
    - `500 Internal Server Error` — ошибка сервиса.
- **Сведения об отделении** (`ManagementController#getBranch`)
  - Запрос: `GET /servicepoint/branches/{branchId}`.
  - Описание: возвращает агрегированное состояние отделения (делегирует `ManagementController#getBranch`).
  - Ответы:
    - `200 OK` — JSON отделения.
    - `404 Not Found` — отделение отсутствует.
    - `500 Internal Server Error` — ошибка сервера.
- **Создание визита с печатью** (`EntrypointController#createVisitFromReception`)
  - Запрос: `POST /entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=true`.
  - Описание: создаёт визит из приёмной и печатает талон на выбранном устройстве.
  - Ответы:
    - `200 OK` — JSON визита и подтверждение печати.
    - `400 Bad Request` — некорректные параметры.
    - `404 Not Found` — отделение, услуга или принтер отсутствуют.
    - `500 Internal Server Error` — ошибка бизнес-логики.
- **Создание визита без печати** (`EntrypointController#createVisitFromReception`)
  - Запрос: `POST /entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=false`.
  - Описание: создаёт виртуальный визит без печати талона.
  - Ответы:
    - `200 OK` — JSON визита.
    - `400 Bad Request` — неверные параметры запроса.
    - `404 Not Found` — связанные сущности не найдены.
    - `500 Internal Server Error` — ошибка бизнес-логики.
- **Перевод визита в конец очереди** (`ServicePointController#visitTransferFromQueue`)
  - Запрос: `PUT /servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=true`.
  - Описание: внешняя служба ставит визит в конец очереди, передавая `serviceInfo` и задержку.
  - Ответы:
    - `200 OK` — визит обновлён.
    - `404 Not Found` — отделение, очередь или визит отсутствуют.
    - `500 Internal Server Error` — ошибка обработки.
- **Перевод визита в начало очереди** (`ServicePointController#visitTransferFromQueue`)
  - Запрос: `PUT /servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=false`.
  - Описание: перемещает визит в начало очереди с помощью внешней службы.
  - Ответы:
    - `200 OK` — визит обновлён.
    - `404 Not Found` — отделение, очередь или визит отсутствуют.
    - `500 Internal Server Error` — ошибка обработки.
- **Внешний пул точки обслуживания (конец)** (`ServicePointController#visitTransferFromQueueToServicePointPool`)
  - Запрос: `PUT /servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=true`.
  - Описание: переносит визит во внешний пул точки обслуживания в конец списка.
  - Ответы:
    - `200 OK` — визит добавлен в пул.
    - `404 Not Found` — отделение, точка или визит отсутствуют.
    - `500 Internal Server Error` — ошибка сервера.
- **Внешний пул точки обслуживания (начало)** (`ServicePointController#visitTransferFromQueueToServicePointPool`)
  - Запрос: `PUT /servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=false`.
  - Описание: помещает визит в начало внешнего пула точки обслуживания.
  - Ответы:
    - `200 OK` — визит добавлен в пул.
    - `404 Not Found` — отделение, точка или визит отсутствуют.
    - `500 Internal Server Error` — ошибка сервера.
- **Внешний пул сотрудника (конец)** (`ServicePointController#visitTransferFromQueueToUserPool`)
  - Запрос: `PUT /servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=true`.
  - Описание: ставит визит в конец внешнего пула выбранного сотрудника.
  - Ответы:
    - `200 OK` — визит закреплён за пулом.
    - `404 Not Found` — отделение, сотрудник или визит отсутствуют.
    - `500 Internal Server Error` — ошибка обработки.
- **Внешний пул сотрудника (начало)** (`ServicePointController#visitTransferFromQueueToUserPool`)
  - Запрос: `PUT /servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=false`.
  - Описание: помещает визит в начало внешнего пула сотрудника.
  - Ответы:
    - `200 OK` — визит закреплён за пулом.
    - `404 Not Found` — отделение, сотрудник или визит отсутствуют.
    - `500 Internal Server Error` — ошибка обработки.


### Кейсы аутентификации

- **Валидный токен**
  - Запрос: любой защищённый REST-эндпоинт.
  - Описание: запрос с корректным заголовком `Authorization: Bearer <token>`.
  - Ответы:
    - `200 OK`.
- **Просроченный токен**
  - Запрос: любой защищённый REST-эндпоинт.
  - Описание: используется токен с истёкшим сроком действия.
  - Ответы:
    - `401 Unauthorized`.
- **Отсутствие токена**
  - Запрос: любой REST-эндпоинт без заголовка `Authorization`.
  - Описание: отправка запроса без передачи bearer-токена.
  - Ответы:
    - `401 Unauthorized`.
- **Недостаточно прав**
  - Запрос: любой защищённый REST-эндпоинт.
  - Описание: токен не содержит требуемой роли или scope.
  - Ответы:
    - `403 Forbidden`.
- **Неверная подпись**
  - Запрос: любой защищённый REST-эндпоинт.
  - Описание: токен модифицирован, подпись не совпадает с ключом провайдера.
  - Ответы:
    - `401 Unauthorized`.
- **Отозванный токен**
  - Запрос: любой защищённый REST-эндпоинт.
  - Описание: используется токен, отозванный сервером аутентификации.
  - Ответы:
    - `401 Unauthorized`.
- **Неизвестный издатель**
  - Запрос: любой защищённый REST-эндпоинт.
  - Описание: токен выпущен другим Identity Provider и не доверен системе.
  - Ответы:
    - `401 Unauthorized`.


Подробности сценариев см. в [docs/use-cases.md](docs/use-cases.md).

## 🧪 Тестирование
Всего модульных и интеграционных тестов: 351 (`mvn test`). Линейное покрытие по JaCoCo — 43,8%.
```bash
JAVA_TOOL_OPTIONS='-Djava.net.preferIPv4Stack=true' mvn -s .mvn/settings.xml test
```

### Модульные тесты

Ниже перечислены модульные проверки. Каждый тест изолирует компонент, подменяя внешние зависимости моками и проверяя поведение класса.

#### Ядро приложения
- ru.aritmos.ApplicationConfigurerTest — убеждается, что `Application.Configurer` включает профиль `dev`, проверяя вызов `defaultEnvironments` на мок‑объекте `ApplicationContextBuilder`.
- ru.aritmos.ApplicationTest — гарантирует запуск приложения и доступ к конфигурации, перехватывая вызов `Micronaut.run` и проверяя отсутствие исключений при `getConfiguration`.
- ru.aritmos.EntrypointTest — валидирует выбор визита Groovy‑скриптом, формируя тестовый список визитов и исполняя скрипт через `GroovyShell`.
- ru.aritmos.GroovyTest — демонстрирует работу Groovy‑скрипта, проверяя, что выборка визита происходит по заданным параметрам.

#### API контроллеры
- ru.aritmos.api.ConfigurationControllerTest — покрывает REST получения конфигурации, вызывая контроллер через встроенный HTTP‑клиент и сравнивая JSON‑ответ.
- ru.aritmos.api.EntrypointControllerTest — покрывает создание визитов (виртуальные, стандартные и из приёмной), проверяя передачу параметров и работу сегментации.
- ru.aritmos.api.HttpErrorHandlerTest — подтверждает формирование унифицированного тела ответа при обработке `HttpStatusException`.
- ru.aritmos.api.KeyCloakControllerTest — тестирует ручки аутентификации Keycloak с использованием заглушек OAuth‑клиента.
- ru.aritmos.api.ManagementControllerTest — проверяет административные операции, отправляя запросы к управленческим эндпоинтам.
- ru.aritmos.api.ServicePointControllerTest — моделирует сценарии обслуживания: поиск визита в очереди, вызовы по списку очередей, подтверждение/отмену визитов и управление режимом автозапуска точек.

#### Заглушки и утилиты
- ru.aritmos.config.LocalNoDockerDataBusClientStubTest — убеждается в работе заглушки DataBus для режима без Docker.
- ru.aritmos.config.LocalNoDockerKeycloakStubTest — проверяет, что заглушка клиента Keycloak возвращает фиктивные данные.
- ru.aritmos.docs.CurlCheatsheetGeneratorTest — генерирует подсказку по `curl`, проверяя создание документа из OpenAPI‑описания.

#### Внешние клиенты
- ru.aritmos.clients.ConfigurationClientTest — проверяет Micronaut‑клиент конфигурации и наличие HTTP‑аннотаций.
- ru.aritmos.clients.PrinterClientTest — подтверждает настройки повторных попыток и поток исполнения HTTP‑клиента печати.
- ru.aritmos.events.clients.DataBusClientTest — убеждается, что клиент отправки событий содержит ожидаемые HTTP‑, retry- и асинхронные аннотации.

#### События и обработчики
- ru.aritmos.events.model.ChangedObjectTest — проверяет модель изменённого объекта, создавая экземпляры и сравнивая поля.
- ru.aritmos.events.model.EventHandlerTest — гарантирует наличие ожидаемых аннотаций и объявленных исключений у метода `Handle`.
- ru.aritmos.events.model.EventTest — валидирует сериализацию и параметры событий, сравнивая JSON и типы.
- ru.aritmos.events.services.DelayedEventsTest — тестирует логику отложенной отправки, используя фиктивный планировщик задач.
- ru.aritmos.events.services.EventServiceTest — проверяет построение событий и взаимодействие с мокированным `DataBusClient`.
- ru.aritmos.events.services.EventTaskTest — проверяет выполнение задач обработки события через моки исполнителей.
- ru.aritmos.events.services.KafkaListenerTest — валидирует регистрацию обработчиков и десериализацию событий слушателем Kafka.
- ru.aritmos.events.services.MultiserviceEventTaskTest — убеждается, что многосервисные задачи выполняются последовательно.
- ru.aritmos.handlers.EventHandlerContextTest — тестирует регистрацию обработчиков событий и их поиск в контексте.

#### Исключения и безопасность
- ru.aritmos.exceptions.BusinessExceptionTest — проверяет генерацию и сообщение бизнес‑исключений.
- ru.aritmos.exceptions.SystemExceptionTest — аналогично тестирует системные исключения.
- ru.aritmos.keycloack.customsecurity.CustomSecurityRuleTest — валидирует пользовательское правило безопасности Micronaut.
- ru.aritmos.keycloack.service.EndSessionEndpointResolverReplacementTest — проверяет резолвер завершения сессии Keycloak.
- ru.aritmos.keycloack.service.KeyCloackClientTest — тестирует клиент Keycloak, подменяя внешние вызовы моками.
- ru.aritmos.keycloack.service.UserMapperTest — проверяет маппинг данных пользователя между Keycloak и доменной моделью.

#### Доменные модели
- ru.aritmos.model.BasedServiceTest — проверяет базовую услугу и её поля.
- ru.aritmos.model.BranchEntityTest — валидирует сущность отделения, включая список услуг.
- ru.aritmos.model.BranchEntityWithVisitsTest — тестирует отделение с вложенными визитами.
- ru.aritmos.model.BranchTest — проверяет доменную модель отделения и закрытие точки обслуживания.
- ru.aritmos.model.DeliveredServiceTest — удостоверяется в корректности модели выполненной услуги визита.
- ru.aritmos.model.EntityTest — проверяет билдер, геттеры/сеттеры и аннотацию `@Serdeable` базовой сущности.
- ru.aritmos.model.OutcomeTest — проверяет перечисление исходов обслуживания.
- ru.aritmos.model.RealmAccessTest — проверяет расширенные права доступа пользователя, включая ветки, группы и модули.
- ru.aritmos.model.QueueTest — проверяет модель очереди и её конструкторы.
- ru.aritmos.model.ServiceTest — валидирует модель услуги и её атрибуты.
- ru.aritmos.model.ServicePointTest — проверяет конструкторы точки обслуживания и значения по умолчанию.
- ru.aritmos.model.TokenTest — подтверждает корректность хранения атрибутов токена авторизации.
- ru.aritmos.model.UserTest — проверяет пользователя и его идентификаторы.
- ru.aritmos.model.UserInfoTest — убеждается, что данные пользователя переносятся через билдер и сеттеры без потерь.
- ru.aritmos.model.UserTokenTest — проверяет агрегированный объект пользователя и связанных токенов.

- ru.aritmos.model.EntryPointTest — проверяет наследование полей точки входа и привязку принтера.
- ru.aritmos.model.GroovyScriptTest — удостоверяется, что модель скрипта создаёт карты параметров и снабжена аннотациями сериализации.
- ru.aritmos.model.MarkTest — тестирует модель пометки визита и обновление полей.
- ru.aritmos.model.ReceptionTest — проверяет построение приёмной с перечнем принтеров и сессий.
- ru.aritmos.model.ReceptionSessionTest — убеждается, что билдер сессии приёмной задаёт пользователя и временные метки.
- ru.aritmos.model.SegmentationRuleDataTest — валидирует билдер данных правила сегментации и возможность задавать свойства позже.
- ru.aritmos.model.ServiceGroupTest — подтверждает работу конструктора группы услуг и хранение идентификаторов сегментации.
- ru.aritmos.model.UserSessionTest — валидирует модель пользовательской сессии VisitManager и работу билдера и сеттеров.
- ru.aritmos.model.VisitParametersTest — проверяет, что билдер параметров визита создаёт пустые коллекции и принимает пользовательские значения.
- ru.aritmos.model.WorkProfileTest — тестирует конструкторы рабочего профиля и редактирование списка очередей.

- ru.aritmos.model.keycloak.ModuleRoleAccessTest — проверяет доступ к функциям по ролям Keycloak.
- ru.aritmos.model.keycloak.ModuleRoleTest — тестирует модель роли модуля.
- ru.aritmos.model.keycloak.ClientAccessTest — убеждается, что карта ролей клиента читается билдерами и сеттерами.
- ru.aritmos.model.keycloak.RealmAccessTest — проверяет хранение перечня ролей в realm.
- ru.aritmos.model.keycloak.TokenTest — подтверждает, что поля токена Keycloak не теряются при маппинге.
- ru.aritmos.model.keycloak.UserInfoTest — проверяет перенос данных пользователя из Keycloak.
- ru.aritmos.model.keycloak.UserSessionTest — тестирует контейнер пользовательской сессии Keycloak.
- ru.aritmos.model.keycloak.UserTokenTest — проверяет агрегацию сведений о пользователе и токенах.
- ru.aritmos.model.keycloak.TinyUserInfoTest — проверяет упрощённое представление данных пользователя.
- ru.aritmos.keycloack.model.CredentialsTest — подтверждает наличие геттеров/сеттеров в модели учётных данных.
- ru.aritmos.keycloack.model.KeyCloackUserTest — проверяет чтение атрибутов сокращённой модели пользователя Keycloak.

- ru.aritmos.model.tiny.TinyClassTest — проверяет билдер и сеттеры компактного представления сущности.
- ru.aritmos.model.tiny.TinyServicePointTest — убеждается, что сокращённая точка обслуживания хранит флаг доступности.

- ru.aritmos.model.tiny.TinyVisitTest — валидирует облегчённую модель визита.
- ru.aritmos.model.visit.TransactionCompletionStatusTest — проверяет полный набор статусов завершения обслуживания визита.
- ru.aritmos.model.visit.VisitEventInformationTest — проверяет сведения о событии визита.
- ru.aritmos.model.visit.VisitEventTest — тестирует сериализацию модели события.
- ru.aritmos.model.visit.VisitStateTest — валидирует перечисление состояний визита и аннотацию сериализации.
- ru.aritmos.model.visit.VisitTest — проверяет доменную модель визита и её билдер.

#### Сервисы
- ru.aritmos.service.BranchServiceTest — проверяет сервис работы с отделениями, включая получение пользователей и инкремент счётчика талонов.
- ru.aritmos.service.ConfigurationTest — тестирует сервис конфигурации приложения.
- ru.aritmos.service.GroovyScriptServiceTest — проверяет выполнение Groovy‑скриптов внутри сервиса.
- ru.aritmos.service.PrinterServiceTest — удостоверяется в корректности печати талонов через мок‑клиента.
- ru.aritmos.service.ServicesTest — проверяет доступ к справочнику услуг.
- ru.aritmos.service.VisitServiceAddEventTest — тестирует добавление события визита.
- ru.aritmos.service.VisitServiceAddServiceTest — проверяет добавление услуги в визит.
- ru.aritmos.service.VisitServiceAutoCallTest — проверяет автоматический вызов визита.
- ru.aritmos.service.VisitServiceDeliveredServicesTest — проверяет получение завершённых услуг визита.
- ru.aritmos.service.VisitServiceGetAllVisitsTest — проверяет получение всех визитов отделения.
- ru.aritmos.service.VisitServiceGetMarksTest — тестирует получение оценок визита.
- ru.aritmos.service.VisitServiceGetQueuesTest — проверяет выбор очередей отделения.
- ru.aritmos.service.VisitServiceGetVisitsTest — тестирует поиск визитов по параметрам.
- ru.aritmos.service.VisitServiceMarkModificationTest — проверяет изменение оценки визита.
- ru.aritmos.service.VisitServiceNoteTest — проверяет добавление заметок к визиту.
- ru.aritmos.service.VisitServiceOutcomeTest — тестирует установку исхода визита.
- ru.aritmos.service.VisitServiceTest — охватывает базовые операции `VisitService`.

#### Правила вызова
- ru.aritmos.service.rules.CallRuleTest — проверяет сигнатуры базового правила вызова и возвращаемые типы `Optional<Visit>`.
- ru.aritmos.service.rules.CustomCallRuleTest — проверяет пользовательское правило выбора визита.
- ru.aritmos.service.rules.MaxLifeTimeCallRuleTest — гарантирует, что правило ограничивает максимальную «жизнь» визита.
- ru.aritmos.service.rules.MaxWaitingTimeCallRuleTest — тестирует выбор визита по максимальному времени ожидания.
- ru.aritmos.service.rules.RuleTest — валидирует базовый контракт правил.
- ru.aritmos.service.rules.SegmentationRuleTest — проверяет правило сегментации очереди.
- ru.aritmos.service.rules.client.CallRuleClientTest — проверяет HTTP‑клиент вызова правила.

### Интеграционные тесты

Интеграционные сценарии поднимают полный контекст приложения и взаимодействуют с внешними сервисами (Kafka, Keycloak, DataBus) через Testcontainers и встроенный HTTP‑клиент.

- ru.aritmos.DataBusClientMockTest — проверяет заглушку клиента DataBus, обеспечивая совместимость событий.
- ru.aritmos.ExternalServicesIT — убеждается в доступности внешних сервисов и корректных настройках окружения.
- ru.aritmos.api.ConfigurationControllerE2EIT — сквозной тест получения конфигурации через HTTP‑запрос.
- ru.aritmos.api.EntrypointControllerE2EIT — сквозной сценарий создания визита.
- ru.aritmos.api.HttpErrorHandlerE2EIT — проверяет формирование унифицированного тела ошибки при обращении к REST.
- ru.aritmos.api.ManagementControllerE2EIT — проверяет полный цикл административных операций.
- ru.aritmos.api.ServicePointControllerE2EIT — сквозной тест точек обслуживания, включая вызов и завершение визита.
- ru.aritmos.api.VisitLifecycleE2EIT — воспроизводит полный жизненный цикл визита от создания до завершения.
- ru.aritmos.events.clients.DataBusClientIT — интеграция с DataBus, отправка и приём событий.
- ru.aritmos.events.services.DelayedEventsIT — проверяет работу отложенных событий вместе с инфраструктурой планировщика.
- ru.aritmos.integration.KeycloakKafkaIntegrationIT — запускает Keycloak и Kafka через Testcontainers и проверяет их взаимодействие.
- ru.aritmos.service.rules.MaxWaitingTimeCallRuleIT — интеграционно проверяет выбор визита с максимальным ожиданием.

Все тесты выполняются локально; при необходимости интеграций поднимите зависимые сервисы в Docker.


## 🌐 Переменные окружения
- `MICRONAUT_ENVIRONMENTS` — активный профиль (например, `local-no-docker`).
- `KAFKA_SERVER` — адрес брокера Kafka для Micronaut.
- `REDIS_SERVER` — URI Redis (например, `redis://host:port`).
- `DATABUS_SERVER`, `CONFIG_SERVER`, `PRINTER_SERVER` — базовые URL внешних HTTP-сервисов VisitManager.
- `LOKI_SERVER` — приёмник логов для Logback.
- `OIDC_ISSUER_URL`, `OIDC_ISSUER_DOMAIN` — настройки провайдера OpenID Connect/Keycloak.
- `OAUTH_CLIENT_ID`, `OAUTH_CLIENT_SECRET` — учётные данные OAuth2-клиента.
- `KEYCLOAK_TECHLOGIN`, `KEYCLOAK_TECHPASSWORD`, `KEYCLOAK_REALM` — техническая учётная запись Keycloak для административных операций.
- Дополнительно см. `application.yml`, профильные `application-*.yml` и файлы `.env.*`.

## 🔗 Полезные ссылки
- [Micronaut Documentation](https://docs.micronaut.io/latest/guide/)
- [Micronaut OpenAPI](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/)
- [Micronaut Security](https://micronaut-projects.github.io/micronaut-security/latest/guide/)

## 🤝 Contributing

Перед отправкой изменений выполните `mvn -s .mvn/settings.xml test` и придерживайтесь соглашения [Conventional Commits](https://www.conventionalcommits.org/). В pull request укажите, что изменено, как проверить изменения и возможные риски.

## 📄 Лицензия

Проект распространяется по простой (неисключительной) лицензии с оплатой вознаграждения по модели «Named User».
Размер роялти пропорционален количеству активных пользователей системы в отчётный период.
Расчёт ведётся в соответствии с методическими рекомендациями Минцифры России
по выбору моделей лицензирования отечественного ПО.
Подробные условия зафиксированы в лицензионном договоре.
