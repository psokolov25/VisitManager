# Документация классов

### ru.aritmos.Configurer

Конфигуратор контекста приложения (устанавливает профиль по умолчанию).

#### Методы
- `configure(@NonNull ApplicationContextBuilder builder)` — Установка профиля окружения по умолчанию. @param builder билдер контекста приложения

### ru.aritmos.api.EntrypointController

@author Pavel Sokolov REST API управления зоной ожидания

### ru.aritmos.api.KeyCloakController

REST API для операций авторизации в Keycloak.

### ru.aritmos.api.ManagementController

@author Pavel Sokolov REST API управления зоной ожидания

### ru.aritmos.api.ServicePointController

@author Pavel Sokolov REST API управления зоной ожидания

### ru.aritmos.clients.ConfigurationClient

HTTP‑клиент получения конфигурации отделений.

### ru.aritmos.clients.PrinterClient

HTTP‑клиент печати талонов.

### ru.aritmos.config.LocalNoDockerDataBusClientStub

Стаб HTTP‑клиента шины данных для профиля local-no-docker.

### ru.aritmos.config.LocalNoDockerKeycloakStub

Стаб-клиент Keycloak для окружения local-no-docker. Отключает реальные сетевые вызовы, возвращая предсказуемые данные.

### ru.aritmos.docs.CurlCheatsheetGenerator

Генератор HTML-шпаргалки по curl-примерам, извлечённым из JavaDoc контроллеров ru.aritmos.api. <p> Использует маркеры "Пример curl", <pre><code> и </code></pre> в комментариях.

#### Методы
- `escape(String s)` — Экранировать спецсимволы HTML. @param s исходная строка @return экранированная строка

### ru.aritmos.events.clients.DataBusClient

HTTP‑клиент для отправки событий на DataBus.

### ru.aritmos.events.model.ChangedObject

Модель изменённой сущности для событий шины данных.

### ru.aritmos.events.model.Event

Событие шины данных между сервисами.

### ru.aritmos.events.model.EventHandler

Обработчик событий шины данных.

### ru.aritmos.events.services.DelayedEvents

Планировщик отложенной отправки событий в шину.

#### Поля
- `TaskScheduler taskScheduler` — Планировщик задач.

#### Методы
- `delayedEventService(String destinationService, Boolean sendToOtherBus, Event event, Long delayInSeconds, EventService eventService)` — Отправить событие через заданную задержку. @param destinationService адресат события @param sendToOtherBus признак отправки во внешнюю шину @param event событие @param delayInSeconds задержка в секундах @param eventService сервис отправки событий
- `delayedEventService(List<String> destinationServices, Boolean sendToOtherBus, Event event, Long delayInSeconds, EventService eventService)` — Отправить событие нескольким адресатам через заданную задержку. @param destinationServices список адресатов @param sendToOtherBus признак отправки во внешнюю шину @param event событие @param delayInSeconds задержка в секундах @param eventService сервис отправки событий

### ru.aritmos.events.services.EventService

Сервис отправки событий в шину данных. <p>Пример использования:</p> <pre>{@code Event event = Event.builder() .eventType("PING") .eventDate(ZonedDateTime.now()) .build(); eventService.send("frontend", false, event); }</pre> <p>Диаграмма последовательности отправки события:</p> <pre> client -> EventService -> DataBusClient -> DataBus </pre> @see <a href="../../../../../../../docs/diagrams/event-service-sequence.svg">Диаграмма последовательности</a>

