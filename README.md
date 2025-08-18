<div >

# VisitManager

Служба управления визитами и клиентопотоком (Micronaut, Java 17).

![Java](https://img.shields.io/badge/Java-17-007396)
![Micronaut](https://img.shields.io/badge/Micronaut-4.4.3-1C1C1C)
![Build](https://img.shields.io/badge/Build-Maven-blue)

</div>

## Содержание
- Введение
- Архитектура и компоненты
- Функциональные возможности
- Варианты использования
- Инструкции для разработчиков
- Руководство по развертыванию
- Аутентификация и безопасность
- REST API (обзор по контроллерам)
- Диаграммы PlantUML
- Переменные окружения
- Структура проекта

---

## Введение
VisitManager — сервис для управления визитами, очередями, точками обслуживания и сотрудниками в отделениях. Проект построен на Micronaut 4.4.3 (Java 17), использует Keycloak для аутентификации, поддерживает интеграции с Redis и Kafka.

Ключевые зависимости (см. pom.xml): Micronaut Security (JWT/OAuth2), OpenAPI, Kafka, Redis, Lombok, Logback, Keycloak 26.x.

---

## Архитектура и компоненты
Высокоуровневое описание контейнеров:

```plantuml
@startuml
!include <C4/C4_Container.puml>

Person(customer, "Клиент")
Person(staff, "Сотрудник/Оператор")

System_Boundary(vm, "VisitManager") {
  Container(api, "VisitManager API", "Micronaut 4.4.3", "REST API для управления визитами, очередями, конфигурацией")
  ContainerDb(redis, "Redis", "Кэш/сессии")
  Container(queue, "Kafka", "Шина событий")
}

System_Ext(keycloak, "Keycloak", "OIDC/OAuth2")
System_Ext(ui, "Web/UI/Терминалы", "Веб-клиенты и устройства печати")

Rel(customer, ui, "Получает талон, отслеживает очередь")
Rel(staff, ui, "Работает на рабочем месте")
Rel(ui, api, "HTTP/JSON")
Rel(api, keycloak, "Introspection/Token, Login")
Rel(api, queue, "Публикация событий")
Rel(api, redis, "Кэш/сессии")
' Персистентное хранилище не используется в текущей конфигурации

@enduml
```

Основные пакеты и назначение:
- `ru.aritmos.api` — REST-контроллеры: зона обслуживания, ожидания, конфигурация, управление инфо, интеграция с Keycloak.
- `ru.aritmos.service` — бизнес-логика: визиты, отделения, конфигурация, услуги.
- `ru.aritmos.model` — доменные модели: Branch, Service, ServicePoint, Queue, Visit и др.
- `ru.aritmos.keycloack` — интеграция с Keycloak (клиент, модели).

Потоки данных (упрощенно):
1) Клиент/терминал создает визит —> API —> проверка конфигурации —> постановка в очередь —> событие в Kafka.
2) Сотрудник открывает рабочее место —> обслуживает визиты —> перевод между состояниями —> события в Kafka.
3) Администратор обновляет конфигурацию отделения (услуги, очереди, СП, правила сегментации).

---

## Функциональные возможности
- Управление визитами: создание (обычное/виртуальное), обновление параметров, маршрутизация по очередям.
- Зона обслуживания: открытие/закрытие рабочих мест, вызов/перевод/завершение визитов, перерывы.
- Зона ожидания: список доступных услуг (с учетом рабочих профилей), создание визитов через терминал/приемную.
- Конфигурация отделений: услуги, группы услуг, очереди, точки обслуживания, правила сегментации, причины перерывов, авто-вызов.
- Информация об отделениях: полное состояние/«облегченный» список.
- Интеграции: Keycloak (аутентификация), Kafka (события), Redis (кэш/сессии), PostgreSQL (персистентность при необходимости).
 - Интеграции: Keycloak (аутентификация), Kafka (события), Redis (кэш/сессии).

---

## Инструкции для разработчиков

Предусловия
- Java 17 (JDK 17)
- Maven 3.9+
- Docker (опционально) — Keycloak/Redis/PostgreSQL локально

Сборка и запуск
- Сборка: `mvn clean package`
- Запуск из Maven: `mvn mn:run` или `mvn exec:java -Dexec.mainClass=ru.aritmos.Application`
- Запуск JAR: `java -jar target/visitmanager-*.jar`

