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

<table>
  <colgroup>
    <col style="width: 28%" />
    <col style="width: 72%" />
  </colgroup>
  <thead>
    <tr>
      <th>Код</th>
      <th>Описание</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>200 OK</code></td>
      <td>успешный запрос</td>
    </tr>
    <tr>
      <td><code>204 No Content</code></td>
      <td>успешный запрос без содержимого</td>
    </tr>
    <tr>
      <td><code>207 Multi-Status</code></td>
      <td>режим автоматического вызова уже активен или отключён</td>
    </tr>
    <tr>
      <td><code>400 Bad Request</code></td>
      <td>некорректный запрос</td>
    </tr>
    <tr>
      <td><code>401 Unauthorized</code></td>
      <td>не авторизован</td>
    </tr>
    <tr>
      <td><code>403 Forbidden</code></td>
      <td>доступ запрещён</td>
    </tr>
    <tr>
      <td><code>404 Not Found</code></td>
      <td>ресурс не найден</td>
    </tr>
    <tr>
      <td><code>405 Method Not Allowed</code></td>
      <td>HTTP-метод не поддерживается для ресурса</td>
    </tr>
    <tr>
      <td><code>409 Conflict</code></td>
      <td>конфликт данных или состояния</td>
    </tr>
    <tr>
      <td><code>415 Unsupported Media Type</code></td>
      <td>неподдерживаемый тип данных</td>
    </tr>
    <tr>
      <td><code>500 Internal Server Error</code></td>
      <td>ошибка сервера</td>
    </tr>
  </tbody>
</table>

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

## 🖥️ REST пульта рабочего места оператора

Ниже приведены REST-запросы, которые использует пульт рабочего места оператора. Для каждого запроса указан контроллер Micronaut, который его обрабатывает, и основные варианты ответов сервера.

