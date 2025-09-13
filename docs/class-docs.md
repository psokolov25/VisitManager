# Документация классов

### ru.aritmos.api.ConfigurationController

@author Pavel Sokolov REST API управления конфигурацией отделений

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

### ru.aritmos.events.services.EventService

Сервис отправки событий в шину данных. <p>Пример использования:</p> <pre>{@code Event event = Event.builder() .eventType("PING") .eventDate(ZonedDateTime.now()) .build(); eventService.send("frontend", false, event); }</pre> <p>Диаграмма последовательности отправки события:</p> <pre> client -> EventService -> DataBusClient -> DataBus </pre> @see <a href="../../../../../../../docs/diagrams/event-service-sequence.svg">Диаграмма последовательности</a>

### ru.aritmos.events.services.EventTask

Асинхронная задача отправки события в шину.

### ru.aritmos.events.services.KafkaListener

Подписчик Kafka для обработки событий шины данных.

### ru.aritmos.events.services.MultiserviceEventTask

Асинхронная задача отправки события нескольким адресатам.

### ru.aritmos.exceptions.BusinessException

Исключение уровня бизнес‑логики с публикацией события об ошибке.

### ru.aritmos.exceptions.SystemException

Системное исключение с публикацией события об ошибке.

### ru.aritmos.handlers.EventHandlerContext

Контекст регистрации обработчиков событий и их привязки к KafkaListener.

### ru.aritmos.keycloack.customsecurity.CustomSecurityRule

Реализация ручной проверки доступа к ресурсу

### ru.aritmos.keycloack.model.Credentials

Учетные данные пользователя для авторизации.

### ru.aritmos.keycloack.model.KeyCloackUser

Сокращённая информация о пользователе из Keycloak.

### ru.aritmos.keycloack.service.EndSessionEndpointResolverReplacement

Реализация резолвера URL завершения сессии с учётом Okta/Keycloak.

### ru.aritmos.keycloack.service.KeyCloackClient

Клиент для взаимодействия с Keycloak: пользователи, группы, сессии.

### ru.aritmos.keycloack.service.UserMapper

Класс отвечает за маппинг свойств пользователя в Keycloak и стандартных свойств OpenId авторизации

### ru.aritmos.model.BasedService

Базовая услуга

### ru.aritmos.model.Branch

Отделение

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

### ru.aritmos.model.keycloak.RealmAccess

Доступы (роли) пользователя в реалме Keycloak.

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

### ru.aritmos.model.tiny.TinyServicePoint

Сокращенный вариант класса рабочей станции

### ru.aritmos.model.tiny.TinyVisit

Сокращенное представление визита

### ru.aritmos.model.visit.TransactionCompletionStatus

Перечень состояний завершения транзакции визита.

### ru.aritmos.model.visit.Visit

Визит

### ru.aritmos.model.visit.VisitEvent

Перечень событий жизненного цикла визита.

### ru.aritmos.model.visit.VisitEventInformation

Информация о событии визита.

### ru.aritmos.model.visit.VisitState

Перечень возможных состояний визита.

### ru.aritmos.service.BranchService

Служба отвечающая за работу с отделениями

### ru.aritmos.service.Configuration

Сервис формирования и публикации конфигурации отделений.

### ru.aritmos.service.GroovyScriptService

Сервис выполнения сценариев Groovy для пользовательских правил.

### ru.aritmos.service.PrinterService

Сервис печати талонов.

### ru.aritmos.service.Services

Класс отвечающий за работу с услугами

### ru.aritmos.service.VisitService

Сервис операций с визитами: создание, перевод, события и печать талонов. <p>Пример использования:</p> <pre>{@code VisitParameters params = new VisitParameters(); params.setServiceIds(List.of("service1")); Visit visit = visitService.createVisit("branch1", "entry1", params, true); }</pre> <p>Диаграмма взаимодействия:</p> <pre> request -> VisitService -> {BranchService, EventService, PrinterService} </pre> @see <a href="../../../../../../docs/diagrams/visit-service-overview.svg">Диаграмма взаимодействия</a>

### ru.aritmos.service.rules.CallRule

Правило вызова клиента на обслуживание.

### ru.aritmos.service.rules.CustomCallRule

Пользовательское правило вызова визита. Делегирует логику вызова внешнему клиенту правил.

### ru.aritmos.service.rules.MaxLifeTimeCallRule

Правило вызова визита по максимальному времени жизни.

### ru.aritmos.service.rules.MaxWaitingTimeCallRule

Правило вызова визита по максимальному времени ожидания.

### ru.aritmos.service.rules.Rule

Базовый интерфейс правила.

### ru.aritmos.service.rules.SegmentationRule

Правила сегментации для выбора очереди на основании параметров визита.

### ru.aritmos.service.rules.client.CallRuleClient

HTTP‑клиент для вызова правила определения следующего визита/обслуживания.