Конфигурация
- Переменная `ENVIRONMENT` (dev/test/prod)
- Параметры Micronaut/Keycloak/Redis/DB — в `application.yml` и переменных окружения

Тестирование
- Юнит-тесты: `mvn test`
- Интеграционные: micronaut-test-resources

Документация API
- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/swagger/visitmanager-<version>.json` (имя может отличаться)

---

## Руководство по развертыванию (Docker)

Вариант 1. Нативный jar
1) Настроить переменные окружения (см. ниже).
2) Запустить зависимые сервисы (Keycloak, Redis).
3) `java -jar target/visitmanager-*.jar`.

В проекте есть готовые compose-файлы и Dockerfile. Ниже подробное описание каждого файла, переменных и сценариев запуска.

Обзор файлов
- `Dockerfile` — сборка образа приложения (многоэтапная: builder Maven + рантайм JDK 17).
- `docker-compose.yml` — запуск из готового реестрового образа + локальный Redis.
- `docker-compose-main.yml` — обертка, которая подключает `docker-compose.yml` и объявляет внешнюю сеть `aritmosplatform-backend`.
- `docker-compose.local.yml` — локальная разработка: билд образа из Dockerfile, запуск Redis, публикация порта 8080, расширенные переменные.
- `.env.*` — примеры/профили переменных окружения (не храните секреты в VCS).

Dockerfile (полное описание)
- Билд-стейдж: `FROM maven:3.9.9-eclipse-temurin-17 AS builder`
  - Копирует весь проект в `/app` и выполняет `mvn clean package -Dmaven.test.skip=true`.
  - Результат: `target/visitmanager.jar`.
- Рантайм-стейдж: `FROM bellsoft/liberica-openjdk-alpine:17`
  - Копирует `visitmanager.jar` в `app.jar`.
  - Открывает порт `8080` и запускает приложение `ENTRYPOINT ["java","-jar","app.jar"]`.
- Примечание: в Dockerfile есть закомментированные альтернативы (jlink/Alpine) и пример ENV — они не активны.

docker-compose.yml (прод/тест из реестра)
- services.visitmanager
  - `image`: `registry.aritmosplatform.ru/aritmosplatform/visitmanager:${ENVIRONMENT}`
  - `hostname`: `visitmanager`
  - `environment` (ключевые):
    - `REDIS_SERVER=redis://visitmanager-redis:6379/6`
    - `DATABUS_SERVER=http://databus:8080`
    - `CONFIG_SERVER=http://localhost:8081`
    - `PRINTER_SERVER=http://192.168.3.29:8084` (пример)
    - `OIDC_ISSUER_DOMAIN=http://keycloak:8080/realms/aritmos_platform`
    - `OIDC_ISSUER_URL=http://keycloak:8080`
    - `OAUTH_CLIENT_ID=${CLIENT_ID}` / `OAUTH_CLIENT_SECRET=${CLIENT_SECRET}`
    - `KEYCLOAK_TECHLOGIN=${LOGIN_ADMIN}` / `KEYCLOAK_TECHPASSWORD=${PASSWORD_ADMIN}`
    - `KAFKA_SERVER=kafka:9092`
    - `LOKI_SERVER=http://loki:3100/loki/api/v1/push`
  - `env_file`: `./.env` — подстановка переменных.
  - `expose`: 8080, 9092, 9093, 9094, 8082 (для связи внутри сети).
  - `ports`: `8080:8080` — публикация API наружу.
  - `networks`: `aritmosplatform-backend`, `visitmanager-network`.
  - `depends_on`: `visitmanager-redis`.
  - `restart: always`.
- services.visitmanager-redis
  - `image: redis:latest`
  - `hostname: visitmanager-redis`
  - `environment`: `REDIS_PASSWORD`, `REDIS_PORT=6379`, `REDIS_DATABASES=16` (берутся из `.env`).
  - `expose`: 6379 (и 9092 указано, но не требуется — оставлено как есть).
  - `networks`: `visitmanager-network`.
  - `volumes`: именованный том `redis_data:/data`.
  - `healthcheck`: `redis-cli --raw incr ping` с интервалом 10s.
  - `restart: always`.
- volumes: `redis_data` (local).
- networks: `visitmanager-network` (bridge).

