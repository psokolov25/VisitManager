package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.HttpStatus;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.DelayedEvents;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.EntryPoint;
import ru.aritmos.model.Entity;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.model.visit.VisitEventInformation;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.WorkProfile;
import ru.aritmos.model.Reception;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.service.rules.CallRule;
import org.keycloak.representations.idm.UserRepresentation;

class VisitServiceTest {

    private static final Logger log = LoggerFactory.getLogger(VisitServiceTest.class);

    @DisplayName("Получение визита возвращает найденную запись")
    @Test
    void getVisitReturnsExistingVisit() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 1);
        Visit visit = Visit.builder().id("v1").branchId("b1").queueId("q1").build();
        queue.getVisits().add(visit);
        branch.getQueues().put(queue.getId(), queue);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.getVisit("b1", "v1");
        assertSame(visit, result);
    }

    @DisplayName("Вызов визита для подтверждения по максимальному времени жизни обновляет визит и формирует событие")
    @Test
    void visitCallForConfirmWithMaxLifeTimeUpdatesVisitAndBuildsEvent() {
        log.info("Подготавливаем отделение и точку обслуживания для сценария с успешным вызовом визита.");
        Branch branch = new Branch("branch-life", "Отделение с визитами");
        ServicePoint servicePoint = new ServicePoint("sp-life", "Окно 42");
        User operator = new User("user-1", "Оператор", null);
        operator.setCurrentWorkProfileId("wp-7");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        Visit visit = Visit.builder().id("visit-1").branchId(branch.getId()).build();

        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        CallRule lifeTimeRule = mock(CallRule.class);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.setLifeTimeCallRule(lifeTimeRule);

        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(lifeTimeRule.call(branch, servicePoint)).thenReturn(Optional.of(visit));

        log.info("Запускаем вызов визита по максимальному времени жизни для точки {}.", servicePoint.getId());
        Optional<Visit> result = service.visitCallForConfirmWithMaxLifeTime(branch.getId(), servicePoint.getId());

        log.info("Проверяем, что визит получен и событие построено корректно.");
        assertTrue(result.isPresent());
        assertSame(visit, result.get());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(eq(visit), eventCaptor.capture(), eq(service));

        VisitEvent event = eventCaptor.getValue();
        assertEquals(VisitEvent.CALLED, event);
        assertNotNull(event.dateTime);

        Map<String, String> params = event.getParameters();
        log.info("Параметры события вызова: {}", params);
        assertEquals(servicePoint.getId(), params.get("servicePointId"));
        assertEquals(servicePoint.getName(), params.get("servicePointName"));
        assertEquals(branch.getId(), params.get("branchId"));
        assertEquals(operator.getId(), params.get("staffId"));
        assertEquals(operator.getName(), params.get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), params.get("workProfileId"));
        assertEquals("callNext", params.get("callMethod"));
    }

    @DisplayName("Вызов визита для подтверждения по максимальному времени жизни включает автообзвон при отсутствии кандидатов")
    @Test
    void visitCallForConfirmWithMaxLifeTimeEnablesAutocallWhenNoVisitFound() {
        log.info("Подготавливаем отделение в режиме автозапуска для проверки обработки отсутствующего визита.");
        Branch branch = new Branch("branch-auto", "Отделение в автозапуске");
        branch.getParameterMap().put("autoCallMode", Boolean.TRUE.toString());
        ServicePoint servicePoint = new ServicePoint("sp-auto", "Окно автозапуска");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        EventService eventService = mock(EventService.class);
        CallRule lifeTimeRule = mock(CallRule.class);
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.setLifeTimeCallRule(lifeTimeRule);

        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(lifeTimeRule.call(branch, servicePoint)).thenReturn(Optional.empty());

        log.info("Проверяем, что вызов без визита приводит к включению автоворонку и исключению.");
        HttpStatusException exception =
                assertThrows(
                        HttpStatusException.class,
                        () -> service.visitCallForConfirmWithMaxLifeTime(branch.getId(), servicePoint.getId()));

        assertEquals(207, exception.getStatus().getCode());
        assertTrue(servicePoint.getAutoCallMode());
        verify(branchService).add(branch.getId(), branch);
    }

    @DisplayName("Возврат из обслуживания в очередь возвращает визит в последнюю очередь")
    @Test
    void stopServingAndBackToQueueReturnsVisitToLastQueue() {
        log.info("Формируем исходные данные для сценария возврата визита из обслуживания в очередь.");
        String branchId = "branch-stop";
        String servicePointId = "sp-stop";
        String queueId = "queue-stop";

        Branch branch = new Branch(branchId, "Отделение с возвратом");
        Queue queue = new Queue(queueId, "Очередь возврата", "A", 1);
        branch.getQueues().put(queueId, queue);

        ServicePoint servicePoint = new ServicePoint(servicePointId, "Окно возврата");
        User operator = new User("operator-1", "Оператор", null);
        operator.setCurrentWorkProfileId("wp-13");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePointId, servicePoint);

        Service currentService = new Service("srv-1", "Кредиты", 60, null);
        assertEquals("srv-1", currentService.getId());
        Visit visit = Visit.builder()
                .id("visit-stop")
                .branchId(branchId)
                .ticket("A-001")
                .currentService(currentService)
                .parameterMap(new HashMap<>())
                .events(List.of(
                        VisitEventInformation.builder()
                                .visitEvent(VisitEvent.START_SERVING)
                                .eventDateTime(ZonedDateTime.now().minusMinutes(5))
                                .parameters(Map.of("servicePointId", servicePointId))
                                .build()))
                .build();
        visit.setCurrentService(currentService);
        visit.getParameterMap().put("LastQueueId", queueId);
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branchId)).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;

        log.info("Запускаем остановку обслуживания визита с последующим возвратом в очередь.");
        Visit result = service.stopServingAndBackToQueue(branchId, servicePointId, 30L);

        log.info("Проверяем, что визит вернулся в очередь и данные очищены корректно.");
        assertSame(visit, result);
        assertEquals(queueId, visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(30L, visit.getReturnTimeDelay());
        assertTrue(queue.getVisits().contains(visit));

        log.info("Проверяем параметры событий обновления визита.");
        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(eq(visit), eventCaptor.capture(), eq(service));
        List<VisitEvent> capturedEvents = eventCaptor.getAllValues();
        VisitEvent stopEvent = capturedEvents.get(0);
        assertEquals(VisitEvent.STOP_SERVING, stopEvent);
        log.info("Параметры STOP_SERVING события: {}", stopEvent.getParameters());
        assertEquals("false", stopEvent.getParameters().get("isForced"));
        assertEquals(servicePointId, stopEvent.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), stopEvent.getParameters().get("servicePointName"));
        assertEquals(branchId, stopEvent.getParameters().get("branchId"));

        VisitEvent backEvent = capturedEvents.get(1);
        assertEquals(VisitEvent.BACK_TO_QUEUE, backEvent);
        assertEquals(branchId, backEvent.getParameters().get("branchId"));
        assertEquals(queueId, backEvent.getParameters().get("queueId"));
        assertEquals(servicePointId, backEvent.getParameters().get("servicePointId"));
        assertEquals(operator.getId(), backEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), backEvent.getParameters().get("staffName"));
        assertNotNull(backEvent.dateTime);

        log.info("Проверяем планирование отложенного события обновления очереди.");
        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents)
                .delayedEventService(
                        eq("frontend"),
                        eq(false),
                        delayedEventCaptor.capture(),
                        eq(30L),
                        eq(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("QUEUE_REFRESHED", delayedEvent.getEventType());
        assertEquals(Map.of("queueId", queueId, "branchId", branchId), delayedEvent.getParams());
        Map<?, ?> body = (Map<?, ?>) delayedEvent.getBody();
        assertEquals(queueId, body.get("id"));
        assertEquals(queue.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
        stopEvent.getParameters().clear();
        stopEvent.dateTime = null;
        backEvent.getParameters().clear();
        backEvent.dateTime = null;
    }

    @DisplayName("Возврат из обслуживания в очередь завершается ошибкой при отсутствии последней очереди")
    @Test
    void stopServingAndBackToQueueFailsWhenLastQueueMissing() {
        log.info("Настраиваем отделение, где визит не содержит информации о последней очереди.");
        String branchId = "branch-stop-error";
        String servicePointId = "sp-stop-error";

        Branch branch = new Branch(branchId, "Отделение без очереди");
        Queue queue = new Queue("queue-error", "Очередь ошибки", "B", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint servicePoint = new ServicePoint(servicePointId, "Окно ошибки");
        User operator = new User("operator-err", "Оператор", null);
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePointId, servicePoint);

        Service currentService = new Service("srv-2", "Ипотека", 120, null);
        assertEquals("srv-2", currentService.getId());
        Visit visit = Visit.builder()
                .id("visit-stop-error")
                .branchId(branchId)
                .ticket("B-777")
                .currentService(currentService)
                .parameterMap(new HashMap<>())
                .events(List.of(
                        VisitEventInformation.builder()
                                .visitEvent(VisitEvent.START_SERVING)
                                .eventDateTime(ZonedDateTime.now().minusMinutes(3))
                                .parameters(Map.of("servicePointId", servicePointId))
                                .build()))
                .build();
        visit.setCurrentService(currentService);
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branchId)).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;

        log.info("Запускаем возврат визита и ожидаем бизнес-ошибку.");
        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.stopServingAndBackToQueue(branchId, servicePointId, 45L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(45L, visit.getReturnTimeDelay());

        log.info("Убеждаемся, что событие StopServing отправлено до возникновения ошибки.");
        ArgumentCaptor<VisitEvent> stopEventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(eq(visit), stopEventCaptor.capture(), eq(service));
        assertEquals(VisitEvent.STOP_SERVING, stopEventCaptor.getValue());
        verify(eventService).send(eq("*"), eq(false), any());
        verifyNoInteractions(delayedEvents);
        stopEventCaptor.getValue().getParameters().clear();
        stopEventCaptor.getValue().dateTime = null;
    }

    @DisplayName("Перенос визита из очереди в пул точки добавляет метаданные внешней услуги")
    @Test
    void visitTransferFromQueueToServicePointPoolAddsExternalServiceMetadata() {
        log.info("Очищаем параметры события перевода визита в пул точки обслуживания.");
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getParameters().clear();

        log.info("Готовим отделение с очередью и целевым пулом точки обслуживания.");
        String branchId = "branch-transfer";
        String poolServicePointId = "pool-sp";
        String queueId = "queue-transfer";

        Branch branch = new Branch(branchId, "Отделение перевода");
        ServicePoint poolServicePoint = new ServicePoint(poolServicePointId, "Пул окна");
        branch.getServicePoints().put(poolServicePointId, poolServicePoint);
        Queue queue = new Queue(queueId, "Очередь перевода", "C", 1);
        branch.getQueues().put(queueId, queue);

        Visit visit = Visit.builder()
                .id("visit-transfer")
                .branchId(branchId)
                .queueId(queueId)
                .ticket("C-123")
                .parameterMap(new HashMap<>(Map.of("LastQueueId", queueId)))
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branchId)).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);
        ru.aritmos.keycloack.service.KeyCloackClient keyCloackClient =
                mock(ru.aritmos.keycloack.service.KeyCloackClient.class);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("kc-1");
        userRepresentation.setUsername("keycloak-user");
        when(keyCloackClient.getUserBySid("sid-123")).thenReturn(Optional.of(userRepresentation));

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;
        service.keyCloackClient = keyCloackClient;

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("externalSystem", "MI");
        serviceInfo.put("requestId", "req-555");

        log.info("Переводим визит во внешний пул точки обслуживания.");
        Visit result = service.visitTransferFromQueueToServicePointPool(
                branchId, poolServicePointId, visit, false, serviceInfo, 25L, "sid-123");

        assertSame(visit, result);
        assertNull(visit.getQueueId());
        assertEquals(poolServicePointId, visit.getPoolServicePointId());
        assertEquals(25L, visit.getTransferTimeDelay());

        log.info("Проверяем параметры события TRANSFER_TO_SERVICE_POINT_POOL.");
        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService)
                .updateVisit(eq(visit), eventCaptor.capture(), eq(service), eq(Boolean.TRUE));
        VisitEvent event = eventCaptor.getValue();
        assertEquals(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, event);
        assertEquals(queueId, event.getParameters().get("queueId"));
        assertEquals(poolServicePointId, event.getParameters().get("poolServicePointId"));
        assertEquals("kc-1", event.getParameters().get("staffId"));
        assertEquals("keycloak-user", event.getParameters().get("staffName"));
        assertEquals("MI", event.getParameters().get("externalSystem"));
        assertEquals("req-555", event.getParameters().get("requestId"));
        assertEquals(branchId, event.getParameters().get("branchId"));
        assertNotNull(event.dateTime);

        log.info("Проверяем создание отложенного события обновления пула точки обслуживания.");
        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents)
                .delayedEventService(
                        eq("frontend"),
                        eq(false),
                        delayedEventCaptor.capture(),
                        eq(25L),
                        eq(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("SERVICEPOINT_POOL_REFRESHED", delayedEvent.getEventType());
        Map<?, ?> body = (Map<?, ?>) delayedEvent.getBody();
        assertEquals(poolServicePointId, body.get("id"));
        assertEquals(poolServicePoint.getName(), body.get("name"));
        assertEquals(branchId, body.get("branchId"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
        assertNull(delayedEvent.getParams());
        event.getParameters().clear();
        event.dateTime = null;
        delayedEvent.setBody(null);
        delayedEvent.setParams(null);
    }

    @DisplayName("Получение визита выбрасывает исключение при отсутствии записи")
    @Test
    void getVisitThrowsWhenMissing() {
        Branch branch = new Branch("b1", "Branch");
        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        EventService eventService = mock(EventService.class);
        service.branchService = branchService;
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.getVisit("b1", "missing"));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @DisplayName("Получение строковой карты точек обслуживания фильтрует занятые точки")
    @Test
    void getStringServicePointHashMapFiltersBusyPoints() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint free = new ServicePoint("sp1", "SP1");
        ServicePoint busy = new ServicePoint("sp2", "SP2");
        busy.setUser(new User("u1", "User", null));
        branch.getServicePoints().put(free.getId(), free);
        branch.getServicePoints().put(busy.getId(), busy);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        HashMap<String, ServicePoint> result = service.getStringServicePointHashMap("b1");
        assertEquals(1, result.size());
        assertTrue(result.containsKey("sp1"));
        assertFalse(result.containsKey("sp2"));
    }

    @DisplayName("Получение карты точек обслуживания возвращает все точки")
    @Test
    void getServicePointHashMapReturnsAllPoints() {
        Branch branch = new Branch("b1", "Branch");
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP1"));
        branch.getServicePoints().put("sp2", new ServicePoint("sp2", "SP2"));

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        HashMap<String, ServicePoint> result = service.getServicePointHashMap("b1");
        assertEquals(2, result.size());
        assertTrue(result.containsKey("sp1"));
        assertTrue(result.containsKey("sp2"));
    }

    @DisplayName("Получение рабочих профилей возвращает Tiny-модели")
    @Test
    void getWorkProfilesReturnsTinyClasses() {
        Branch branch = new Branch("b1", "Branch");
        branch.getWorkProfiles().put("wp1", new WorkProfile("wp1", "Profile"));

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<TinyClass> profiles = service.getWorkProfiles("b1");
        assertEquals(1, profiles.size());
        assertEquals(new TinyClass("wp1", "Profile"), profiles.get(0));
    }

    @DisplayName("Получение пользователей отделения возвращает сотрудников отделения")
    @Test
    void getUsersReturnsBranchUsers() {
        Branch branch = new Branch("b1", "Branch");
        branch.getUsers().put("u1", new User("u1", "User", null));

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<User> users = service.getUsers("b1");
        assertEquals(1, users.size());
        assertEquals("u1", users.get(0).getId());
    }

    @DisplayName("Получение принтеров собирает уникальные устройства печати")
    @Test
    void getPrintersCollectsUniquePrinters() {
        Branch branch = new Branch("b1", "Branch");
        EntryPoint ep = new EntryPoint();
        ep.setId("e1");
        ep.setName("EP");
        ep.setPrinter(new Entity("p1", "P1"));
        branch.getEntryPoints().put("e1", ep);
        Reception reception = new Reception();
        reception.setBranchId("b1");
        reception.setPrinters(new java.util.ArrayList<>(List.of(new Entity("p1", "P1"), new Entity("p2", "P2"))));
        branch.setReception(reception);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<Entity> printers = service.getPrinters("b1");
        assertEquals(2, printers.size());
        assertTrue(printers.contains(new Entity("p1", "P1")));
        assertTrue(printers.contains(new Entity("p2", "P2")));
    }

    @DisplayName("Получение очередей возвращает список сущностей очередей")
    @Test
    void getQueusReturnsEntityList() {
        Branch branch = new Branch("b1", "Branch");
        branch.getQueues().put("q1", new Queue("q1", "Q1", "A", 1));

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<Entity> queues = service.getQueus("b1");
        assertEquals(List.of(new Entity("q1", "Q1")), queues);
    }

    @DisplayName("Получение полных данных очередей возвращает сами очереди")
    @Test
    void getFullQueusReturnsQueues() {
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Q1", "A", 1);
        branch.getQueues().put("q1", queue);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        List<Queue> queues = service.getFullQueus("b1");
        assertEquals(List.of(queue), queues);
    }

    @DisplayName("Получение работающих сотрудников агрегирует пользователей из точек обслуживания")
    @Test
    void getAllWorkingUsersAggregatesFromServicePoints() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        User user = new User("u1", "User", null);
        sp.setUser(user);
        branch.getServicePoints().put("sp1", sp);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(ru.aritmos.keycloack.service.KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        HashMap<String, User> users = service.getAllWorkingUsers("b1");
        assertEquals(1, users.size());
        assertSame(user, users.get("u1"));
    }
}