#### Методы
- `send(String destinationServices, Boolean sendToOtherBus, Event event)` — Клиент для отправки событий в шину данных. */ @Inject DataBusClient dataBusClient; /** Имя текущего сервиса-источника событий. */ @Value("${micronaut.application.name}") String applicationName; /** Конвертация даты типа {@link ZonedDateTime} в строку формата EEE, dd MMM yyyy HH:mm:ss zzz @param date дата типа {@link ZonedDateTime} @return строка даты формата EEE, dd MMM yyyy HH:mm:ss zzz / String getDateString(ZonedDateTime date) { DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); return format.format(date); } /** Отправка события на шину данных. @param destinationServices служба-адресат @param sendToOtherBus флаг переправки события в соседние шины данных @param event тело события
- `send(List<String> destinationServices, Boolean sendToOtherBus, Event event)` — Отправка события на шину данных. @param destinationServices список служб-адресатов @param sendToOtherBus флаг переправки события в соседние шины данных @param event тело события
- `sendChangedEvent(String destinationServices, Boolean sendToOtherBus, Object oldValue, Object newValue, Map<String, String> params, String action)` — Отправка события изменения сущности. @param destinationServices служба-адресат @param sendToOtherBus флаг переправки события в соседние шины данных @param oldValue старое значение @param newValue новое значение @param params дополнительные параметры @param action действие над событием
- `sendChangedEvent(List<String> destinationServices, Boolean sendToOtherBus, Object oldValue, Object newValue, Map<String, String> params, String action)` — Отправка события изменения сущности. @param destinationServices список служб-адресатов @param sendToOtherBus флаг переправки события в соседние шины данных @param oldValue старое значение @param newValue новое значение @param params дополнительные параметры @param action действие над событием

### ru.aritmos.events.services.EventTask

Асинхронная задача отправки события в шину.

#### Поля
- `String destinationService` — Получатель события.
- `Boolean sendToOtherBus` — Отправлять ли событие в соседнюю шину.
- `Event event` — Событие.
- `EventService eventService` — Сервис отправки событий.

### ru.aritmos.events.services.MultiserviceEventTask

Асинхронная задача отправки события нескольким адресатам.

#### Поля
- `List<String> destinationServices` — Получатели события.
- `Boolean sendToOtherBus` — Отправлять ли событие в соседнюю шину.
- `Event event` — Событие.
- `EventService eventService` — Сервис отправки событий.

### ru.aritmos.exceptions.BusinessException

Исключение уровня бизнес‑логики с публикацией события об ошибке.

### ru.aritmos.exceptions.SystemException

Системное исключение с публикацией события об ошибке.

### ru.aritmos.handlers.EventHandlerContext

Контекст регистрации обработчиков событий и их привязки к KafkaListener.

### ru.aritmos.keycloack.customsecurity.CustomSecurityRule

Реализация ручной проверки доступа к ресурсу

#### Методы
- `getOrder()` — Возвращение порядкового номера проверки, обеспечивающего порядок применения правил проверки @return порядковый номер
- `check(@Nullable HttpRequest<Object> request, @Nullable Authentication authentication)` — Реализация логики проверки доступа к ресурсу (на пример разрешение ресурса по лицензионному соглашения) @param request данные рест запроса, который проверяется на соответствие требованиям @param authentication данные пользователя @return результат проверки SecurityRuleResult.UNKNOWN - результат игнорируется и переходит на следующее правило проверки SecurityRuleResult.REJECTED - результат отрицательный, передается отказ от доступа к ресурсу SecurityRuleResult.ALLOWED - результат положительный, передается разрешение доступа к ресурсу

### ru.aritmos.keycloack.model.Credentials

Учетные данные пользователя для авторизации.

### ru.aritmos.keycloack.model.KeyCloackUser

Сокращённая информация о пользователе из Keycloak.

#### Поля
- `String email` — Электронная почта пользователя. (Lombok: getter)
- `String name` — Отображаемое имя пользователя. (Lombok: getter)
- `String preferred_username` — Предпочитаемое имя пользователя (username). (Lombok: getter)
- `List<String> roles` — Роли в realm. (Lombok: getter)
- `HashMap<String, Object> attributes` — Произвольные атрибуты пользователя. (Lombok: getter)
- `List<String> groups` — Список групп, в которых состоит пользователь. (Lombok: getter)

### ru.aritmos.keycloack.service.EndSessionEndpointResolverReplacement

Реализация резолвера URL завершения сессии с учётом Okta/Keycloak.

#### Поля
- `BeanContext beanContext` — Контекст бинов Micronaut.
- `TokenResolver tokenResolver` — Резолвер токена в запросе.
- `SecurityConfiguration securityConfiguration` — Конфигурация безопасности.

### ru.aritmos.keycloack.service.KeyCloackClient

Клиент для взаимодействия с Keycloak: пользователи, группы, сессии.

#### Методы
- `getAuthzClient(String secret, String keycloakUrl, String realm, String clientId)` — Сервис отправки событий. */ @Inject EventService eventService; /** Технический логин для доступа к Keycloak. */ @Property(name = "micronaut.security.oauth2.clients.keycloak.techlogin") String techlogin; /** Технический пароль для доступа к Keycloak. */ @Property(name = "micronaut.security.oauth2.clients.keycloak.techpassword") String techpassword; /** Идентификатор клиента Keycloak. */ @Property(name = "micronaut.security.oauth2.clients.keycloak.client-id") String clientId; /** Секрет клиента Keycloak. */ @Property(name = "micronaut.security.oauth2.clients.keycloak.client-secret") String secret; @Getter /** Realm Keycloak, в котором выполняются операции. */ @Property(name = "micronaut.security.oauth2.clients.keycloak.realm") String realm; /** URL сервера Keycloak. */ @Property(name = "micronaut.security.oauth2.clients.keycloak.keycloakurl") String keycloakUrl; /** Экземпляр клиента Keycloak (ленивая инициализация). */ Keycloak keycloak; //  private static void keycloakLogout(Keycloak keycloak) { //    if (keycloak.tokenManager() != null) { //      try { //        keycloak.tokenManager().logout(); //      } catch (Exception e) { //        log.warn(e.getMessage()); //      } //    } //    keycloak.close(); //  } /** Создать клиент авторизации Keycloak. @param secret секрет клиента @param keycloakUrl URL сервера Keycloak @param realm realm @param clientId идентификатор клиента @return клиент авторизации
- `getAllBranchesByRegionName(String regionName, Keycloak keycloak)` — Получить все отделения по имени региона. @param regionName имя региона @param keycloak клиент Keycloak @return список групп
- `getBranchPathByBranchPrefix(String regionName, String prefix)` — Получить путь группы отделения по префиксу. @param regionName имя региона @param prefix префикс отделения @return путь группы
- `getAllBranchesByRegionId(String regionId, Keycloak keycloak)` — Получить все отделения рекурсивно по идентификатору региона. @param regionId идентификатор региона @param keycloak клиент Keycloak @return список групп
- `getAllBranchesOfUser(String username)` — Получение всех отделений, к которым пользователь имеет доступ @param username имя пользователя @return список групп формате keycloak GroupRepresentation
- `getUserBySid(String sid)` — Найти пользователя по идентификатору сессии. @param sid идентификатор сессии @return пользователь, если найден
- `isUserModuleTypeByUserName(String userName, String type)` — Проверить принадлежность пользователя к типу модуля. @param userName логин пользователя @param type тип модуля (например, admin) @return признак принадлежности
- `Auth(@Body Credentials credentials)` — Авторизация пользователя в Keycloak @param credentials логин и пароль пользователя @return - данные авторизации (токен, токен обновления и т д)
- `getUserInfo(String userName)` — Получить информацию о пользователе. @param userName логин @return пользователь, если найден
- `getUserSessionByLogin(UserRepresentation user)` — Получить информацию о сессии пользователя. @param user пользователь Keycloak @return сессия пользователя, если есть
- `userLogout(@PathVariable String login, Boolean isForced, String reason)` — Выход сотрудника. @param login логин сотрудника @param isForced принудительный выход @param reason причина
- `getKeycloak()` — Получить/инициализировать клиент Keycloak. @return клиент Keycloak

### ru.aritmos.keycloack.service.UserMapper

Класс отвечает за маппинг свойств пользователя в Keycloak и стандартных свойств OpenId авторизации

### ru.aritmos.model.BasedService

Базовая услуга

### ru.aritmos.model.Branch

Отделение

#### Методы
- `incrementTicketCounter(Queue queue)` — Инкремент счётчика талонов очереди. @param queue очередь @return новое значение счётчика или -1, если очередь не принадлежит отделению
- `getAllVisits()` — Получение перечня всех визитов отделение, с ключом - идентификатором визита @return перечень визитов с ключом - идентификатором визита
- `getAllVisitsList()` — Получение перечня всех визитов отделение, с ключом - идентификатором визита @return перечень визитов с ключом - идентификатором визита
- `getVisitsByStatus(List<String> statuses)` — Получение перечня всех визитов отделение, с ключом - идентификатором визита с фильтрацией по статусам визита @param statuses список статусов @return перечень визитов с ключом - идентификатором визита
- `closeServicePoint(String servicePointId, EventService eventService, VisitService visitService, Boolean withLogout, Boolean isBreak, String breakReason, Boolean isForced, String reason)` — Закрытие точки обслуживания. @param servicePointId идентификатор точки обслуживания @param eventService служба рассылки событий @param visitService сервис визитов @param withLogout флаг закрытия сессии сотрудника @param isBreak флаг начала перерыва @param breakReason причина перерыва @param isForced принудительное закрытие визита @param reason причина принудительного закрытия
- `updateVisit(Visit visit, EventService eventService, String action, VisitService visitService)` — Обновление визита и рассылка события с произвольным действием. @param visit визит @param eventService сервис событий @param action действие (будет добавлено к префиксу VISIT_) @param visitService сервис визитов
- `updateVisit(Visit visit, EventService eventService, VisitEvent visitEvent, VisitService visitService, Boolean isToStart)` — Обновление визита по событию с выбором позиции начала/конца. @param visit визит @param eventService сервис событий @param visitEvent событие визита @param visitService сервис визитов @param isToStart поместить визит в начало (true) или в конец (false)
- `updateVisit(Visit visit, EventService eventService, VisitEvent visitEvent, VisitService visitService, Integer index)` — Обновление визита по событию с указанием позиции. @param visit визит @param eventService сервис событий @param visitEvent событие визита @param visitService сервис визитов @param index индекс вставки (или -1 для добавления в конец)
- `updateVisit(Visit visit, EventService eventService, VisitEvent visitEvent, VisitService visitService)` — Обновление визита по событию (перегрузка без дополнительных параметров). @param visit визит @param eventService сервис событий @param visitEvent событие визита @param visitService сервис визитов
- `addUpdateService(HashMap<String, Service> serviceHashMap, EventService eventService, Boolean checkVisits, VisitService visitService)` — Добавление или обновление услуг отделения. @param serviceHashMap карта услуг (id -> услуга) @param eventService сервис событий @param checkVisits учитывать активные визиты при изменении услуг @param visitService сервис визитов
- `deleteServices(List<String> serviceIds, EventService eventService, Boolean checkVisits, VisitService visitService)` — Удаление услуг отделения. @param serviceIds список идентификаторов услуг @param eventService сервис событий @param checkVisits учитывать активные визиты при удалении услуг @param visitService сервис визитов
- `adUpdateServiceGroups(HashMap<String, ServiceGroup> serviceGroupHashMap, EventService eventService)` — Добавление/обновление групп услуг с установкой связей услуг. @param serviceGroupHashMap карта групп услуг @param eventService сервис событий
- `addUpdateServicePoint(HashMap<String, ServicePoint> servicePointHashMap, Boolean restoreVisit, Boolean restoreUser, EventService eventService)` — Добавление/обновление точек обслуживания. @param servicePointHashMap карта точек обслуживания @param restoreVisit восстановить визит на точке @param restoreUser восстановить пользователя на точке @param eventService сервис событий
- `deleteServicePoints(List<String> servicePointIds, EventService eventService)` — Удаление точек обслуживания. @param servicePointIds список идентификаторов точек @param eventService сервис событий
- `addUpdateQueues(HashMap<String, Queue> queueHashMap, Boolean restoreVisits, EventService eventService)` — Добавление/обновление очередей отделения. @param queueHashMap карта очередей @param restoreVisits восстановить связанные визиты @param eventService сервис событий
- `deleteQueues(List<String> queueIds, EventService eventService)` — Удаление очередей отделения. @param queueIds список идентификаторов очередей @param eventService сервис событий
- `adUpdateSegmentRules(HashMap<String, SegmentationRuleData> segmentationRuleDataHashMap, EventService eventService)` — Добавление/обновление правил сегментации отделения. @param segmentationRuleDataHashMap карта правил сегментации @param eventService сервис событий