docker-compose-main.yml (внешняя сеть)
- `include: - docker-compose.yml` — агрегирует базовый compose.
- `networks.aritmosplatform-backend` — объявляет bridge-сеть, через которую сервис общается с внешними компонентами (Keycloak, Kafka, Loki, Databus и т.д.).

docker-compose.local.yml (локальная разработка)
- services.visitmanager
  - `build`: из `Dockerfile` (context `./`).
  - `image`: `aritmos/visitmanager:0.8` (тег локального образа).
  - `container_name`: `visitmanager`.
  - `ports`: `8080:8080`.
  - `expose`: 8080, 9092, 9093, 9094, 8082.
  - `networks`: `aritmosplatform-backend` (внешняя сеть должна существовать заранее).
  - `depends_on`: `visitmanager-redis` (с `condition: service_healthy`).
  - `environment`: аналогичны `docker-compose.yml`, но значения заданы явно для локального окружения.
  - `restart: always`.
- services.visitmanager-redis
  - `image: redis:latest`
  - `container_name: visitmanager-redis`
  - `expose`: 6379
  - `volumes`: монтирование локальных путей для данных/конфига Redis (замените `/path/to/local/...` на ваши пути).
  - `environment`: `REDIS_PASSWORD`, `REDIS_PORT=6379`, `REDIS_DATABASES=16`.
  - `healthcheck`: как в базовом файле.
  - `restart: always`.
- networks
  - `aritmosplatform-backend`: `external: true` — сеть должна быть создана заранее (`docker network create aritmosplatform-backend`).

Запуск и управление
- Прод/тест из реестра:
  - `docker compose -f docker-compose-main.yml --env-file .env up -d`
- Локальная разработка (с билдом):
  - `docker compose -f docker-compose.local.yml up -d --build`
- Остановка: `docker compose -f <file> down`
- Перезапуск сервиса: `docker compose -f <file> restart visitmanager`
- Логи: `docker compose -f <file> logs -f visitmanager`
- Инспекция сети: `docker network ls`, `docker network inspect <network>`

Сети и порты

Переменные окружения (.env)
- Используйте `.env` для безопасной подстановки значений (см. список в разделе «Переменные окружения» ниже).
- Не коммитьте реальные секреты: используйте `.env.local`/`.env.dev` в локальной среде и секреты в CI/CD/оркестраторе в проде.

Внешние зависимости (ожидаемые эндпоинты)
- Keycloak: `http://keycloak:8080` (realm `aritmos_platform`)
- Kafka: `kafka:9092`
- Loki: `http://loki:3100` (push API `/loki/api/v1/push`)
- Databus: `http://databus:8080`
- Config Server: `http://localhost:8081` (или ваш адрес)
- Printer Server: `http://192.168.3.29:8084` (пример)

Исходные файлы Docker

