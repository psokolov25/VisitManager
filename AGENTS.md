# Repository Guidelines

## Структура проекта и модули
- `src/main/java` — исходники Java (`ru.aritmos.*`), точка входа `ru.aritmos.Application`.
- `src/main/resources` — конфигурации и ресурсы (`application.yml`, `logback.xml`, `keycloak.json`).
- `src/test/java` — модульные/интеграционные тесты; `src/test/resources` — фикстуры.
- `docs/` — документация/диаграммы; `pom.xml` — настройка Maven; контейнеризация: `Dockerfile`, `docker-compose*.yml`.
- Результат сборки: `target/visitmanager.jar`.

## Сборка, тесты и запуск
- Сборка с тестами: `mvn -s .mvn/settings.xml clean verify`.
- Сборка без тестов: `mvn -s .mvn/settings.xml -DskipTests package`.
- Запуск JAR: `java -jar target/visitmanager.jar`.
- Запуск в dev (если подключен плагин): `mvn -s .mvn/settings.xml mn:run`.
- Тесты: `mvn -s .mvn/settings.xml test`.
- Локально в Docker: `docker compose -f docker-compose.local.yml up -d --build`; остановка — `docker compose -f docker-compose.local.yml down`.

## Стиль кодирования и соглашения
- Java 17; отступ 4 пробела; ~120 символов в строке; UTF‑8.
- Пакеты — нижний регистр (`ru.aritmos.*`); классы — PascalCase; методы/поля — camelCase; константы — UPPER_SNAKE_CASE.
- Используйте Lombok для шаблонного кода (`@Getter`, `@Setter`, `@Builder`).
- Аннотации Micronaut/JSR (`@Singleton`, `@NonNull`, `@Nullable`).
- Логирование через SLF4J/Logback; не используйте `System.out`.
- Не добавляйте бинарные артефакты (изображения JPG/PNG, архивы и т.п.); диаграммы храните в текстовом формате (PlantUML, ASCII).

## Тестирование
- Фреймворки: JUnit 5 + Micronaut Test + Mockito.
- Именование: `*Test` (например, `VisitServiceTest`), структура пакетов как в `main`.
- Юнит‑тесты быстрые; Redis/Kafka — мокать где возможно.
- Запуск: `mvn -s .mvn/settings.xml test`. Порог покрытия не закреплён; целимся в ≥80% для изменённого кода.

## Коммиты и Pull Request’ы
- Предпочтительно Conventional Commits: `feat(scope): …`, `fix: …`, `refactor: …`, `docs: …`, `chore: …`. Допустимы RU/EN; кратко и в настоящем времени.
- В PR указывайте: что и зачем, связанный issue, план проверки (curl/сценарий), изменения конфигурации/переменных, потенциальные риски.
- CI собирает Docker‑образы из `Dockerfile`; не вносите ломающих изменений без явной мотивации и миграций.

## Безопасность и конфигурация
- Не коммитьте секреты. Для разработки — `.env.local`; для стендов — переменные CI/CD.
- Ключевые переменные: `OIDC_ISSUER_URL`, `OAUTH_CLIENT_ID/SECRET`, `REDIS_SERVER`, `KAFKA_SERVER`, `LOKI_SERVER`.
- Профиль Micronaut по умолчанию — `dev` (см. `Application.Configurer`); переопределение — `MICRONAUT_ENVIRONMENTS`.
