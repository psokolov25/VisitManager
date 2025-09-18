package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.DelayedEvents;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.model.visit.VisitEventInformation;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.PrinterService;
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.service.rules.SegmentationRule;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты, закрывающие наиболее масштабные непокрытые ранее сценарии {@link VisitService}.
 */
@Slf4j
@ExtendWith(TestLoggingExtension.class)
class VisitServiceUncoveredOperationsTest {

    private final List<VisitEvent> eventsToReset = List.of(
            VisitEvent.TRANSFER_TO_QUEUE,
            VisitEvent.TRANSFER_TO_USER_POOL,
            VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL,
            VisitEvent.BACK_TO_USER_POOL,
            VisitEvent.STOP_SERVING);

    @BeforeEach
    void resetEventsBefore() {
        resetVisitEvents();
    }

    @AfterEach
    void resetEventsAfter() {
        resetVisitEvents();
    }

    @Test
    void visitTransferMovesVisitToQueueWithDelay() {
        log.info("Готовим отделение с оператором и очередями для перевода");
        Branch branch = new Branch("br-1", "Центральное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-1", "Окно №1");
        User operator = new User();
        operator.setId("staff-1");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-1");
        servicePoint.setUser(operator);
        Queue targetQueue = new Queue("queue-target", "Очередь выдачи", "A", 5);
        Queue previousQueue = new Queue("queue-prev", "Предыдущая очередь", "B", 5);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getQueues().put(targetQueue.getId(), targetQueue);
        branch.getQueues().put(previousQueue.getId(), previousQueue);

        Visit visit = Visit.builder()
                .id("visit-queue-transfer")
                .branchId(branch.getId())
                .queueId(previousQueue.getId())
                .servicePointId(servicePoint.getId())
                .ticket("A001")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("LastQueueId", previousQueue.getId());
        visit.getEvents().add(
                VisitEventInformation.builder()
                        .visitEvent(VisitEvent.TRANSFER_TO_QUEUE)
                        .eventDateTime(ZonedDateTime.now().minusMinutes(5))
                        .parameters(Map.of("queueId", previousQueue.getId()))
                        .build());
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));

        log.info("Переводим визит {} из точки {} в очередь {}", visit.getId(), servicePoint.getId(), targetQueue.getId());
        Visit result = serviceUnderTest.visitTransfer(branch.getId(), servicePoint.getId(), targetQueue.getId(), false, 40L);

        log.info("Проверяем, что визит перемещён в очередь и очищен от привязок к точке обслуживания");
        assertSame(visit, result);
        assertEquals(targetQueue.getId(), visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(40L, visit.getTransferTimeDelay());
        assertNotNull(visit.getTransferDateTime());
        assertEquals("true", visit.getParameterMap().get("isTransferredToStart"));
        assertTrue(targetQueue.getVisits().contains(visit));

        log.info("Проверяем параметры события обновления и отложенного уведомления");
        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Boolean> toStartCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), toStartCaptor.capture());
        assertTrue(toStartCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_QUEUE, transferEvent);
        assertEquals(previousQueue.getId(), transferEvent.getParameters().get("oldQueueId"));
        assertEquals(targetQueue.getId(), transferEvent.getParameters().get("newQueueId"));
        assertEquals(servicePoint.getId(), transferEvent.getParameters().get("servicePointId"));
        assertEquals(branch.getId(), transferEvent.getParameters().get("branchId"));
        assertEquals(operator.getId(), transferEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), transferEvent.getParameters().get("staffName"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(40L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("QUEUE_REFRESHED", delayedEvent.getEventType());
        assertEquals(targetQueue.getId(), delayedEvent.getParams().get("queueId"));
        assertEquals(branch.getId(), delayedEvent.getParams().get("branchId"));
        Map<String, String> body = castBody(delayedEvent.getBody());
        assertEquals(previousQueue.getId(), body.get("id"));
        assertEquals(previousQueue.getName(), body.get("name"));
        assertEquals("TRANSFER_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitTransferFromQueueToUserPoolAssignsUserAndSchedulesRefresh() {
        log.info("Формируем отделение и пользователя, принимающего визит в пуле");
        Branch branch = new Branch("br-2", "Отделение на площади");
        User poolUser = new User();
        poolUser.setId("user-42");
        poolUser.setName("Анна Сотрудник");
        branch.getUsers().put(poolUser.getId(), poolUser);

        Visit visit = Visit.builder()
                .id("visit-user-pool")
                .branchId(branch.getId())
                .queueId("queue-initial")
                .ticket("B010")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        UserRepresentation representation = new UserRepresentation();
        representation.setId("kc-1");
        representation.setUsername("receptionist");
        when(keyCloackClient.getUserBySid("sid-1")).thenReturn(java.util.Optional.of(representation));

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, keyCloackClient);

        log.info("Переводим визит {} из очереди в пул пользователя {}", visit.getId(), poolUser.getId());
        Visit result = serviceUnderTest.visitTransferFromQueueToUserPool(
                branch.getId(), poolUser.getId(), visit, false, 25L, "sid-1");

        log.info("Проверяем, что визит закреплён за пулом пользователя и очищен от очереди");
        assertSame(visit, result);
        assertNull(visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertEquals(poolUser.getId(), visit.getPoolUserId());
        assertEquals(25L, visit.getTransferTimeDelay());

        log.info("Анализируем событие обновления и отложенное оповещение пула");
        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Boolean> appendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), appendCaptor.capture());
        assertTrue(appendCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_USER_POOL, transferEvent);
        assertEquals("queue-initial", transferEvent.getParameters().get("queueId"));
        assertEquals(poolUser.getId(), transferEvent.getParameters().get("userId"));
        assertEquals(branch.getId(), transferEvent.getParameters().get("branchId"));
        assertEquals("kc-1", transferEvent.getParameters().get("staffId"));
        assertEquals("receptionist", transferEvent.getParameters().get("staffName"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(25L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("USER_POOL_REFRESHED", delayedEvent.getEventType());
        assertEquals(branch.getId(), delayedEvent.getParams().get("branchId"));
        assertEquals(poolUser.getId(), delayedEvent.getParams().get("poolUserId"));
        Map<String, String> body = castBody(delayedEvent.getBody());
        assertEquals(poolUser.getId(), body.get("id"));
        assertEquals(poolUser.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitBackToUserPoolStopsServingAndAssignsTargetUser() {
        log.info("Создаём отделение с активной точкой обслуживания и пулом сотрудника");
        Branch branch = new Branch("br-3", "Отделение в центре");
        ServicePoint activePoint = new ServicePoint("sp-active", "Окно активное");
        User activeStaff = new User();
        activeStaff.setId("staff-active");
        activeStaff.setName("Михаил Оператор");
        activeStaff.setCurrentWorkProfileId("wp-active");
        activePoint.setUser(activeStaff);
        ServicePoint poolPoint = new ServicePoint("sp-pool", "Окно пула");
        User poolUser = new User();
        poolUser.setId("staff-pool");
        poolUser.setName("Ольга Специалист");
        poolUser.setCurrentWorkProfileId("wp-pool");
        poolPoint.setUser(poolUser);
        branch.getServicePoints().put(activePoint.getId(), activePoint);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        Visit visit = Visit.builder()
                .id("visit-back-user")
                .branchId(branch.getId())
                .servicePointId(activePoint.getId())
                .ticket("C202")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("LastPoolUserId", poolUser.getId());
        activePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));

        log.info("Возвращаем визит {} из точки {} в пул пользователя {}", visit.getId(), activePoint.getId(), poolUser.getId());
        Visit result = serviceUnderTest.visitBackToUserPool(
                branch.getId(), activePoint.getId(), poolUser.getId(), 50L);

        log.info("Проверяем, что визит отвязан от точки и закреплён за пулом пользователя");
        assertSame(visit, result);
        assertNull(visit.getServicePointId());
        assertNull(visit.getQueueId());
        assertNull(visit.getPoolServicePointId());
        assertEquals(poolUser.getId(), visit.getPoolUserId());
        assertEquals(50L, visit.getReturnTimeDelay());
        assertNotNull(visit.getTransferDateTime());
        assertNotNull(visit.getReturnDateTime());
        assertNull(visit.getStartServingDateTime());
        assertFalse(visit.getParameterMap().containsKey("LastPoolUserId"));

        log.info("Проверяем последовательность событий остановки и возврата");
        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest));
        List<VisitEvent> events = eventCaptor.getAllValues();
        assertEquals(2, events.size());
        VisitEvent stopEvent = events.get(0);
        assertSame(VisitEvent.STOP_SERVING, stopEvent);
        assertEquals("false", stopEvent.getParameters().get("isForced"));
        assertEquals(branch.getId(), stopEvent.getParameters().get("branchId"));
        assertEquals(activePoint.getId(), stopEvent.getParameters().get("servicePointId"));
        assertEquals(activePoint.getName(), stopEvent.getParameters().get("servicePointName"));
        assertEquals(activeStaff.getId(), stopEvent.getParameters().get("staffId"));
        assertEquals(activeStaff.getName(), stopEvent.getParameters().get("staffName"));
        assertEquals(activeStaff.getCurrentWorkProfileId(), stopEvent.getParameters().get("workProfileId"));

        VisitEvent backEvent = events.get(1);
        assertSame(VisitEvent.BACK_TO_USER_POOL, backEvent);
        assertEquals(branch.getId(), backEvent.getParameters().get("branchId"));
        assertEquals(activeStaff.getId(), backEvent.getParameters().get("staffId"));
        assertEquals(activeStaff.getName(), backEvent.getParameters().get("staffName"));
        assertEquals(activeStaff.getCurrentWorkProfileId(), backEvent.getParameters().get("workProfileId"));
        assertEquals(poolUser.getId(), backEvent.getParameters().get("userId"));
        assertEquals(poolUser.getName(), backEvent.getParameters().get("userName"));
        assertEquals(activePoint.getId(), backEvent.getParameters().get("servicePointId"));
    }

    @Test
    void visitTransferWithVisitEntityPlacesToStartWhenRequested() {
        log.info("Подготавливаем отделение с точкой обслуживания и двумя очередями");
        Branch branch = new Branch("br-4", "Северное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-queue", "Окно очереди");
        User operator = new User();
        operator.setId("staff-q");
        operator.setName("Ирина Оператор");
        operator.setCurrentWorkProfileId("wp-q");
        servicePoint.setUser(operator);
        Queue targetQueue = new Queue("queue-target-2", "Очередь оформления", "D", 3);
        Queue previousQueue = new Queue("queue-prev-2", "Предыдущая очередь", "E", 4);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getQueues().put(targetQueue.getId(), targetQueue);
        branch.getQueues().put(previousQueue.getId(), previousQueue);

        Visit visit = Visit.builder()
                .id("visit-to-start")
                .branchId(branch.getId())
                .queueId(previousQueue.getId())
                .servicePointId(servicePoint.getId())
                .ticket("D404")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("LastQueueId", previousQueue.getId());

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));

        log.info("Переводим визит {} в начало очереди {}", visit.getId(), targetQueue.getId());
        Visit result = serviceUnderTest.visitTransfer(
                branch.getId(), servicePoint.getId(), targetQueue.getId(), visit, true, 35L);

        log.info("Проверяем, что визит находится в новой очереди и помечен как перенесённый в начало");
        assertSame(visit, result);
        assertEquals(targetQueue.getId(), visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(35L, visit.getTransferTimeDelay());
        assertNotNull(visit.getTransferDateTime());
        assertNotNull(visit.getParameterMap().get("isTransferredToStart"));

        log.info("Проверяем событие обновления очереди и параметры уведомления");
        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Boolean> startCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), startCaptor.capture());
        assertTrue(startCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_QUEUE, transferEvent);
        assertEquals(previousQueue.getId(), transferEvent.getParameters().get("oldQueueId"));
        assertEquals(targetQueue.getId(), transferEvent.getParameters().get("newQueueId"));
        assertEquals(servicePoint.getId(), transferEvent.getParameters().get("servicePointId"));
        assertEquals(branch.getId(), transferEvent.getParameters().get("branchId"));
        assertEquals(operator.getId(), transferEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), transferEvent.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), transferEvent.getParameters().get("workProfileId"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(35L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("QUEUE_REFRESHED", delayedEvent.getEventType());
        assertEquals(targetQueue.getId(), delayedEvent.getParams().get("queueId"));
        assertEquals(branch.getId(), delayedEvent.getParams().get("branchId"));
        Map<String, String> body = castBody(delayedEvent.getBody());
        assertEquals(previousQueue.getId(), body.get("id"));
        assertEquals(previousQueue.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitTransferFromQueueToServicePointPoolPropagatesExternalInfo() {
        log.info("Настраиваем отделение и пул точки обслуживания для внешнего переноса");
        Branch branch = new Branch("br-5", "Южное отделение");
        ServicePoint poolPoint = new ServicePoint("sp-pool-ext", "Пул окна 5");
        poolPoint.setUser(null);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        Visit visit = Visit.builder()
                .id("visit-pool-ext")
                .branchId(branch.getId())
                .queueId("queue-legacy")
                .ticket("E505")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("externalSystem", "reception");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        UserRepresentation representation = new UserRepresentation();
        representation.setId("kc-ext");
        representation.setUsername("external-user");
        when(keyCloackClient.getUserBySid("sid-ext")).thenReturn(java.util.Optional.of(representation));

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, keyCloackClient);

        log.info("Переносим визит {} в пул {} через внешнюю службу", visit.getId(), poolPoint.getId());
        Visit result = serviceUnderTest.visitTransferFromQueueToServicePointPool(
                branch.getId(), poolPoint.getId(), visit, false, serviceInfo, 55L, "sid-ext");

        log.info("Проверяем обновление визита и параметры события");
        assertSame(visit, result);
        assertNull(visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(poolPoint.getId(), visit.getPoolServicePointId());
        assertEquals(55L, visit.getTransferTimeDelay());
        assertNotNull(visit.getTransferDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Boolean> appendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), appendCaptor.capture());
        assertTrue(appendCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, transferEvent);
        assertEquals("queue-legacy", transferEvent.getParameters().get("queueId"));
        assertEquals(poolPoint.getId(), transferEvent.getParameters().get("poolServicePointId"));
        assertEquals("kc-ext", transferEvent.getParameters().get("staffId"));
        assertEquals("external-user", transferEvent.getParameters().get("staffName"));
        assertEquals(branch.getId(), transferEvent.getParameters().get("branchId"));
        assertEquals("reception", transferEvent.getParameters().get("externalSystem"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(55L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("SERVICEPOINT_POOL_REFRESHED", delayedEvent.getEventType());
        Map<String, String> body = castBody(delayedEvent.getBody());
        assertEquals(poolPoint.getId(), body.get("id"));
        assertEquals(poolPoint.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    private VisitService createVisitService(
            BranchService branchService,
            EventService eventService,
            DelayedEvents delayedEvents,
            KeyCloackClient keyCloackClient) {
        VisitService serviceUnderTest = new VisitService();
        serviceUnderTest.branchService = branchService;
        serviceUnderTest.eventService = eventService;
        serviceUnderTest.delayedEvents = delayedEvents;
        serviceUnderTest.printerService = mock(PrinterService.class);
        serviceUnderTest.keyCloackClient = keyCloackClient;
        serviceUnderTest.segmentationRule = mock(SegmentationRule.class);
        serviceUnderTest.setWaitingTimeCallRule(mock(CallRule.class));
        serviceUnderTest.setLifeTimeCallRule(mock(CallRule.class));
        return serviceUnderTest;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castBody(Object body) {
        return (Map<String, String>) body;
    }

    private void resetVisitEvents() {
        eventsToReset.forEach(event -> {
            event.getParameters().clear();
            event.dateTime = null;
        });
    }
}