Dockerfile
```dockerfile
#Перед этим нужно поменять версию в pom.xml на 17
#FROM amazoncorretto:17.0.4-alpine3.15 AS builder
#ADD [".\\", "/src"]
#WORKDIR /src
#COPY pom.xml .
#COPY src ./src
FROM maven:3.9.9-eclipse-temurin-17 AS builder
COPY . /app
WORKDIR /app


#ENV REDIS_SERVER="redis://redis:6379"
#ENV REDIS_SERVER_TEST="redis://localhost:6379"
#ENV DATABUS_SERVER="http://192.168.8.45:8082"
#ENV CONFIG_SERVER="http://localhost:8081"
#ENV PRINTER_SERVER="http://192.168.3.33:8084"
#ENV OIDC_ISSUER_DOMAIN="http://192.168.8.45:9090/realms/Aritmos"
#ENV OIDC_ISSUER_URL="http://192.168.8.45:9090"
#ENV OAUTH_CLIENT_ID="myclient"
#ENV KEYCLOAK_TECHLOGIN="visitmanager"
#ENV KEYCLOAK_TECHPASSWORD="visitmanager"
#RUN sed -i 's/\r$//' mvnw
#RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -Dmaven.test.skip=true
RUN mvn clean package -Dmaven.test.skip=true



#FROM amazoncorretto:17.0.4-alpine3.15 AS packager
#RUN apk add --no-cache binutils
#ENV JAVA_MINIMAL="/opt/java-minimal"
#RUN $JAVA_HOME/bin/jlink \
#                  --verbose \
#                  --add-modules \
#                      java.base,java.sql,java.naming,java.desktop,java.management,java.security.jgss,jdk.zipfs,jdk.unsupported \
#                  --compress 2 --strip-debug --no-header-files --no-man-pages \
#                  --output "$JAVA_MINIMAL"

#FROM alpine:3.16.2
#ENV JAVA_HOME=/opt/java-minimal
#ENV PATH="$PATH:$JAVA_HOME/bin"
#ENV REDIS_SERVER="redis://redis:6379"
#ENV REDIS_SERVER_TEST="redis://localhost:6379"
#ENV DATABUS_SERVER="http://192.168.8.45:8082"
#ENV CONFIG_SERVER="http://localhost:8081"
#ENV PRINTER_SERVER="http://192.168.3.33:8084"
#ENV OIDC_ISSUER_DOMAIN="http://192.168.8.45:9090/realms/Aritmos"
#ENV OIDC_ISSUER_URL="http://192.168.8.45:9090"
#ENV OAUTH_CLIENT_ID="myclient"
#ENV KEYCLOAK_TECHLOGIN="visitmanager"
#ENV KEYCLOAK_TECHPASSWORD="visitmanager"
#ENV KAFKA_SERVER="192.168.8.45:9094"
#ENV LOKI_SERVER="http://192.168.3.13:3100/loki/api/v1/push"
#COPY --from=packager "$JAVA_HOME" "$JAVA_HOME"
FROM bellsoft/liberica-openjdk-alpine:17
COPY --from=builder "/app/target/visitmanager.jar" "app.jar"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

docker-compose.yml
```yaml
services:

  visitmanager:
    image: registry.aritmosplatform.ru/aritmosplatform/visitmanager:${ENVIRONMENT}
    hostname: visitmanager
    environment:
      - REDIS_SERVER=redis://visitmanager-redis:6379/6
      - DATABUS_SERVER=http://databus:8080
      - CONFIG_SERVER=http://localhost:8081
      #- PRINTER_SERVER=http://printerservice:8084
      - PRINTER_SERVER=http://192.168.3.29:8084
      - OIDC_ISSUER_DOMAIN=http://keycloak:8080/realms/aritmos_platform
      - OAUTH_CLIENT_ID=${CLIENT_ID}
      - KEYCLOAK_TECHLOGIN=${LOGIN_ADMIN}
      - KEYCLOAK_TECHPASSWORD=${PASSWORD_ADMIN}
      - OIDC_ISSUER_URL=http://keycloak:8080
      - KAFKA_SERVER=kafka:9092
      - LOKI_SERVER=http://loki:3100/loki/api/v1/push
      - OAUTH_CLIENT_SECRET=${CLIENT_SECRET}
    env_file: ./.env
    expose:
      - '8080'
      - '9094'
      - '9092'
      - '9093'
      - '8082'
    ports:
      - "8080:8080"
    networks:
      - aritmosplatform-backend
      - visitmanager-network
    depends_on:
      - visitmanager-redis
    #      visitmanager-redis:
    #        condition: service_healthy
    restart: always

  visitmanager-redis:
    image: redis:latest
    hostname: visitmanager-redis
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - REDIS_PORT=6379
      - REDIS_DATABASES=16
    env_file: ./.env
    expose:
      - '6379'
      - '9092'
    networks:
      - visitmanager-network
    restart: always
    # ports:
    #   - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      interval: 10s
      timeout: 5s
      retries: 5
      test: [ "CMD", "redis-cli", "--raw", "incr", "ping" ]

volumes:
  redis_data:
    external: false

networks:
  visitmanager-network:
    driver: bridge
```

docker-compose-main.yml
```yaml
include:
  - docker-compose.yml

networks:
  aritmosplatform-backend:
    driver: bridge
