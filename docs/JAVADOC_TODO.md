# Javadoc Strict — План доведения до 0 предупреждений

Этот файл — чек‑лист, чтобы продолжить работу, даже если пропадёт контекст.

## Как запускать строгую проверку

- Строгий Javadoc (JUnit тесты выключены):
  - PowerShell: `./mvnw -DskipTests -Ddoclint=all -DfailOnWarnings=true javadoc:javadoc`
  - С логом в файл: `./mvnw -DskipTests -Ddoclint=all -DfailOnWarnings=true javadoc:javadoc *> javadoc_strict.log`
- Быстрый просмотр предупреждений:
  - PowerShell: `Select-String -Path javadoc_strict.log -Pattern "warning:" | Select-Object -First 200 | % { $_.Line }`

## Шаблон краткого Javadoc (RU)

```java
/**
 * Краткое описание одной строкой.
 *
 * @param paramName описание параметра
 * @return описание возвращаемого значения
 * @throws SomeException когда/почему бросается (если метод объявляет исключение)
 */
```

Минимум — описать все public методы/конструкторы/поля, на которые ругается Javadoc, и выровнять `@param/@return/@throws` под фактические сигнатуры.

## Приоритет: где чаще всего остаются предупреждения

1) Контроллеры
- `src/main/java/ru/aritmos/api/EntrypointController.java`
  - Для `createVirtualVisit`, `createVisit` (обе перегрузки), `createVisitFromReception`:
    - Добавить `@param segmentationRuleId`/`@param sid` где есть в сигнатурах.
    - Добавить `@throws ru.aritmos.exceptions.SystemException` для методов, объявляющих это исключение.

- `src/main/java/ru/aritmos/api/ServicePointController.java`
  - У методов `visitTransferFromQueueToUserPool(...)` добавить недостающие `@param`:
    - `isAppend`, `transferTimeDelay`, `sid` (если есть в сигнатуре), `index` — в вариантах с позицией.

2) Сервисы
- `src/main/java/ru/aritmos/service/VisitService.java`
  - Группа методов: `createVisit(...)`, `createVisitFromReception(...)` (перегрузки),
    `createVisit2(...)` (включая вариант с `segmentationRuleId`),
    `createVisit2FromReception(...)` (перегрузки), `createVirtualVisit2(...)`,
    `visitTransfer(...)` (все перегрузки), `visitBack(...)`, `backCalledVisit(...)`,
    `getAllWorkingUsers(...)`, `visitEnd(...)`, `visitCall(...)` — выровнять `@param/@return/@throws`.

- `src/main/java/ru/aritmos/service/BranchService.java`
  - Для public методов добавить краткие описания и `@param/@return`.

3) Утилиты/интерфейсы/модели
- `src/main/java/ru/aritmos/service/rules/CallRule.java`, `CustomCallRule.java`,
  `src/main/java/ru/aritmos/service/rules/client/CallRuleClient.java` — краткие Javadoc на интерфейсы/классы и методы.
- `src/main/java/ru/aritmos/events/services/DelayedEvents.java` — Javadoc класса, конструктора и методов `delayedEventService(...)` (все перегрузки, `@param taskScheduler`).
- `src/main/java/ru/aritmos/events/clients/DataBusClient.java` — Javadoc интерфейса и метода `send(...)` (описать параметры/возврат).
- `src/main/java/ru/aritmos/events/model/Event.java`, `EventHandler.java` — Javadoc класса и публичных методов.
- `src/main/java/ru/aritmos/keycloack/service/EndSessionEndpointResolverReplacement.java` — Javadoc конструктора (`@param securityConfiguration`, `@param tokenResolver`).
- `src/main/java/ru/aritmos/model/DeliveredService.java` — Javadoc конструктора.
- `src/main/java/ru/aritmos/model/Entity.java` — Javadoc класса.
- `src/main/java/ru/aritmos/model/WorkProfile.java` — Javadoc для конструкторов (с `id` и без).
- `src/main/java/ru/aritmos/model/User.java` — Javadoc для конструкторов (c `id`, без `id`), геттеров `getName()`, `isOnBreak()`.
- `src/main/java/ru/aritmos/model/visit/VisitEvent.java` — следить, чтобы у статических методов были `@param visitEvent` и `@return`, и не было дублирующихся блоков Javadoc.

4) Исключения
- `src/main/java/ru/aritmos/exceptions/BusinessException.java`, `SystemException.java`
  - Класс‑описание и краткие Javadoc для публичных конструкторов/полей (`@param` и смысловые описания).

## Рекомендации по стилю
- Язык: русский, кратко и по делу.
- Не копировать сигнатуру в текст — описывать смысл.
- Всегда синхронизировать `@param` с фактическими именами параметров и порядком.
- Добавлять `@throws` только для действительно объявленных исключений.

## Контрольные команды
- Строгий прогон: `./mvnw -DskipTests -Ddoclint=all -DfailOnWarnings=true javadoc:javadoc`
- При большом объёме предупреждений писать лог: `*> javadoc_strict.log`, затем искать файлы:
  - `Select-String -Path javadoc_strict.log -Pattern "warning:" | % { $_.Line }`

После правок повторять прогон до «0 предупреждений».

