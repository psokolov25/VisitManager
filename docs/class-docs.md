# Документация классов

### ru.aritmos.api.ConfigurationController

REST API управления конфигурацией отделений.

#### Поля
- `BranchService branchService` — сервис управления отделениями.
- `VisitService visitService` — сервис визитов.
- `Configuration configuration` — сервис формирования конфигурации.

#### Методы
- `Map<String, Branch> update(Map<String, Branch> branchHashMap)` — обновление конфигурации отделений.
- `Map<String, Branch> update()` — обновление демо‑конфигурации.
- `void addUpdateService(String branchId, HashMap<String, Service> serviceHashMap, Boolean checkVisits)` — добавление или обновление услуг.
- `HashMap<String, String> getBreakReasons(String branchId)` — получение списка причин перерыва.
- `void deleteServices(String branchId, List<String> serviceIds, Boolean checkVisits)` — удаление услуг.
- `void addUpdateServicePoint(String branchId, HashMap<String, ServicePoint> servicePointHashMap, Boolean restoreVisit, Boolean restoreUser)` — добавление или обновление точек обслуживания.
- `void addUpdateServiceGroups(String branchId, HashMap<String, ServiceGroup> serviceGroupHashMap)` — добавление или обновление групп услуг.
- `void addUpdateSegmentationRules(String branchId, HashMap<String, SegmentationRuleData> segmentationRuleDataHashMap)` — добавление или обновление правил сегментации.
- `void deleteServicePoints(String branchId, List<String> servicePointIds)` — удаление точек обслуживания.
- `Optional<Branch> setAutoCallModeOfBranchOn(String branchId)` — включение автоматического вызова.
- `Optional<Branch> setAutoCallModeOfBranchOff(String branchId)` — выключение автоматического вызова.
- `void addUpdateQueues(String branchId, HashMap<String, Queue> queueHashMap, Boolean restoreVisits)` — добавление или обновление очередей.
- `void deleteQueues(String branchId, List<String> queueIds)` — удаление очередей.

### ru.aritmos.service.VisitService

Сервис работы с визитами.

#### Поля
- `KeyCloackClient keyCloackClient` — клиент Keycloak.
- `BranchService branchService` — сервис отделений. (Lombok: getter)
- `EventService eventService` — сервис событий.
- `DelayedEvents delayedEvents` — планировщик отложенных событий.
- `PrinterService printerService` — сервис печати талонов.
- `CallRule waitingTimeCallRule` — правило вызова по максимальному времени ожидания.
- `CallRule lifeTimeCallRule` — правило вызова по максимальному времени жизни визита.
- `SegmentationRule segmentationRule` — правило сегментации визитов.

#### Методы
- `void setWaitingTimeCallRule(CallRule callRule)` — инъекция правила вызова по максимальному времени ожидания.
- `void setLifeTimeCallRule(CallRule callRule)` — инъекция правила вызова по максимальному времени жизни визита.
- `Visit getVisit(String branchId, String visitId)` — получение визита по идентификаторам отделения и визита.
- `HashMap<String, ServicePoint> getStringServicePointHashMap(String branchId)` — доступные сервис‑пойнты отделения.
- `HashMap<String, ServicePoint> getServicePointHashMap(String branchId)` — все сервис‑пойнты отделения.
- `List<TinyClass> getWorkProfiles(String branchId)` — рабочие профили отделения.
- `List<User> getUsers(String branchId)` — пользователи отделения.
- `List<Visit> getVisits(String branchId, String queueId)` — визиты очереди, отсортированные по времени ожидания.