```

docker-compose.local.yml
```yaml
services:

  visitmanager:
    expose:
      - '8080'
      - '9094'
      - '9092'
      - '9093'
      - '8082'
    networks:
      - aritmosplatform-backend
    container_name: visitmanager
    ports:
      - "8080:8080"
    build:
      context: ./
      dockerfile: Dockerfile
    image: aritmos/visitmanager:0.8
    depends_on:
      visitmanager-redis:
        condition: service_healthy
    environment:
      - REDIS_SERVER=redis://visitmanager-redis:6379/6
      - DATABUS_SERVER=http://databus:8080
      - CONFIG_SERVER=http://localhost:8081
      #- PRINTER_SERVER=http://printerservice:8084
      - PRINTER_SERVER=http://192.168.3.29:8084
      - OIDC_ISSUER_DOMAIN=http://keycloak:8080/realms/aritmos_platform
      - OAUTH_CLIENT_ID=myclient
      - KEYCLOAK_TECHLOGIN=admin
      - KEYCLOAK_TECHPASSWORD=LtlVjhjp!23
      - OIDC_ISSUER_URL=http://keycloak:8080
      - KAFKA_SERVER=kafka:9092
      - LOKI_SERVER=http://loki:3100/loki/api/v1/push
      - OAUTH_CLIENT_SECRET=HKdkHNfaMWWsDqFa6a4q3kcNVkRjN0Wv
      - KEYCLOAK_REALM=aritmos_platform
    restart: always
  visitmanager-redis:
    expose:
      - '6379'
      - '9092'
    healthcheck:
      interval: 10s
      timeout: 5s
      retries: 5
      test: [ "CMD", "redis-cli", "--raw", "incr", "ping" ]
    networks:
      - aritmosplatform-backend
    image: redis:latest
    container_name: visitmanager-redis
    restart: always
    # ports:
    #   - "6379:6379"
    volumes:
      - /path/to/local/dаta:/root/redis
      - /path/to/local/redis.conf:/usr/local/etc/redis/redis.conf
    environment:
      - REDIS_PASSWORD=my-password
      - REDIS_PORT=6379
      - REDIS_DATABASES=16
networks:
  aritmosplatform-backend:
    external:
      name: aritmosplatform-backend
```
    environment:
      - ENVIRONMENT=prod
      - KEYCLOAK_URL=http://keycloak:8080
      - KEYCLOAK_REALM=master
      - KEYCLOAK_CLIENT_ID=visitmanager
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    ports:
      - "8080:8080"
    depends_on:
      - keycloak
      - redis
  redis:
    image: redis:6.2
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    command: start-dev --http-port=8080
```

Безопасность
- Храните секреты вне репозитория (Vault/Secret Manager)
- Ограничьте Swagger UI в prod
- Используйте TLS/HTTPS

---

## Аутентификация и безопасность
- Keycloak (OIDC/OAuth2) + Micronaut Security
- Вспомогательные эндпоинты через `KeyCloakController`
- Передавайте Bearer-токен в заголовке `Authorization`

Пример получения токена
```bash
curl -X POST http://localhost:8080/keycloak \
  -H 'Content-Type: application/json' \
  -d '{"login":"user","password":"pass"}'
```

Завершение сессии пользователя
```bash
curl -X POST 'http://localhost:8080/keycloak/users/john.doe?isForced=true&reason=SECURITY'
```

---

## REST API (обзор по контроллерам)

Обзор ключевых эндпоинтов на основе `ru.aritmos.api`. Полные детали — в Swagger.

### EntrypointController (`/entrypoint`)
- POST `/branches/{branchId}/servicePoint/{servicePointId}/virtualVisit` — виртуальный визит.
- POST `/branches/{branchId}/entryPoints/{entryPointId}/visit` — создание визита (опц. печать, `segmentationRuleId`).
- POST `/branches/{branchId}/printer/{printerId}/visitWithParameters` — создание визита + печать на принтере.
- POST `/branches/{branchId}/entryPoints/{entryPointId}/visitWithParameters` — создание визита с параметрами.
- PUT `/branches/{branchId}/visits/{visitId}` — обновление параметров визита.
- GET `/branches/{branchId}/services` — доступные услуги (с учетом активных рабочих профилей).
- GET `/branches/{branchId}/services/all` — полный список услуг отделения.

Примеры
```bash
curl -X POST \
  'http://localhost:8080/entrypoint/branches/{branchId}/entryPoints/{entryPointId}/visit' \
  -H 'Content-Type: application/json' \
  -d '["<serviceId1>","<serviceId2>"]'
```

```bash
curl -X POST \
  'http://localhost:8080/entrypoint/branches/{branchId}/servicePoint/{servicePointId}/virtualVisit' \
  -H 'Content-Type: application/json' \
  -d '["<serviceId>"]'
```