### ru.aritmos.model.BranchEntity

Сущность отделения

### ru.aritmos.model.BranchEntityWithVisits

Сущность отделения, содержащая перечень визитов

### ru.aritmos.model.DeliveredService

Оказанная услуга

### ru.aritmos.model.Entity

Базовая сущность с идентификатором и именем.

### ru.aritmos.model.EntryPoint

Точка входа

### ru.aritmos.model.GroovyScript

Модель скрипта Groovy для сегментации и вызова.

### ru.aritmos.model.Mark

Пометка о визите

### ru.aritmos.model.Outcome

Итог обслуживания

### ru.aritmos.model.Queue

Очередь

### ru.aritmos.model.RealmAccess

Доступы пользователя: роли, группы и модули.

### ru.aritmos.model.Reception

Приёмная.

### ru.aritmos.model.ReceptionSession

Сеанс работы приёмной.

### ru.aritmos.model.SegmentationRuleData

Параметры правила сегментации визитов.

### ru.aritmos.model.Service

Услуга

### ru.aritmos.model.ServiceGroup

Группа услуг отделения.

### ru.aritmos.model.ServicePoint

Точка обслуживания

### ru.aritmos.model.Token

Информация о токене авторизации.

### ru.aritmos.model.User

Пользователь

#### Методы
- `getName()` — Имя пользователя. @return имя
- `isOnBreak()` — Признак, что пользователь сейчас на перерыве. @return true, если перерыв начат и не завершён

### ru.aritmos.model.UserInfo

Информация о пользователе.

### ru.aritmos.model.UserSession

Информация о сессии пользователя.

### ru.aritmos.model.UserToken

Пользователь и связанные с ним токены.

### ru.aritmos.model.VisitParameters

Параметры визита.

### ru.aritmos.model.WorkProfile

Рабочий профиль

### ru.aritmos.model.keycloak.ClientAccess

Доступы клиента Keycloak: роли по клиентам.

### ru.aritmos.model.keycloak.ModuleRole

ModuleRole

#### Поля
- `String name` — Имя модуля который нужно проверить на доступ
- `String role` — Роль пользователя котороя должна предоставить доступ к модулю

#### Методы
- `name(String name)` — Установить имя модуля (chainable). @param name имя модуля @return текущий экземпляр ModuleRole
- `role(String role)` — Установить требуемую роль (chainable). @param role роль @return текущий экземпляр ModuleRole

### ru.aritmos.model.keycloak.ModuleRoleAccess

ModuleRoleAccess

#### Поля
- `String name` — Имя модуля который нужно проверить на доступ
- `String role` — Роль пользователя котороя должна предоставить доступ к модулю
- `Boolean access` — True предоставить доступ к модулю, false отказать в доступе

#### Методы
- `name(String name)` — Установить имя модуля (chainable). @param name имя модуля @return текущий экземпляр ModuleRoleAccess
- `role(String role)` — Установить роль (chainable). @param role роль @return текущий экземпляр ModuleRoleAccess
- `access(Boolean access)` — Установить доступ (chainable). @param access доступ @return текущий экземпляр ModuleRoleAccess

### ru.aritmos.model.keycloak.RealmAccess

Доступы (роли) пользователя в реалме Keycloak.

### ru.aritmos.model.keycloak.TinyUserInfo

TinyUserInfo