<table>
  <colgroup>
    <col style="width: 4%" />
    <col style="width: 23%" />
    <col style="width: 24%" />
    <col style="width: 29%" />
    <col style="width: 20%" />
  </colgroup>
  <thead>
    <tr>
      <th>№</th>
      <th>Метод и путь</th>
      <th>Описание действия</th>
      <th>Ответы сервера</th>
      <th>Контроллер и метод</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>1</td>
      <td><code>GET /managementinformation/branches/tiny</code></td>
      <td>Компактный список отделений для выбора филиала.</td>
      <td>
        <div><code>200 OK</code> — JSON-массив <code>{ id, name }</code>.</div>
        <div><code>400 Bad Request</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ManagementController#getTinyBranches</code> 【F:src/main/java/ru/aritmos/api/ManagementController.java†L127-L142】</td>
    </tr>
    <tr>
      <td>2</td>
      <td><code>GET /servicepoint/branches/{branchId}/servicePoints</code></td>
      <td>Список точек обслуживания отделения для выбора рабочего места.</td>
      <td>
        <div><code>200 OK</code> — точки с признаком доступности.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#getServicePoints</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L168-L193】</td>
    </tr>
    <tr>
      <td>3</td>
      <td><code>GET /servicepoint/branches/{branchId}/users</code></td>
      <td>Возвращает сотрудников отделения и их статусы.</td>
      <td>
        <div><code>200 OK</code> — массив сотрудников.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#getUsersOfBranch</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L268-L291】</td>
    </tr>
    <tr>
      <td>4</td>
      <td><code>GET /entrypoint/branches/{branchId}/services/all</code></td>
      <td>Полный каталог услуг отделения для фильтров и карточек визита.</td>
      <td>
        <div><code>200 OK</code> — список услуг.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>EntrypointController#getAllServices</code> 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L442-L457】</td>
    </tr>
    <tr>
      <td>5</td>
      <td><code>GET /servicepoint/branches/{branchId}/queues/full</code></td>
      <td>Подробные данные по очередям отделения.</td>
      <td>
        <div><code>200 OK</code> — очереди и талоны.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#getFullQueues</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L150-L165】</td>
    </tr>
    <tr>
      <td>6</td>
      <td><code>DELETE /servicepoint/branches/{branchId}/visits/{visitId}</code></td>
      <td>Отменяет визит по идентификатору.</td>
      <td>
        <div><code>204 No Content</code> — успешная отмена.</div>
        <div><code>404 Not Found</code>, <code>409 Conflict</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#deleteVisit</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2334-L2363】</td>
    </tr>
    <tr>
      <td>7</td>
      <td><code>GET /servicepoint/branches/{branchId}/printers</code></td>
      <td>Список доступных принтеров отделения.</td>
      <td>
        <div><code>200 OK</code> — перечень устройств.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#getPrinters</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L102-L117】</td>
    </tr>
    <tr>
      <td>8</td>
      <td><code>GET /servicepoint/branches/{branchId}</code></td>
      <td>Возвращает сводные данные отделения (делегирует <code>ManagementController#getBranch</code>).</td>
      <td>
        <div><code>200 OK</code> — JSON отделения.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ManagementController#getBranch</code> 【F:src/main/java/ru/aritmos/api/ManagementController.java†L65-L71】</td>
    </tr>
    <tr>
      <td>9</td>
      <td><code>POST /entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=true</code></td>
      <td>Создаёт визит с печатью талона на выбранном принтере.</td>
      <td>
        <div><code>200 OK</code> — созданный визит.</div>
        <div><code>400 Bad Request</code>, <code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>EntrypointController#createVisitFromReception</code> 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L302-L372】</td>
    </tr>
    <tr>
      <td>10</td>
      <td><code>POST /entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=false</code></td>
      <td>Создаёт виртуальный визит без печати талона.</td>
      <td>
        <div><code>200 OK</code> — созданный визит.</div>
        <div><code>400 Bad Request</code>, <code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>EntrypointController#createVisitFromReception</code> 【F:src/main/java/ru/aritmos/api/EntrypointController.java†L302-L372】</td>
    </tr>
    <tr>
      <td>11</td>
      <td><code>PUT /servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=true</code></td>
      <td>Переносит визит внешней службой в конец очереди.</td>
      <td>
        <div><code>200 OK</code> — обновлённый визит.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#visitTransferFromQueue</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2724-L2764】</td>
    </tr>
    <tr>
      <td>12</td>
      <td><code>PUT /servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=false</code></td>
      <td>Переносит визит внешней службой в начало очереди.</td>
      <td>
        <div><code>200 OK</code> — обновлённый визит.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#visitTransferFromQueue</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2724-L2764】</td>
    </tr>
    <tr>
      <td>13</td>
      <td><code>PUT /servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=true</code></td>
      <td>Добавляет визит в конец пула выбранной точки обслуживания.</td>
      <td>
        <div><code>200 OK</code> — визит в пуле.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#visitTransferFromQueueToServicePointPool</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2994-L3034】</td>
    </tr>
    <tr>
      <td>14</td>
      <td><code>PUT /servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=false</code></td>
      <td>Помещает визит в начало пула точки обслуживания.</td>
      <td>
        <div><code>200 OK</code> — визит в пуле.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#visitTransferFromQueueToServicePointPool</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L2994-L3034】</td>
    </tr>
    <tr>
      <td>15</td>
      <td><code>PUT /servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=true</code></td>
      <td>Переносит визит в конец пула сотрудника.</td>
      <td>
        <div><code>200 OK</code> — визит в пуле сотрудника.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#visitTransferFromQueueToUserPool</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L3295-L3321】</td>
    </tr>
    <tr>
      <td>16</td>
      <td><code>PUT /servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=false</code></td>
      <td>Переносит визит в начало пула сотрудника.</td>
      <td>
        <div><code>200 OK</code> — визит в пуле сотрудника.</div>
        <div><code>404 Not Found</code> или <code>500 Internal Server Error</code> при ошибках.</div>
      </td>
      <td><code>ServicePointController#visitTransferFromQueueToUserPool</code> 【F:src/main/java/ru/aritmos/api/ServicePointController.java†L3295-L3321】</td>
    </tr>
  </tbody>
</table>

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

<table>
  <colgroup>
    <col style="width: 20%" />
    <col style="width: 30%" />
    <col style="width: 30%" />
    <col style="width: 20%" />
  </colgroup>
  <thead>
    <tr>
      <th>Кейс</th>
      <th>REST-запрос</th>
      <th>Описание</th>
      <th>Ожидаемый ответ</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Создание визита</td>
      <td><code>POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit</code></td>
      <td>Клиент регистрирует талон на выбранные услуги через пульт или терминал.</td>
      <td>
        <div><code>200 OK</code> — JSON визита.</div>
      </td>
    </tr>
    <tr>
      <td>Визит на несколько услуг</td>
      <td><code>POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit</code></td>
      <td>В теле запроса передаются несколько идентификаторов услуг: <code>["serviceId1","serviceId2"]</code>.</td>
      <td>
        <div><code>200 OK</code> — JSON визита со списком услуг.</div>
      </td>
    </tr>
    <tr>
      <td>Визит с параметрами</td>
      <td><code>POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visitWithParameters</code></td>
      <td>Создание талона с передачей объекта <code>{ "serviceIds": [], "parameters": {} }</code>.</td>
      <td>
        <div><code>200 OK</code> — JSON визита с указанными параметрами.</div>
      </td>
    </tr>
    <tr>
      <td>Печать талона</td>
      <td><code>POST /entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=true</code></td>
      <td>Регистрация визита с одновременной печатью талона на выбранном принтере.</td>
      <td>
        <div><code>200 OK</code> — JSON визита, талон отправлен на принтер.</div>
      </td>
    </tr>
    <tr>
      <td>Отмена визита</td>
      <td><code>DELETE /servicepoint/branches/{branchId}/visits/{visitId}</code></td>
      <td>Клиент или оператор удаляет талон по идентификатору.</td>
      <td>
        <div><code>204 No Content</code> — визит удалён.</div>
        <div><code>404 Not Found</code> — визит не найден.</div>
        <div><code>409 Conflict</code> — нарушено бизнес-ограничение.</div>
      </td>
    </tr>
    <tr>
      <td>Статус визита</td>
      <td><code>GET /servicepoint/branches/{branchId}/visits/{visitId}</code></td>
      <td>Запрос актуального состояния визита по идентификатору.</td>
      <td>
        <div><code>200 OK</code> — JSON визита.</div>
      </td>
    </tr>
    <tr>
      <td>Невалидная услуга</td>
      <td><code>POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit</code></td>
      <td>В теле указана услуга, отсутствующая в конфигурации отделения.</td>
      <td>
        <div><code>404 Not Found</code> — услуга не найдена.</div>
      </td>
    </tr>
    <tr>
      <td>Пустой список услуг</td>
      <td><code>POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit</code></td>
      <td>Передан пустой массив услуг: <code>[]</code>.</td>
      <td>
        <div><code>400 Bad Request</code> — необходимо указать услугу.</div>
      </td>
    </tr>
    <tr>
      <td>Отмена чужого визита</td>
      <td><code>DELETE /servicepoint/branches/{branchId}/visits/{visitId}</code></td>
      <td>Попытка удалить визит, который закреплён за другим пользователем.</td>
      <td>
        <div><code>403 Forbidden</code> — нет прав на удаление.</div>
      </td>
    </tr>
  </tbody>
</table>

### Кейсы операторов

Ниже приведены проверки интерфейса оператора. В описании каждого кейса указано, какой контроллер Micronaut обрабатывает соответствующий REST-запрос.

<table>
  <colgroup>
    <col style="width: 20%" />
    <col style="width: 30%" />
    <col style="width: 30%" />
    <col style="width: 20%" />
  </colgroup>
  <thead>
    <tr>
      <th>Кейс и контроллер</th>
      <th>REST-запрос</th>
      <th>Описание действия</th>
      <th>Ожидаемый ответ</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>
        <div>Выбор отделения</div>
        <div><code>ManagementController#getTinyBranches</code></div>
      </td>
      <td><code>GET /managementinformation/branches/tiny</code></td>
      <td>Загружает компактный список отделений при входе оператора.</td>
      <td>
        <div><code>200 OK</code> — JSON с полями <code>id</code>, <code>name</code>.</div>
        <div><code>400 Bad Request</code> — неверные параметры фильтрации.</div>
        <div><code>500 Internal Server Error</code> — сбой сервера.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Загрузка рабочих мест</div>
        <div><code>ServicePointController#getServicePoints</code></div>
      </td>
      <td><code>GET /servicepoint/branches/{branchId}/servicePoints</code></td>
      <td>Возвращает точки обслуживания отделения и их доступность.</td>
      <td>
        <div><code>200 OK</code> — список объектов с признаками занятости.</div>
        <div><code>404 Not Found</code> — отделение отсутствует.</div>
        <div><code>500 Internal Server Error</code> — ошибка сервиса.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Загрузка сотрудников</div>
        <div><code>ServicePointController#getUsersOfBranch</code></div>
      </td>
      <td><code>GET /servicepoint/branches/{branchId}/users</code></td>
      <td>Показывает сотрудников отделения и их текущий статус.</td>
      <td>
        <div><code>200 OK</code> — массив пользователей.</div>
        <div><code>404 Not Found</code> — отделение не найдено.</div>
        <div><code>500 Internal Server Error</code> — ошибка сервера.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Каталог услуг</div>
        <div><code>EntrypointController#getAllServices</code></div>
      </td>
      <td><code>GET /entrypoint/branches/{branchId}/services/all</code></td>
      <td>Загружает полный перечень услуг и их параметров.</td>
      <td>
        <div><code>200 OK</code> — массив услуг.</div>
        <div><code>404 Not Found</code> — отделение недоступно.</div>
        <div><code>500 Internal Server Error</code> — ошибка интеграций.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Картина очередей</div>
        <div><code>ServicePointController#getFullQueues</code></div>
      </td>
      <td><code>GET /servicepoint/branches/{branchId}/queues/full</code></td>
      <td>Отображает очереди отделения, их талоны и метрики.</td>
      <td>
        <div><code>200 OK</code> — список очередей с визитами.</div>
        <div><code>404 Not Found</code> — отделение отсутствует.</div>
        <div><code>500 Internal Server Error</code> — ошибка сервера.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Аннулирование визита</div>
        <div><code>ServicePointController#deleteVisit</code></div>
      </td>
      <td><code>DELETE /servicepoint/branches/{branchId}/visits/{visitId}</code></td>
      <td>Удаляет талон по идентификатору в интерфейсе оператора.</td>
      <td>
        <div><code>204 No Content</code> — визит удалён.</div>
        <div><code>404 Not Found</code> — визит не найден.</div>
        <div><code>409 Conflict</code> — нарушено бизнес-правило.</div>
        <div><code>500 Internal Server Error</code> — ошибка сервера.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Список принтеров</div>
        <div><code>ServicePointController#getPrinters</code></div>
      </td>
      <td><code>GET /servicepoint/branches/{branchId}/printers</code></td>
      <td>Показывает доступные устройства печати талонов.</td>
      <td>
        <div><code>200 OK</code> — перечень принтеров.</div>
        <div><code>404 Not Found</code> — отделение отсутствует.</div>
        <div><code>500 Internal Server Error</code> — ошибка сервиса.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Сведения об отделении</div>
        <div><code>ManagementController#getBranch</code></div>
      </td>
      <td><code>GET /servicepoint/branches/{branchId}</code></td>
      <td>Возвращает агрегированное состояние отделения (делегирует <code>ManagementController#getBranch</code>).</td>
      <td>
        <div><code>200 OK</code> — JSON отделения.</div>
        <div><code>404 Not Found</code> — отделение отсутствует.</div>
        <div><code>500 Internal Server Error</code> — ошибка сервера.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Создание визита с печатью</div>
        <div><code>EntrypointController#createVisitFromReception</code></div>
      </td>
      <td><code>POST /entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=true</code></td>
      <td>Создаёт визит из приёмной и печатает талон на выбранном устройстве.</td>
      <td>
        <div><code>200 OK</code> — JSON визита и подтверждение печати.</div>
        <div><code>400 Bad Request</code> — некорректные параметры.</div>
        <div><code>404 Not Found</code> — отделение, услуга или принтер отсутствуют.</div>
        <div><code>500 Internal Server Error</code> — ошибка бизнес-логики.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Создание визита без печати</div>
        <div><code>EntrypointController#createVisitFromReception</code></div>
      </td>
      <td><code>POST /entrypoint/branches/{branchId}/printer/{printerId}/visitWithParameters?printTicket=false</code></td>
      <td>Создаёт виртуальный визит без печати талона.</td>
      <td>
        <div><code>200 OK</code> — JSON визита.</div>
        <div><code>400 Bad Request</code> — неверные параметры запроса.</div>
        <div><code>404 Not Found</code> — связанные сущности не найдены.</div>
        <div><code>500 Internal Server Error</code> — ошибка бизнес-логики.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Перевод визита в конец очереди</div>
        <div><code>ServicePointController#visitTransferFromQueue</code></div>
      </td>
      <td><code>PUT /servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=true</code></td>
      <td>Внешняя служба ставит визит в конец очереди, передавая <code>serviceInfo</code> и задержку.</td>
      <td>
        <div><code>200 OK</code> — визит обновлён.</div>
        <div><code>404 Not Found</code> — отделение, очередь или визит отсутствуют.</div>
        <div><code>500 Internal Server Error</code> — ошибка обработки.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Перевод визита в начало очереди</div>
        <div><code>ServicePointController#visitTransferFromQueue</code></div>
      </td>
      <td><code>PUT /servicepoint/branches/{branchId}/queue/{queueId}/visits/{visitId}/externalService/transfer?isAppend=false</code></td>
      <td>Перемещает визит в начало очереди с помощью внешней службы.</td>
      <td>
        <div><code>200 OK</code> — визит обновлён.</div>
        <div><code>404 Not Found</code> — отделение, очередь или визит отсутствуют.</div>
        <div><code>500 Internal Server Error</code> — ошибка обработки.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Внешний пул точки обслуживания (конец)</div>
        <div><code>ServicePointController#visitTransferFromQueueToServicePointPool</code></div>
      </td>
      <td><code>PUT /servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=true</code></td>
      <td>Переносит визит во внешний пул точки обслуживания в конец списка.</td>
      <td>
        <div><code>200 OK</code> — визит добавлен в пул.</div>
        <div><code>404 Not Found</code> — отделение, точка или визит отсутствуют.</div>
        <div><code>500 Internal Server Error</code> — ошибка сервера.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Внешний пул точки обслуживания (начало)</div>
        <div><code>ServicePointController#visitTransferFromQueueToServicePointPool</code></div>
      </td>
      <td><code>PUT /servicepoint/branches/{branchId}/servicePoint/{servicePointId}/pool/visits/{visitId}/externalService/transfer?isAppend=false</code></td>
      <td>Помещает визит в начало внешнего пула точки обслуживания.</td>
      <td>
        <div><code>200 OK</code> — визит добавлен в пул.</div>
        <div><code>404 Not Found</code> — отделение, точка или визит отсутствуют.</div>
        <div><code>500 Internal Server Error</code> — ошибка сервера.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Внешний пул сотрудника (конец)</div>
        <div><code>ServicePointController#visitTransferFromQueueToUserPool</code></div>
      </td>
      <td><code>PUT /servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=true</code></td>
      <td>Ставит визит в конец внешнего пула выбранного сотрудника.</td>
      <td>
        <div><code>200 OK</code> — визит закреплён за пулом.</div>
        <div><code>404 Not Found</code> — отделение, сотрудник или визит отсутствуют.</div>
        <div><code>500 Internal Server Error</code> — ошибка обработки.</div>
      </td>
    </tr>
    <tr>
      <td>
        <div>Внешний пул сотрудника (начало)</div>
        <div><code>ServicePointController#visitTransferFromQueueToUserPool</code></div>
      </td>
      <td><code>PUT /servicepoint/branches/{branchId}/users/{userId}/pool/visits/{visitId}/externalService/transfer?isAppend=false</code></td>
      <td>Помещает визит в начало внешнего пула сотрудника.</td>
      <td>
        <div><code>200 OK</code> — визит закреплён за пулом.</div>
        <div><code>404 Not Found</code> — отделение, сотрудник или визит отсутствуют.</div>
        <div><code>500 Internal Server Error</code> — ошибка обработки.</div>
      </td>
    </tr>
  </tbody>
</table>


### Кейсы аутентификации

<table>
  <colgroup>
    <col style="width: 20%" />
    <col style="width: 30%" />
    <col style="width: 30%" />
    <col style="width: 20%" />
  </colgroup>
  <thead>
    <tr>
      <th>Кейс</th>
      <th>REST-запрос</th>
      <th>Описание</th>
      <th>Ожидаемый ответ</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Валидный токен</td>
      <td>Любой защищённый REST-эндпоинт</td>
      <td>Запрос с корректным заголовком <code>Authorization: Bearer &lt;token&gt;</code>.</td>
      <td><code>200 OK</code></td>
    </tr>
    <tr>
      <td>Просроченный токен</td>
      <td>Любой защищённый REST-эндпоинт</td>
      <td>Используется токен с истёкшим сроком действия.</td>
      <td><code>401 Unauthorized</code></td>
    </tr>
    <tr>
      <td>Отсутствие токена</td>
      <td>Запрос без заголовка <code>Authorization</code></td>
      <td>Отправка запроса без передачи bearer-токена.</td>
      <td><code>401 Unauthorized</code></td>
    </tr>
    <tr>
      <td>Недостаточно прав</td>
      <td>Любой защищённый REST-эндпоинт</td>
      <td>Токен не содержит требуемой роли или scope.</td>
      <td><code>403 Forbidden</code></td>
    </tr>
    <tr>
      <td>Неверная подпись</td>
      <td>Любой защищённый REST-эндпоинт</td>
      <td>Токен модифицирован, подпись не совпадает с ключом провайдера.</td>
      <td><code>401 Unauthorized</code></td>
    </tr>
    <tr>
      <td>Отозванный токен</td>
      <td>Любой защищённый REST-эндпоинт</td>
      <td>Используется токен, который был отозван сервером аутентификации.</td>
      <td><code>401 Unauthorized</code></td>
    </tr>
    <tr>
      <td>Неизвестный издатель</td>
      <td>Любой защищённый REST-эндпоинт</td>
      <td>Токен выпущен другим Identity Provider и не доверен системе.</td>
      <td><code>401 Unauthorized</code></td>
    </tr>
  </tbody>
</table>

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