### ServicePointController (`/servicepoint`)
Группы операций:
- Данные: принтеры, очереди (кратко/полно), точки обслуживания (кратко/детально), сотрудники, рабочие профили.
- Поиск по пользователю: рабочее место/пользователь по логину.
- Работа сотрудников: открытие/закрытие, перерывы, авто-вызов.
- Управление визитами: вызов, перенос, завершение, отмена, возврат в ожидание, печать и т.д.

Пример: открыть рабочее место
```bash
curl -X POST \
  'http://localhost:8080/servicepoint/branches/{branchId}/servicePoints/{servicePointId}/workProfiles/{workProfileId}/users/{userName}/open'
```

Пример: закрыть рабочее место (перерыв/форс)
```bash
curl -X POST \
  'http://localhost:8080/servicepoint/branches/{branchId}/servicePoints/{servicePointId}/close?isBreak=true&breakReason=LUNCH&isForced=false'
```

### ConfigurationController (`/configuration`)
- POST `/branches` — полное обновление конфигурации (bulk).
- POST `/branches/hardcode` — загрузка демо-конфигурации.
- PUT `/branches/{branchId}/services` — добавить/обновить услуги (`checkVisits`).
- DELETE `/branches/{branchId}/services` — удалить услуги (`checkVisits`).
- PUT `/branches/{branchId}/servicePoints` — добавить/обновить СП (`restoreVisit`, `restoreUser`).
- DELETE `/branches/{branchId}/servicePoints` — удалить СП.
- PUT `/branches/{branchId}/serviceGroups` — добавить/обновить группы услуг.
- PUT `/branches/{branchId}/segmentationRules` — добавить/обновить правила сегментации.
- PUT `/branches/{branchId}/queues` — добавить/обновить очереди (`restoreVisits`).
- DELETE `/branches/{branchId}/queues` — удалить очереди.
- GET `/branches/{branchId}/break/reasons` — причины перерывов.
- PUT `/branches/{branchId}/autocallModeOn|Off` — включить/выключить авто-вызов.

### ManagementController (`/managementinformation`)
- GET `/branches/{id}` — состояние отделения.
- GET `/branches` — карта отделений (с фильтром по `userName`).
- GET `/branches/tiny` — компактный список отделений (id+name).

### KeyCloakController (без префикса)
- POST `/keycloak` — авторизация (возврат `AuthorizationResponse`).
- POST `/keycloak/users/{login}` — завершение сессии пользователя (`isForced`, `reason`).

---

## Варианты использования

Ключевые сценарии работы сервиса (укрупненно):
- Клиент/терминал:
  - Получение талона и создание визита (обычный/виртуальный)
  - Просмотр доступных услуг филиала
- Сотрудник (оператор):
  - Открытие/закрытие рабочего места, установка перерыва
  - Вызов, перевод, завершение визитов; возврат в ожидание; печать талона
  - Включение/выключение авто-вызова
- Администратор отделения:
  - Обновление конфигурации: услуги, группы, очереди, точки обслуживания, правила сегментации
  - Получение агрегированного состояния отделения, компактных списков
- Интеграции/Инфраструктура:
  - Аутентификация через Keycloak
  - Публикация доменных событий в Kafka
  - Кэширование состояний и конфигурации в Redis

Диаграмма вариантов использования (упрощенно)
```plantuml
@startuml
left to right direction
actor Клиент as Client
actor Сотрудник as Staff
actor Администратор as Admin

rectangle "VisitManager API" {
  usecase UC1 as "Получить талон\n(создать визит)"
  usecase UC2 as "Создать виртуальный визит"
  usecase UC3 as "Просмотреть услуги"
  usecase UC4 as "Открыть/закрыть рабочее место"
  usecase UC5 as "Вызвать/перевести/завершить визит"
  usecase UC6 as "Перерыв"
  usecase UC7 as "Управление конфигурацией"
  usecase UC8 as "Получить состояние отделения"
}

Client -- UC1
Client -- UC2
Client -- UC3
Staff -- UC4
Staff -- UC5
Staff -- UC6
Admin -- UC7
Admin -- UC8

note right of UC7
  Услуги, очереди, СП,
  группы, правила сегментации,
  авто-вызов
end note

@enduml
```

---

## Диаграммы PlantUML

