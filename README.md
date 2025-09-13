# VisitManager

Служба управления визитами и клиентопотоком, построенная на Micronaut 4.4.3 и Java 17. Сервис управляет очередями, точками обслуживания, сотрудниками и визитами клиентов в отделениях.

![Java](https://img.shields.io/badge/Java-17-007396)
![Micronaut](https://img.shields.io/badge/Micronaut-4.4.3-1C1C1C)
![Build](https://img.shields.io/badge/Build-Maven-blue)
![Tests](https://img.shields.io/badge/Tests-Maven%20Passing-brightgreen)
[![Docs](https://img.shields.io/badge/Docs-Use%20Cases-blue)](docs/use-cases.md)
![Coverage](https://img.shields.io/badge/Coverage-80%25-yellow)
![Docker](https://img.shields.io/badge/Docker-ready-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

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
 - [📘 Документация классов](#-документация-классов)
- [🧑‍💼 Сценарии работы сотрудника](#-сценарии-работы-сотрудника)
- [🤖 Документация для автотестеров](#-документация-для-автотестеров)
- [🧪 Тестирование](#-тестирование)
- [🌐 Переменные окружения](#-переменные-окружения)
- [🔗 Полезные ссылки](#-полезные-ссылки)

## 🧾 Обзор
VisitManager предоставляет REST‑интерфейсы для создания визитов, управления очередями и обслуживания клиентов. Сервис обрабатывает события асинхронно через Kafka, кеширует конфигурацию в Redis и использует Keycloak для аутентификации. Поддерживается горизонтальное масштабирование и расширяемая модель домена.

## ⚙️ Настройка окружения

### Требования
- JDK 17
- Maven 3 (локально установленный `mvn`, поддерживающий память настроек и прокси)
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

### Профиль `javadoc-strict`
Профиль для строгой генерации Javadoc: включает полный doclint и завершает сборку при предупреждениях.
```bash
mvn -s .mvn/settings.xml -Pjavadoc-strict javadoc:javadoc
```

### Работа за прокси
Проект уже содержит файл `.mvn/settings.xml` с настроениями прокси, поэтому достаточно запускать Maven с опцией `-s .mvn/settings.xml`.
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
    service/       бизнес-логика и правила вызова
    model/         доменные сущности (Branch, Service, Visit...)
    events/        генерация и обработка событий
    clients/       внешние REST-клиенты
    keycloack/     интеграция с Keycloak
  main/resources/  конфигурация Micronaut
  test/            модульные и интеграционные тесты
docs/              дополнительная документация
scripts/           примеры сценариев
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

## 📦 Примеры кода

### Использование сервиса
```java
import jakarta.inject.Inject;
import ru.aritmos.service.VisitService;

class VisitFacade {
    @Inject VisitService visitService;

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
import jakarta.inject.Singleton;

@Client("/")
interface VisitClient {
    @Post("/entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit")
    Visit create(String branchId, String entryPointId, List<String> services);
}
```
```java
// использование клиента
Visit created = visitClient.create("001", "01", List.of("serviceId1"));
```

### Работа с `HttpClient`
```java
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.HttpRequest;

try (HttpClient client = HttpClient.create(new URL("http://localhost:8080"))) {
    BlockingHttpClient blocking = client.toBlocking();
    HttpRequest<?> req = HttpRequest.GET("/managementinformation/branches");
    String body = blocking.retrieve(req);
    System.out.println(body);
}
```

## 📊 Диаграммы

Диаграммы доступны в формате SVG (исходники — в `docs/diagrams/*.puml`):

![Кейсы использования](docs/diagrams/use-cases.svg)
![Архитектура](docs/diagrams/architecture.svg)
![Последовательность: создание визита](docs/diagrams/sequence-create-visit.svg)
![Последовательность: завершение визита](docs/diagrams/sequence-update-visit.svg)

Дополнительные диаграммы и анализ сценариев см. в [docs/use-cases.md](docs/use-cases.md).

## 📘 Документация классов

Описание классов, сформированное на основе JavaDoc комментариев: [docs/class-docs.md](docs/class-docs.md).

## 🧑‍💼 Сценарии работы сотрудника

![Рабочий процесс сотрудника](docs/diagrams/employee-workflow.svg)

### 1. Открытие рабочего места
1. `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/workProfiles/{workProfileId}/users/{userName}/open` — сотрудник занимает точку обслуживания.

### 2. Вызов визита
1. `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/confirmed/visits/call` — запрос следующего визита (`200 OK` + визит или `204 No Content`).

### 3. Начало обслуживания визита
1. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/confirm/{visitId}` — подтверждение прихода клиента.

### 4. Перевод и возвращение визита
1. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/queue/{queueId}/visit/transferFromServicePoint?isAppend=true` — перевод в другую очередь.
2. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/transfer` — перевод в пул ТО.
3. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/poolServicePoint/{poolServicePointId}/visit/put_back` — возврат из пула.

### 5. Повторный вызов визита
1. `POST /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/confirmed/recall/{visitId}` — повторное приглашение клиента.

### 6. Завершение визита
1. `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{servicePointId}/visit/end?isForced=false` — фиксация завершения обслуживания.

### 7. Закрытие рабочего места
- Перерыв: `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/close?isBreak=true&breakReason=...`
- Выход из системы: `POST /servicepoint/branches/{branchId}/servicePoints/{servicePointId}/logout`

## 🤖 Документация для автотестеров

### Файлы настроек
- `src/test/resources/application.yml` — базовая конфигурация Micronaut для тестов;
- `src/test/resources/application-test.yml` — профиль `test` для автотестов;
- `src/test/resources/application-dev.yml` — конфигурация dev при локальном запуске;
- `src/test/resources/logback-test.xml` — настройки логирования в тестах;
- `src/test/resources/loki.properties` — параметры отправки логов;
- `src/test/resources/keycloak.json` — конфигурация Keycloak для тестов.

### Кейсы клиентов

| Кейс | Запрос | Ожидаемый ответ |
|---|---|---|
| Создание визита | `POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit` тело `["serviceId1"]` | `201 Created` + JSON визита |
| Невалидная услуга | тот же запрос с несуществующей услугой | `404 Not Found` |
| Очередь переполнена | тот же запрос при переполненной очереди | `409 Conflict` |
| Отмена визита | `DELETE /entrypoint/branches/{branchId}/visits/{visitId}` | `200 OK` |
| Статус визита | `GET /entrypoint/branches/{branchId}/visits/{visitId}` | `200 OK` + JSON |
| Печать талона | `POST ...?printTicket=true` | `201 Created`, талон отправлен на принтер |
| Визит с параметрами | `POST /entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visitWithParameters` с телом `{ "serviceIds": [], "parameters": {} }` | `201 Created` |
| Отмена чужого визита | `DELETE` с чужим `visitId` | `403 Forbidden` |

### Кейсы операторов

| Кейс | Запрос | Ожидаемый ответ |
|---|---|---|
| Открытие точки | `POST /servicepoint/branches/{branchId}/servicePoints/{spId}/workProfiles/{wpId}/users/{user}/open` | `200 OK` |
| Вызов визита | `POST /servicepoint/branches/{branchId}/servicePoints/{spId}/confirmed/visits/call` | `200 OK` + JSON визита |
| Подтверждение/завершение | `POST /servicepoint/branches/{branchId}/visits/servicePoints/{spId}/confirmed/confirm/{visitId}` | `200 OK` |
| Нет визитов | `POST /servicepoint/branches/{branchId}/servicePoints/{spId}/confirmed/visits/call` при пустой очереди | `204 No Content` |
| Перевод визита | `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{spId}/queue/{queueId}/visit/transferFromQueue/{visitId}` | `200 OK` |
| Ошибка перевода | тот же запрос с неверными параметрами | `400 Bad Request` или `409 Conflict` |
| Закрытие точки | `POST /servicepoint/branches/{branchId}/servicePoints/{spId}/close` | `200 OK` |
| Перевод в пул ТО | `PUT /servicepoint/branches/{branchId}/visits/servicePoints/{spId}/poolServicePoint/{poolSpId}/visit/transferFromQueue?isAppend=true` | `200 OK` |
| Повторное завершение | повторный `.../confirmed/confirm/{visitId}` | `409 Conflict` |

### Кейсы аутентификации

| Кейс | Запрос | Ожидаемый ответ |
|---|---|---|
| Валидный токен | любой запрос с корректным `Authorization: Bearer <token>` | `200 OK` |
| Просроченный токен | запрос с токеном с истёкшим сроком | `401 Unauthorized` |
| Отсутствие токена | запрос без заголовка Authorization | `401 Unauthorized` |
| Недостаточно прав | токен без нужной роли | `403 Forbidden` |
| Неверная подпись | токен с некорректной подписью | `401 Unauthorized` |
| Отозванный токен | токен, отозванный сервером | `401 Unauthorized` |
| Неизвестный издатель | токен от другого Identity Provider | `401 Unauthorized` |

Подробности сценариев см. в [docs/use-cases.md](docs/use-cases.md).

## 🧪 Тестирование
```bash
JAVA_TOOL_OPTIONS='-Djava.net.preferIPv4Stack=true' mvn -s .mvn/settings.xml test
```
Все тесты выполняются локально; при необходимости интеграций поднимите зависимые сервисы в Docker.

## 🌐 Переменные окружения
- `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`
- `REDIS_HOST`, `REDIS_PORT`
- `KAFKA_SERVER`
- `PRINTER_SERVER`
- `DATABUS_SERVER`
- `OIDC_ISSUER_DOMAIN`, `OIDC_ISSUER_URL`
- `MICRONAUT_ENVIRONMENTS` — активный профиль (например, `local-no-docker`).
- Дополнительно см. `application.yml` и файлы `.env.*`.

## 🔗 Полезные ссылки
- [Micronaut Documentation](https://docs.micronaut.io/latest/guide/)
- [Micronaut OpenAPI](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/)
- [Micronaut Security](https://micronaut-projects.github.io/micronaut-security/latest/guide/)