#### Поля
- `String name` — полное имя залогиненного пользователя
- `String email` — Информация о пользователе(любой необходимый текст) */ @NotNull @Schema( name = "description", example = "super admin", description = "Информация о пользователе(любой необходимый текст)", requiredMode = Schema.RequiredMode.REQUIRED) @JsonProperty(JSON_PROPERTY_DESCRIPTION) @JsonInclude(JsonInclude.Include.NON_NULL) private String description; /** Почтовый ящик
- `String login` — Логин пользователя

#### Методы
- `name(String name)` — Установить имя (chainable). @param name имя пользователя @return текущий экземпляр TinyUserInfo
- `description(String description)` — Установить описание (chainable). @param description описание @return текущий экземпляр TinyUserInfo
- `email(String email)` — Установить email (chainable). @param email почта @return текущий экземпляр TinyUserInfo
- `login(String login)` — Установить логин (chainable). @param login логин @return текущий экземпляр TinyUserInfo

### ru.aritmos.model.keycloak.Token

Токен авторизации Keycloak.

### ru.aritmos.model.keycloak.UserInfo

Информация о пользователе из Keycloak.

### ru.aritmos.model.keycloak.UserSession

Информация о сессии пользователя.

### ru.aritmos.model.keycloak.UserToken

Пользователь и связанные с ним токены.

### ru.aritmos.model.tiny.TinyClass

Класс для формирования списков объектов

#### Поля
- `String id` — Идентификатора объекта (Lombok: getter)
- `String name` — Название объекта (Lombok: getter)

### ru.aritmos.model.tiny.TinyServicePoint

Сокращенный вариант класса рабочей станции

### ru.aritmos.model.tiny.TinyVisit

Сокращенное представление визита

#### Методы
- `getWaitingTime()` — Дата и время создания визита */ ZonedDateTime createDate; /** Идентификатор визита */ String id; /** Номер талона */ String ticketId; /** Текущая услуга */ Service currentService; /** Дата и время перевода */ ZonedDateTime transferDate; /** Время ожидания в секундах */ Long waitingTime; /** Суммарное время ожидания в секундах. */ Long totalWaitingTime; /** Расчёт текущего времени ожидания в секундах. @return время ожидания в секундах
- `getTotalWaitingTime()` — Расчёт суммарного времени ожидания в секундах. @return суммарное время ожидания

### ru.aritmos.model.visit.TransactionCompletionStatus

Перечень состояний завершения транзакции визита.

### ru.aritmos.model.visit.Visit

Визит

#### Поля
- `Long returnTimeDelay` — Идентификатор визита */ String id; /** Статус визита */ String status; /** Талон */ String ticket; /** Идентификатор отделения */ String branchId; /** Название отделения */ String branchName; /** Префикс отделения */ @JsonInclude(JsonInclude.Include.ALWAYS) String branchPrefix; /** Путь к отделению */ @JsonInclude(JsonInclude.Include.ALWAYS) String branchPath; /** Дата создания визита */ ZonedDateTime createDateTime; /** Дата перевода */ @Schema(nullable = true) ZonedDateTime transferDateTime; /** Дата возвращения */ @Schema(nullable = true) ZonedDateTime returnDateTime; /** Дата вызова */ @Schema(nullable = true) ZonedDateTime callDateTime; /** Дата начала обслуживания */ @Schema(nullable = true) ZonedDateTime startServingDateTime; /** Дата завершения обслуживания */ @Schema(nullable = true) ZonedDateTime servedDateTime; /** Дата завершения визита */ @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(nullable = true) ZonedDateTime endDateTime; /** Идентификатор точки обслуживания */ @Schema(nullable = true) String servicePointId; /** Логин вызвавшего сотрудника */ @Schema(nullable = true) String userName; /** Id вызвавшего сотрудника */ @Schema(nullable = true) String userId; /** Id сотрудника, в пуле которого располагается визит */ @Schema(nullable = true) String poolUserId; /** Id точка обслуживания, в пуле которого располагается визит */ @Schema(nullable = true) String poolServicePointId; /** Массив не обслуженных услуг */ List<Service> unservedServices; /** Обслуженные услуги */ @JsonInclude(JsonInclude.Include.NON_NULL) List<Service> servedServices; /** Марки визита */ @JsonInclude(JsonInclude.Include.NON_NULL) List<Mark> visitMarks; /** Заметки визита */ @JsonInclude(JsonInclude.Include.NON_NULL) List<Mark> visitNotes; /** Время ожидания в последней очереди в секундах */ Long waitingTime; /** Время прошедшее от возвращения в очередь */ @Schema(nullable = true) Long returningTime; /** Время прошедшее от перевода в очередь */ @Schema(nullable = true) Long transferingTime; /** Общее время с создания визита в секундах */ @Schema(nullable = true) Long visitLifeTime; /** Время обслуживания в секундах */ @Schema(nullable = true) Long servingTime; /** Текущая услуга */ Service currentService; /** Дополнительные параметры визита */ @JsonInclude(JsonInclude.Include.NON_NULL) HashMap<String, String> parameterMap; /** Признак печати талона */ Boolean printTicket; /** Точка создания визита талона */ EntryPoint entryPoint; /** Идентификатор очереди */ @Schema(nullable = true) String queueId; /** Массив событий */ List<VisitEvent> visitEvents; /** История событий визита. */ List<VisitEventInformation> events; /** Лимит ожидания после возвращения визита в очередь или пул сотрудника или точки обслуживания (Lombok: getter)
- `Long transferTimeDelay` — Лимит ожидания после перевода визита в очередь или пул сотрудника или точки обслуживания (Lombok: getter)

#### Методы
- `getWaitingTime()` — Расчёт текущего времени ожидания в секундах. @return время ожидания в секундах
- `getReturningTime()` — Разница между текущим временем и временем возвращения, если времени возвращения нет - возвращаем 0 @return время ожидания с момента возвращения
- `getTransferingTime()` — Разница между текущим временем и временем перевода, если времени перевода нет - возвращаем 0 @return время ожидания с момента перевода
- `getVisitLifeTime()` — Время жизни визита. @return общее время от создания до завершения
- `getServingTime()` — Время обслуживания визита. @return длительность обслуживания в секундах

### ru.aritmos.model.visit.VisitEvent

Перечень событий жизненного цикла визита.

#### Поля
- `ZonedDateTime dateTime` — Дополнительные параметры события. */ @Getter final Map<String, String> parameters = new HashMap<>(); /** Время наступления события.

#### Методы
- `isNewOfTransaction(VisitEvent visitEvent)` — Проверка: является ли событие началом новой транзакции. @param visitEvent событие визита @return истина, если после события начинается новая транзакция
- `isIgnoredInStat(VisitEvent visitEvent)` — Проверка, исключается ли событие из статистики. @param visitEvent событие визита @return истина, если событие не отправляется в статистику
- `isFrontEndEvent(VisitEvent visitEvent)` — Проверка необходимости отправки события на фронтенд. @param visitEvent событие визита @return истина, если событие отправляется на фронтенд
- `getStatus(VisitEvent visitEvent)` — Получить статус завершения транзакции для события, если применимо. @param visitEvent событие визита @return статус завершения или null
- `canBeNext(VisitEvent next)` — Проверка на возможность следующего события после текущего. @param next следующее событие @return истина, если событие допустимо
- `getState()` — Получение состояния визита, соответствующего текущему событию. @return состояние визита

### ru.aritmos.model.visit.VisitEventInformation

Информация о событии визита.

### ru.aritmos.model.visit.VisitState

Перечень возможных состояний визита.

### ru.aritmos.service.BranchService

Служба отвечающая за работу с отделениями

#### Методы
- `getBranches()` — Получение списка отделений (без детальной информации). @return карта отделений (id -> отделение)
- `getDetailedBranches()` — Получение списка отделений с детальной информацией. @return карта отделений (id -> отделение)
- `add(String key, Branch value)` — Создание или обновление отделения. Отправляет событие об изменении сущности. @param key идентификатор отделения @param value модель отделения @return сохранённое отделение
- `branchExists(String key)` — Проверка на наличие отделения в списке отделений по ключу @param key ключ отделения @return флаг существования отделения
- `delete(String key, VisitService visitService)` — Удаление отделения. @param key идентификатор отделения @param visitService сервис визитов (для корректного закрытия точек) @throws BusinessException если отделение не найдено
- `updateVisit(Visit visit, String action, VisitService visitService)` — Обновление визита и рассылка соответствующего события. @param visit визит @param action действие для имени события @param visitService сервис визитов
- `updateVisit(Visit visit, VisitEvent visitEvent, VisitService visitService)` — Обновление визита по событию. @param visit визит @param visitEvent событие визита @param visitService сервис визитов
- `updateVisit(Visit visit, VisitEvent visitEvent, VisitService visitService, Boolean isToStart)` — Обновление визита по событию с возможностью задать начало списка. @param visit визит @param visitEvent событие визита @param visitService сервис визитов @param isToStart поместить визит в начало списка очереди/пула
- `updateVisit(Visit visit, VisitEvent visitEvent, VisitService visitService, Integer index)` — Обновление визита по событию с указанием позиции. @param visit визит @param visitEvent событие визита @param visitService сервис визитов @param index позиция вставки (или -1 для добавления в конец)
- `changeUserWorkProfileInServicePoint(String branchId, String servicePointId, String workProfileId)` — Смена рабочего профиля пользователя на точке обслуживания. @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param workProfileId идентификатор рабочего профиля @return пользователь с обновлённым рабочим профилем @throws BusinessException если профиль, точка или пользователь не найдены
- `checkServicePointChange(String servicePointId, User user, String oldServicePointId)` — Отправка события о смене точки обслуживания пользователем. @param servicePointId новая точка обслуживания @param user пользователь @param oldServicePointId предыдущая точка обслуживания
- `checkWorkProfileChange(String workProfileId, User user, String oldWorkProfileId)` — Отправка события о смене рабочего профиля пользователем. @param workProfileId новый рабочий профиль @param user пользователь @param oldWorkProfileId предыдущий рабочий профиль
- `closeServicePoint(String branchId, String servicePointId, VisitService visitService, Boolean isWithLogout, Boolean isBreak, String breakReason, Boolean isForced, String reason)` — Закрытие точки обслуживания. @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param visitService сервис визитов @param isWithLogout флаг завершения сессии пользователя @param isBreak флаг начала перерыва @param breakReason причина перерыва @param isForced принудительное закрытие визита @param reason причина принудительного закрытия
- `getUsers(String branchId)` — Получение пользователей отделения. @param branchId идентификатор отделения @return карта пользователей (логин -> пользователь)
- `incrementTicketCounter(String branchId, Queue queue)` — Инкремент счётчика талонов очереди. @param branchId идентификатор отделения @param queue очередь @return новое значение счётчика либо -1, если очередь не найдена
- `addUpdateService(String branchId, HashMap<String, Service> serviceHashMap, Boolean checkVisits, VisitService visitService)` — Добавление/обновление услуг отделения. @param branchId идентификатор отделения @param serviceHashMap карта услуг (id -> услуга) @param checkVisits учитывать активные визиты при изменении @param visitService сервис визитов
- `deleteServices(String branchId, List<String> serviceIds, Boolean checkVisits, VisitService visitService)` — Удаление услуг отделения. @param branchId идентификатор отделения @param serviceIds список идентификаторов услуг @param checkVisits учитывать активные визиты при удалении @param visitService сервис визитов
- `addUpdateServicePoint(String branchId, HashMap<String, ServicePoint> servicePointHashMap, Boolean restoreVisit, Boolean restoreUser)` — Добавление/обновление точек обслуживания. @param branchId идентификатор отделения @param servicePointHashMap карта точек обслуживания @param restoreVisit восстановить визит на точке @param restoreUser восстановить пользователя на точке
- `addUpdateServiceGroups(String branchId, HashMap<String, ServiceGroup> serviceGroupsHashMap)` — Добавление/обновление групп услуг. @param branchId идентификатор отделения @param serviceGroupsHashMap карта групп услуг
- `deleteServicePoints(String branchId, List<String> servicePointIds)` — Удаление точек обслуживания. @param branchId идентификатор отделения @param servicePointIds список идентификаторов точек обслуживания
- `addUpdateQueues(String branchId, HashMap<String, Queue> queueHashMap, Boolean restoreVisits)` — Добавление/обновление очередей. @param branchId идентификатор отделения @param queueHashMap карта очередей @param restoreVisits восстановить визиты очередей
- `deleteQueues(String branchId, List<String> queueIds)` — Удаление очередей. @param branchId идентификатор отделения @param queueIds список идентификаторов очередей
- `addUpdateSegmentationRules(String branchId, HashMap<String, SegmentationRuleData> segmentationRuleDataHashMap)` — Добавление/обновление правил сегментации. @param branchId идентификатор отделения @param segmentationRuleDataHashMap карта правил сегментации
- `getServicesByWorkProfileId(String branchId, String workProfileId)` — Получение услуг соответствующего рабочего профиля @param branchId идентификатор отделения @param workProfileId идентификатор рабочего профиля @return список услуг
- `getServicesByQueueId(String branchId, String queueId)` — Получение услуг соответствующей очереди @param branchId идентификатор отделения @param queueId идентификатор очереди @return список услуг
- `getDeliveredServicesByBranchId(String branchId)` — Получение перечня возможных оказанных услуг отделения. @param branchId идентификатор отделения @return список возможных оказанных услуг

### ru.aritmos.service.Configuration

Сервис формирования и публикации конфигурации отделений.

#### Методы
- `createBranchConfiguration(Map<String, Branch> branchHashMap)` — Сервис управления отделениями. */ @Inject BranchService branchService; /** Клиент Keycloak. */ @Inject KeyCloackClient keyCloackClient; /** Сервис визитов. */ @Inject VisitService visitService; /** Сервис отправки событий. */ @Inject EventService eventService; /** Создать и опубликовать конфигурацию отделений. @param branchHashMap карта отделений (id -> отделение) @return карта детализированных отделений
- `createDemoBranch()` — Создать демонстрационную конфигурацию отделений. @return карта отделений (демо)

### ru.aritmos.service.GroovyScriptService

Сервис выполнения сценариев Groovy для пользовательских правил.

#### Методы
- `Execute(GroovyScript groovyScript)` — Выполнить скрипт Groovy с передачей входных параметров и сбором результатов. @param groovyScript объект скрипта и его параметров

### ru.aritmos.service.PrinterService

Сервис печати талонов.

#### Методы
- `print(String id, Visit visit)` — Отправить визит на печать. @param id идентификатор принтера @param visit визит

### ru.aritmos.service.Services

Класс отвечающий за работу с услугами

#### Методы
- `getAllServices(String branchId)` — Получение всех услуг отделения @param branchId идентификатор отделения @return список услуг
- `getAllAvailableServices(String branchId)` — Получение всех доступных на данный момент услуг @param branchId идентификатор отделения @return список доступных услуг

### ru.aritmos.service.VisitService

Сервис операций с визитами: создание, перевод, события и печать талонов. <p>Пример использования:</p> <pre>{@code VisitParameters params = new VisitParameters(); params.setServiceIds(List.of("service1")); Visit visit = visitService.createVisit("branch1", "entry1", params, true); }</pre> <p>Диаграмма взаимодействия:</p> <pre> request -> VisitService -> {BranchService, EventService, PrinterService} </pre> @see <a href="../../../../../../docs/diagrams/visit-service-overview.svg">Диаграмма взаимодействия</a>

#### Поля
- `KeyCloackClient keyCloackClient` — Клиент Keycloak для получения сведений о пользователях.

#### Методы
- `getVisit(String branchId, String visitId)` — Возвращает визит по идентификатору отделения и визита. @param branchId идентификатор отделения @param visitId идентификатор визита @return визит
- `getVisits(String branchId, String queueId)` — Возвращает визиты, содержащиеся в указанной очереди. Визиты сортируются по времени ожидания, от большего к меньшему. @param branchId идентификатор отделения @param queueId идентификатор очереди @return список визитов
- `getVisits(String branchId, String queueId, Long limit)` — Возвращает ограниченный список визитов указанной очереди. Если визитов меньше значения {@code limit}, возвращаются все. Визиты сортируются по времени ожидания, от большего к меньшему. @param branchId идентификатор отделения @param queueId идентификатор очереди @param limit максимальное количество визитов @return список визитов
- `createVisit(String branchId, String entryPointId, VisitParameters visitParameters, Boolean printTicket, String segmentationRuleId)` — Создание визита с указанием правила сегментации @param branchId идентификатор отделения @param entryPointId идентификатор энтри поинта @param visitParameters передаваемые список услуг и дополнительные параметры визита @param printTicket флаг печати талона @param segmentationRuleId идентификатор правила вызова @return созданный визит
- `createVisitFromReception(String branchId, String printerId, VisitParameters visitParameters, Boolean printTicket, String segmentationRuleId, String sid)` — Создание визита из приемной с передачей идентификатора используемого правила сегментации @param branchId идентификатор отделения @param printerId идентификатор энтри поинта @param visitParameters передаваемые список услуг и дополнительные параметры визита @param printTicket флаг печати талона @param segmentationRuleId идентификатор правила сегментации @param sid идентификатор сессии сотрудника (cookie sid) @return созданный визит
- `addEvent(Visit visit, VisitEvent event, EventService eventService)` — Добавление события в визит @param visit визит @param event событие @param eventService служба отправки события визита на шину данных
- `getQueues(String branchId, String servicePointId)` — Получение списка очередей @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return опциональный список очередей
- `getAllVisits(String branchId)` — @param branchId идентификатор отделения @return список визитов
- `getVisitsByStatuses(String branchId, List<String> statuses)` — Получение списка визитов с фильтрацией по их статусам. Выводятся визиты, чей статус входит в передаваемым в теле запроса списком статусов. @param branchId идентификатор отделения @param statuses список статусов, по котором должны быть отфильтрованы визиты @return список визитов
- `createVisit2(String branchId, String entryPointId, ArrayList<Service> services, HashMap<String, String> parametersMap, Boolean printTicket, String segmentationRuleId)` — Создание визита @param branchId идентификатор отделения @param entryPointId идентификатор энтри поинта @param services список услуг @param parametersMap параметры визита @param printTicket флаг печати талона @param segmentationRuleId идентификатор правила сегментации @return визит
- `createVisit2FromReception(String branchId, String printerId, ArrayList<Service> services, HashMap<String, String> parametersMap, Boolean printTicket, String segmentationRuleId, String sid)` — Создание визита из приемной @param branchId идентификатор отделения @param printerId идентификатор принтера @param services список услуг @param parametersMap параметры визита @param printTicket флаг печати талона @param segmentationRuleId идентификатор правила сегментации @param sid идентификатор сессии сотрудника (cookie sid) @return визит
- `getDeliveredServices(String branchId, String servicePointId)` — Получение списка предоставленных фактических услуг у текущей услугу текущего визита в указанной точке обслуживания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return оказанные услуги
- `addDeliveredService(String branchId, String servicePointId, String deliveredServiceId)` — Добавление фактической услуги @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param deliveredServiceId идентификатор фактической услуги @return визит
- `deleteDeliveredService(String branchId, String servicePointId, String deliveredServiceId)` — Удаление фактической услуги @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param deliveredServiceId идентификатор фактической услуги @return визит
- `addService(String branchId, String servicePointId, String serviceId)` — Добавление услуги @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param serviceId идентификатор услуги @return визит
- `addMark(String branchId, String servicePointId, Mark mark)` — Добавление текстовой пометки в визит @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param mark пометка @return визит
- `deleteMark(String branchId, String servicePointId, Mark mark)` — Удаление текстовой пометки в визите @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param mark пометка @return визит
- `deleteMark(String branchId, String servicePointId, String markId)` — Удаление заметки в визите @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param markId идентификатор заметки @return визит
- `getMarks(String branchId, String visitId)` — Просмотр марков в визите @param branchId идентификатор отделения @param visitId идентификатор точки обслуживания * @return визит
- `addMark(String branchId, String servicePointId, String markId)` — Добавление марков в визите @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param markId идентификатор заметки @return визит
- `addOutcomeService(String branchId, String servicePointId, String outcomeId)` — Добавление итога услуги @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param outcomeId идентификатор итога услуги @return визит
- `addOutcomeOfDeliveredService(String branchId, String servicePointId, String deliveredServiceId, String outcomeId)` — Добавление итога фактической услуги @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param deliveredServiceId идентификатор фактической услуги @param outcomeId идентификатор итога услуги @return визит
- `deleteOutcomeDeliveredService(String branchId, String servicePointId, String deliveredServiceId)` — Удаление итога фактической услуги @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param deliveredServiceId идентификатор фактической услуги @return визит
- `deleteOutcomeService(String branchId, String servicePointId, String serviceId)` — Удаление итога услуги @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param serviceId идентификатор услуги @return Услуга
- `backCalledVisit(String branchId, String visitId, Long returnTimeDelay)` — Возвращение вызванного визита в очередь. @param branchId идентификатор отделения @param visitId идентификатор визита @param returnTimeDelay задержка возвращения (сек) @return визит
- `stopServingAndBackToQueue(String branchId, String servicePointId, Long returnTimeDelay)` — Остановка обслуживания визита в точке обслуживания и возвращение визита в очередь с задержкой @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param returnTimeDelay задержка возвращения в секундах @return визит
- `visitTransfer(String branchId, String servicePointId, String queueId, Boolean isAppend, Long transferTimeDelay)` — Перевод визита в очередь. @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param queueId идентификатор очереди @param isAppend вставка в конец (true) или начало (false) @param transferTimeDelay задержка перевода в секундах @return визит
- `visitBack(String branchId, String servicePointId, String queueId, Long returnTimeDelay)` — Возвращение визита в очередь @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param queueId идентификатор очереди @param returnTimeDelay задержка возвращения в секундах @return визит
- `visitBackToServicePointPool(String branchId, String servicePointId, String poolServicePointId, Long returnTimeDelay)` — Возвращение визита в пул точки обслуживания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param poolServicePointId идентификатор точки обслуживания в чей пул осуществляется возвращение @param returnTimeDelay задержка возвращения в секундах @return визит
- `visitPutBack(String branchId, String servicePointId, Long returnTimeDelay)` — Вернуть визит из точки обслуживания в предыдущий пул (точки или сотрудника). @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param returnTimeDelay задержка возвращения (сек) @return визит
- `visitPostPone(String branchId, String servicePointId)` — Отложить визит в указанной точке обслуживания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return отложенный визит
- `visitBackToUserPool(String branchId, String servicePointId, String userId, Long returnTimeDelay)` — Возвращение визита в пул сотрудника @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param userId идентификатор cотрудника @param returnTimeDelay задержка возвращения в секундах @return визит
- `visitTransfer(String branchId, String servicePointId, String queueId, Visit visit, Integer index, Long transferTimeDelay)` — Перевод визита из очереди в очередь в определенную позицию @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param queueId идентификатор очереди @param visit визит @param index позиция визита в списке @param transferTimeDelay задержка визита после перевода (период запрета на вызов после перевода) @return визит
- `visitTransfer(String branchId, String servicePointId, String queueId, Visit visit, Boolean isToStart, Long transferTimeDelay)` — Перевод визита в очередь @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param queueId идентификатор очереди @param visit визит @param isToStart флаг вставки визита в начало или в конец (по умолчанию в начало) @param transferTimeDelay задержка визита после перевода (период запрета на вызов после перевода) @return визит
- `visitTransfer(String branchId, String queueId, Visit visit, Boolean isAppend, HashMap<String, String> serviceInfo, Long transferTimeDelay, String sid)` — Перевод визита из очереди в очередь внешней службой. @param branchId идентификатор отделения @param queueId идентификатор очереди @param visit визит @param isAppend вставка в конец (true) или начало (false) @param serviceInfo данные внешней службы @param transferTimeDelay задержка визита после перевода (период запрета на вызов после перевода) @param sid идентификатор сессии сотрудника (cookie sid) @return визит
- `visitTransferFromQueueToServicePointPool(String branchId, String servicePointId, String poolServicePointId, Visit visit, Integer index, Long transferTimeDelay)` — Перевод визита из очереди в пул точки обслуживания в определенную позицию в пуле @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param poolServicePointId идентификатор очереди @param visit визит @param index позиция визита в списке @param transferTimeDelay задержка визита после перевода (период запрета на вызов после перевода) @return визит
- `visitTransferFromQueueToServicePointPool(String branchId, String servicePointId, String poolServicePointId, Visit visit, Boolean isAppend, Long transferTimeDelay)` — Перевод визита из очереди в пул точки обслуживания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param poolServicePointId идентификатор очереди @param visit визит @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец) @param transferTimeDelay задержка визита после перевода (период запрета на вызов после перевода) @return визит
- `visitTransferFromQueueToServicePointPool(String branchId, String poolServicePointId, Visit visit, Boolean isAppend, HashMap<String, String> serviceInfo, Long transferTimeDelay, String sid)` — Перевод визита из очереди в пул точки обслуживания внешней службой (MI, Ресепшен и т д) @param branchId идентификатор отделения @param poolServicePointId идентификатор очереди @param visit визит @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец) @param serviceInfo данные о внешней службе @param transferTimeDelay задержка визита после перевода (период запрета на вызов после перевода) @param sid идентификатор сессии сотрудника (cookie sid) @return визит
- `getAllWorkingUsers(String branchId)` — Получение списка всех сотрудников работающих в отделении @param branchId идентификатор отделения @return карта пользователей (логин -> пользователь)
- `visitTransferFromQueueToUserPool(String branchId, String userId, Visit visit, Boolean isAppend, Long transferTimeDelay, String sid)` — Перевод визита из очереди в пул сотрудника @param branchId идентификатор отделения @param userId идентификатор сотрудника @param visit визит @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец) @param transferTimeDelay задержка перевода @param sid идентификатор сессии сотрудника (cookie sid) @return визит
- `visitTransferFromQueueToUserPool(String branchId, String userId, Visit visit, Boolean isAppend, HashMap<String, String> serviceInfo, Long transferTimeDelay, String sid)` — Перевод визита из очереди в пул сотрудника из внешней службы (MI, Ресепшен) @param branchId идентификатор отделения @param userId идентификатор cотрудника @param visit визит @param isAppend флаг вставки визита в начало или в конец (по умолчанию в конец) @param serviceInfo данные о внешней службе @param transferTimeDelay задержка перевода @param sid идентификатор сессии сотрудника (cookie sid) @return визит
- `visitTransferFromQueueToUserPool(String branchId, String userId, Visit visit, Integer index, Long transferTimeDelay, String sid)` — Перевод визита из очереди в пул сотрудника в определенную позицию @param branchId идентификатор отделения @param userId идентификатор cотрудника @param visit визит @param index позиция визита в списке @param transferTimeDelay задержка перевода @param sid идентификатор сессии сотрудника (cookie sid) @return визит
- `visitEnd(String branchId, String servicePointId, Boolean isForced, String reason)` — Завершение визита @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param isForced принудительное завершение обслуживания @param reason причина завершения @return визит
- `visitCall(String branchId, String servicePointId, Visit visit, String callMethod)` — Вызов визита @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param visit визит @param callMethod способ вызова @return визит
- `visitCall(String branchId, String servicePointId, String visitId)` — Вызов визита по идентификатору @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param visitId идентификатор визита @return визит
- `visitCallForConfirmWithMaxWaitingTime(String branchId, String servicePointId, Visit visit)` — Вызов визита с ожиданием подтверждения прихода клиента @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param visit визит @return визит
- `visitReCallForConfirm(String branchId, String servicePointId, Visit visit)` — Повторный вызов визита с ожиданием подтверждения прихода клиента @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param visit визит @return визит
- `visitConfirm(String branchId, String servicePointId, Visit visit)` — Подтверждение прихода клиента @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param visit визит @return визит
- `visitNoShow(String branchId, String servicePointId, Visit visit)` — Завершение не пришедшего визита @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param visit визит @return визит
- `visitCallForConfirmWithMaxWaitingTime(String branchId, String servicePointId)` — Вызов визита с подтверждением прихода c максимальным временем ожидания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return визит
- `visitCallForConfirmWithMaxWaitingTime(String branchId, String servicePointId, List<String> queueIds)` — Вызов визита с подтверждением прихода с максимальным временем ожидания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param queueIds идентификаторы очередей @return визит
- `visitCallForConfirmWithMaxLifeTime(String branchId, String servicePointId)` — Вызов визита с подтверждением прихода c максимальным временем создания визита @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return визит
- `setAutoCallModeOfBranch(String branchId, Boolean isAutoCallMode)` — Включение-выключение режима авовызова для отделения @param branchId идентификатор отделения * @param isAutoCallMode режим автовызова @return отделение
- `setAutoCallModeOfServicePoint(String branchId, String servicePointId, Boolean isAutoCallMode)` — Включение-выключение режима авовызова для точки обслуживания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param isAutoCallMode режим автовызова @return точка обслуживания
- `setConfirmRequiredModeOfServicePoint(String branchId, String servicePointId, Boolean isConfirmRequiredMode)` — Включение-выключение режима необходимости подтверждения прихода для точки обслуживания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param isConfirmRequiredMode режим необходимости подтверждения @return точка обслуживания
- `cancelAutoCallModeOfServicePoint(String branchId, String servicePointId)` — Отмена режима автовызова для точки обслуживания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return точка обслуживания
- `startAutoCallModeOfServicePoint(String branchId, String servicePointId)` — Включение режима автовызова для точки обслуживания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return точка обслуживания
- `visitCallForConfirmWithMaxLifeTime(String branchId, String servicePointId, List<String> queueIds)` — Вызов визита с подтверждением прихода c максимальным временем создания визита @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param queueIds идентификаторы очередей @return визит
- `visitAutoCall(Visit visit)` — Автовызов визита @param visit созданный визит @return визит после аввтовызова
- `visitCallWithMaximalWaitingTime(String branchId, String servicePointId)` — Вызов визита с максимальным временем ожидания @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return визит
- `visitCallWithMaximalWaitingTime(String branchId, String servicePointId, List<String> queueIds)` — Вызов визита с максимальным временем ожидания из очередей, чьи идентификаторы указаны в @param queueIds @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param queueIds идентификаторы очередей @return визит
- `visitCallWithMaxLifeTime(String branchId, String servicePointId)` — Вызов визита с максимальным временем жизни визита @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @return визит
- `visitCallWithMaxLifeTime(String branchId, String servicePointId, List<String> queueIds)` — Вызов визита с максимальным временем жизни визита @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param queueIds идентификаторы очередей @return визит
- `deleteVisit(Visit visit)` — Удаление визита @param visit визит
- `visitTransferToUserPool(String branchId, String servicePointId, String userId, Long transferTimeDelay)` — Перевод визита из точки обслуживания в пул сотрудника. @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param userId идентификатор сотрудника @param transferTimeDelay задержка перевода (сек) @return визит
- `visitTransferToServicePointPool(String branchId, String servicePointId, String poolServicePointId, Long transferTimeDelay)` — Перевод визита из точки обслуживания в пул другой точки обслуживания. @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания-источника @param poolServicePointId идентификатор точки обслуживания-пула @param transferTimeDelay задержка перевода (сек) @return визит
- `visitTransferToServicePointPool(String branchId, String servicePointId, String poolServicePointId, HashMap<String, String> serviceInfo, Long transferTimeDelay)` — Перевод визита из точки обслуживания в пул точки обслуживания с указанием внешней службы. @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания-источника @param poolServicePointId идентификатор точки обслуживания-пула @param serviceInfo данные внешней службы @param transferTimeDelay задержка перевода (сек) @return визит
- `getQueus(String branchId)` — Получить список очередей (id, name) отделения. @param branchId идентификатор отделения @return список сущностей очередей
- `getFullQueus(String branchId)` — Получить полный список очередей отделения. @param branchId идентификатор отделения @return список очередей
- `getPrinters(String branchId)` — Получить список принтеров отделения. @param branchId идентификатор отделения @return список принтеров (id, name)
- `addNote(String branchId, String servicePointId, String noteText)` — Добавить заметку к визиту на точке обслуживания. @param branchId идентификатор отделения @param servicePointId идентификатор точки обслуживания @param noteText текст заметки @return визит с добавленной заметкой
- `getNotes(String branchId, String visitId)` — Просмотр заметок в визите @param branchId идентификатор отделения @param visitId идентификатор точки обслуживания * @return визит

### ru.aritmos.service.rules.CallRule

Правило вызова клиента на обслуживание.

### ru.aritmos.service.rules.CustomCallRule

Пользовательское правило вызова визита. Делегирует логику вызова внешнему клиенту правил.

#### Методы
- `call(Branch branch, ServicePoint servicePoint)` — Вызов визита по настроенному правилу. @param branch отделение @param servicePoint точка обслуживания @return опционально найденный визит
- `call(Branch branch, ServicePoint servicePoint, List<String> queueIds)` — Вызов визита по набору очередей. @param branch отделение @param servicePoint точка обслуживания @param queueIds идентификаторы очередей @return опционально найденный визит
- `getAvailiableServicePoints(Branch currentBranch, Visit visit)` — Получение доступных точек обслуживания для визита. @param currentBranch текущее отделение @param visit визит @return список доступных точек обслуживания

### ru.aritmos.service.rules.MaxLifeTimeCallRule

Правило вызова визита по максимальному времени жизни.

#### Методы
- `call(Branch branch, ServicePoint servicePoint)` — Вызов визита исходя из максимального времени ожидания/возврата. @param branch отделение @param servicePoint точка обслуживания @return опционально найденный визит
- `call(Branch branch, ServicePoint servicePoint, List<String> queueIds)` — Вызов визита из заданного списка очередей. @param branch отделение @param servicePoint точка обслуживания @param queueIds список идентификаторов очередей @return опционально найденный визит
- `getAvailiableServicePoints(Branch currentBranch, Visit visit)` — Возвращает список точек обслуживания, которые могут вызвать данный визит @param currentBranch текущее отделение @param visit визит @return список точек обслуживания

### ru.aritmos.service.rules.MaxWaitingTimeCallRule

Правило вызова визита по максимальному времени ожидания.

#### Методы
- `visitComparer(Visit visit1, Visit visit2)` — Сервис событий. */ @Inject EventService eventService; /** Компаратор визитов по признакам переноса/возврата и времени ожидания. @param visit1 первый визит @param visit2 второй визит @return результат сравнения для сортировки
- `call(Branch branch, ServicePoint servicePoint)` — Конвертация даты типа {@link ZonedDateTime} в строку формата EEE, dd MMM yyyy HH:mm:ss zzz @param date дата типа {@link ZonedDateTime} @return строка даты формата EEE, dd MMM yyyy HH:mm:ss zzz / ZonedDateTime getDateNyString(String date) { DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); return ZonedDateTime.parse(date, format); } /** Вызов визита исходя из максимального времени ожидания. @param branch отделение @param servicePoint точка обслуживания @return опционально найденный визит
- `call(Branch branch, ServicePoint servicePoint, List<String> queueIds)` — Вызов визита из заданного списка очередей по максимальному ожиданию. @param branch отделение @param servicePoint точка обслуживания @param queueIds список идентификаторов очередей @return опционально найденный визит
- `getAvailiableServicePoints(Branch currentBranch, Visit visit)` — Возвращает список точек обслуживания, которые могут вызвать данный визит @param currentBranch текущее отделение @param visit визит @return список точек обслуживания

### ru.aritmos.service.rules.Rule

Базовый интерфейс правила.

### ru.aritmos.service.rules.SegmentationRule

Правила сегментации для выбора очереди на основании параметров визита.

#### Методы
- `getQueue(Visit visit, Branch branch, String segmentationRuleId)` — Получить очередь по идентификатору правила сегментации (Groovy). @param visit визит @param branch отделение @param segmentationRuleId идентификатор правила сегментации @return очередь (если определена)
- `checkSegmentationRules(Visit visit, List<SegmentationRuleData> rules)` — Поиск идентификатора очереди в правиле сегментации @param visit визит @param rules правила сегментации @return идентификатор очереди

### ru.aritmos.service.rules.client.CallRuleClient

HTTP‑клиент для вызова правила определения следующего визита/обслуживания.