Диаграмма последовательности: создание визита
```plantuml
@startuml
actor Client
participant UI as "Терминал/UI"
participant API as "VisitManager API"
participant Branch as "BranchService"
participant Visit as "VisitService"
database Redis
queue Kafka

Client -> UI: Выбор услуг
UI -> API: POST /entrypoint/.../visit
API -> Branch: Получить конфигурацию отделения
Branch --> API: OK / Услуги существуют
API -> Visit: Создать визит (serviceIds, params)
Visit -> Redis: Обновить кэш/состояние
Visit -> Kafka: VISIT_CREATED
Visit --> API: Visit{ id, ticket, queue }
API --> UI: 200 + тело визита
@enduml
```

Диаграмма компонентов (упрощенно)
```plantuml
@startuml
!include <C4/C4_Component.puml>

Container_Boundary(api, "VisitManager API") {
  Component(entry, "EntrypointController", "REST")
  Component(sp, "ServicePointController", "REST")
  Component(cfg, "ConfigurationController", "REST")
  Component(mgmt, "ManagementController", "REST")
  Component(kc, "KeyCloakController", "REST")
  Component(svcBranch, "BranchService", "BL")
  Component(svcVisit, "VisitService", "BL")
}
System_Ext(kc_ext, "Keycloak")
ContainerDb(redis, "Redis", "Кэш/сессии")
Container(queue, "Kafka", "Шина событий")

Rel(entry, svcVisit, "Создание визитов")
Rel(sp, svcVisit, "Обслуживание визитов")
Rel(cfg, svcBranch, "Изменение конфигурации")
Rel(mgmt, svcBranch, "Чтение состояния отделений")
Rel(kc, kc_ext, "Аутентификация", "OIDC")

Rel(svcVisit, redis, "Кэширование/состояние")
Rel(svcBranch, redis, "Кэш конфигурации")
Rel(svcVisit, queue, "Публикация событий")
@enduml
```

---

## Переменные окружения
- `ENVIRONMENT` — окружение (dev/test/prod)
- `KEYCLOAK_URL` — базовый URL Keycloak
- `KEYCLOAK_REALM` — realm
- `KEYCLOAK_CLIENT_ID` — client-id
- `REDIS_HOST`, `REDIS_PORT`

Используемые в docker-compose переменные (и параметры) сервиса
- `REDIS_SERVER` — строка подключения Redis (например: `redis://visitmanager-redis:6379/6`)
- `DATABUS_SERVER` — URL Databus (например: `http://databus:8080`)
- `CONFIG_SERVER` — URL сервера конфигурации (например: `http://localhost:8081`)
- `PRINTER_SERVER` — URL сервиса печати (например: `http://printerservice:8084` или IP)
- `OIDC_ISSUER_DOMAIN` — Issuer для OIDC (например: `http://keycloak:8080/realms/aritmos_platform`)
- `OIDC_ISSUER_URL` — базовый URL Keycloak (например: `http://keycloak:8080`)
- `OAUTH_CLIENT_ID`, `OAUTH_CLIENT_SECRET` — OAuth2 клиент
- `KEYCLOAK_TECHLOGIN`, `KEYCLOAK_TECHPASSWORD` — техническая учетная запись
- `KAFKA_SERVER` — адрес брокера Kafka (например: `kafka:9092`)
- `LOKI_SERVER` — URL push API Loki (например: `http://loki:3100/loki/api/v1/push`)
- `REDIS_PASSWORD` — пароль Redis (используется контейнером `visitmanager-redis` из `.env`)

Дополнительно в `application.yml`:
- `micronaut.security.oauth2.clients.keycloak.*` (techlogin, techpassword, client-id, realm, keycloakurl)

---

## Структура проекта (основное)
```
src/
  main/java/ru/aritmos/
    api/
      EntrypointController.java
      ServicePointController.java
      ConfigurationController.java
      ManagementController.java
      KeyCloakController.java
    service/
      BranchService.java
      VisitService.java
      ...
    model/
      ... доменные классы (Branch, Service, ServicePoint, Queue, Visit, ...)
    keycloack/
      ... интеграция с Keycloak
  main/resources/
    application.yml
```

---

## Полезные ссылки
- Micronaut Guide: https://docs.micronaut.io/4.4.3/guide/index.html
- OpenAPI/Swagger: https://micronaut-projects.github.io/micronaut-openapi/latest/guide/
- Kafka: https://micronaut-projects.github.io/micronaut-kafka/latest/guide/
- Security OAuth2: https://micronaut-projects.github.io/micronaut-security/latest/guide/
