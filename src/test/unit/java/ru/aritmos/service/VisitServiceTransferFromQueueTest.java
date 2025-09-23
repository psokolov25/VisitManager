package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

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
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.service.rules.SegmentationRule;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты перевода визитов между очередями, пулами точек обслуживания и пулами сотрудников.
 */
@Slf4j
@ExtendWith(TestLoggingExtension.class)
class VisitServiceTransferFromQueueTest {

    private final List<VisitEvent> eventsToReset = List.of(
            VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL,
            VisitEvent.TRANSFER_TO_QUEUE,
            VisitEvent.BACK_TO_SERVICE_POINT_POOL,
            VisitEvent.STOP_SERVING,
            VisitEvent.TRANSFER_TO_USER_POOL);

    @BeforeEach
    void clearEventParametersBefore() {
        resetVisitEvents();
    }

    @AfterEach
    void clearEventParametersAfter() {
        resetVisitEvents();
    }

    @Test
    void visitTransferFromQueueToServicePointPoolPlacesVisitAtExactIndex() {
        log.info("Готовим отделение с оператором и пулом точки обслуживания");
        Branch branch = new Branch("b1", "Главное отделение");
        ServicePoint sourcePoint = new ServicePoint("sp-source", "Окно №1");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-1");
        sourcePoint.setUser(operator);
        ServicePoint poolPoint = new ServicePoint("sp-pool", "Пул окна №1");
        branch.getServicePoints().put(sourcePoint.getId(), sourcePoint);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        log.info("Создаём визит с исходной очередью и талоном");
        Visit visit = Visit.builder()
                .id("visit-1")
                .branchId("b1")
                .queueId("queue-1")
                .ticket("A001")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));

        log.info("Переводим визит {} в пул {} с указанием позиции", visit.getId(), poolPoint.getId());
        Visit result = serviceUnderTest.visitTransferFromQueueToServicePointPool(
                "b1", sourcePoint.getId(), poolPoint.getId(), visit, 1, 30L);

        log.info("Проверяем, что визит обновлён и перемещён в пул точки обслуживания");
        assertSame(visit, result);
        assertNull(visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(poolPoint.getId(), visit.getPoolServicePointId());
        assertNotNull(visit.getTransferDateTime());
        assertEquals(30L, visit.getTransferTimeDelay());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), indexCaptor.capture());
        assertEquals(1, indexCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, transferEvent);
        assertEquals("queue-1", transferEvent.getParameters().get("queueId"));
        assertEquals(poolPoint.getId(), transferEvent.getParameters().get("poolServicePointId"));
        assertEquals(sourcePoint.getId(), transferEvent.getParameters().get("servicePointId"));
        assertEquals("b1", transferEvent.getParameters().get("branchId"));
        assertEquals("user-1", transferEvent.getParameters().get("staffId"));
        assertEquals("Иван Оператор", transferEvent.getParameters().get("staffName"));
        assertEquals("wp-1", transferEvent.getParameters().get("workProfileId"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(30L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("SERVICEPOINT_POOL_REFRESHED", delayedEvent.getEventType());
        assertEquals("b1", delayedEvent.getParams().get("branchId"));
        assertEquals(sourcePoint.getId(), delayedEvent.getParams().get("servicePointId"));
        Map<String, String> body = (Map<String, String>) delayedEvent.getBody();
        assertEquals(sourcePoint.getId(), body.get("id"));
        assertEquals(poolPoint.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals("A001", body.get("ticket"));
    }

    @Test
    void visitTransferToQueueFromServicePointPlacesVisitToStartWhenRequested() {
        log.info("Готовим отделение с очередью и оператором точки обслуживания");
        Branch branch = new Branch("b1", "Главное отделение");
        Queue targetQueue = new Queue("queue-new", "Очередь выдачи", "A", 5);
        Queue previousQueue = new Queue("queue-1", "Предыдущая очередь", "B", 5);
        branch.getQueues().put(targetQueue.getId(), targetQueue);
        branch.getQueues().put(previousQueue.getId(), previousQueue);
        ServicePoint servicePoint = new ServicePoint("sp-1", "Окно №1");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-1");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        log.info("Формируем визит, который уже находился в очереди");
        Visit visit = Visit.builder()
                .id("visit-queue")
                .branchId("b1")
                .queueId(previousQueue.getId())
                .ticket("B007")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("LastQueueId", previousQueue.getId());

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));

        log.info("Переводим визит {} в очередь {} с установкой в начало", visit.getId(), targetQueue.getId());
        Visit result = serviceUnderTest.visitTransfer(
                "b1", servicePoint.getId(), targetQueue.getId(), visit, true, 45L);

        log.info("Проверяем, что визит обновил очередь и помечен как перенесённый в начало");
        assertSame(visit, result);
        assertEquals(targetQueue.getId(), visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertNotNull(visit.getParameterMap().get("isTransferredToStart"));
        assertEquals(45L, visit.getTransferTimeDelay());
        assertNotNull(visit.getTransferDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Boolean> toStartCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), toStartCaptor.capture());
        assertTrue(toStartCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_QUEUE, transferEvent);
        assertEquals(previousQueue.getId(), transferEvent.getParameters().get("oldQueueId"));
        assertEquals(targetQueue.getId(), transferEvent.getParameters().get("newQueueId"));
        assertEquals(servicePoint.getId(), transferEvent.getParameters().get("servicePointId"));
        assertEquals("b1", transferEvent.getParameters().get("branchId"));
        assertEquals("user-1", transferEvent.getParameters().get("staffId"));
        assertEquals("Иван Оператор", transferEvent.getParameters().get("staffName"));
        assertEquals("wp-1", transferEvent.getParameters().get("workProfileId"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(45L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("QUEUE_REFRESHED", delayedEvent.getEventType());
        assertEquals(targetQueue.getId(), delayedEvent.getParams().get("queueId"));
        assertEquals("b1", delayedEvent.getParams().get("branchId"));
        Map<String, String> body = (Map<String, String>) delayedEvent.getBody();
        assertEquals(previousQueue.getId(), body.get("id"));
        assertEquals(previousQueue.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitTransferToQueueFromExternalServicePropagatesServiceInfoAndSid() {
        log.info("Готовим отделение с очередями и заполняем информацию Keycloak");
        Branch branch = new Branch("b1", "Главное отделение");
        Queue targetQueue = new Queue("queue-new", "Очередь выдачи", "A", 5);
        Queue previousQueue = new Queue("queue-prev", "Предыдущая очередь", "B", 5);
        branch.getQueues().put(targetQueue.getId(), targetQueue);
        branch.getQueues().put(previousQueue.getId(), previousQueue);

        log.info("Создаём визит, который ранее обслуживался в точке обслуживания");
        Visit visit = Visit.builder()
                .id("visit-external")
                .branchId("b1")
                .queueId(previousQueue.getId())
                .servicePointId("sp-legacy")
                .ticket("B101")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("LastQueueId", previousQueue.getId());

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("kc-user");
        userRepresentation.setUsername("receptionist");
        when(keyCloackClient.getUserBySid("sid-42")).thenReturn(java.util.Optional.of(userRepresentation));

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, keyCloackClient);

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("externalSystem", "reception");

        log.info("Переводим визит {} из внешней службы в очередь {}", visit.getId(), targetQueue.getId());
        Visit result = serviceUnderTest.visitTransfer(
                "b1", targetQueue.getId(), visit, false, serviceInfo, 20L, "sid-42");

        log.info("Проверяем, что визит переместился в очередь и содержит информацию о переносе");
        assertSame(visit, result);
        assertEquals(targetQueue.getId(), visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals("true", visit.getParameterMap().get("isTransferredToStart"));
        assertEquals(20L, visit.getTransferTimeDelay());
        assertNotNull(visit.getTransferDateTime());
        assertNotNull(visit.getReturnDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Boolean> appendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), appendCaptor.capture());
        assertTrue(appendCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_QUEUE, transferEvent);
        assertEquals(previousQueue.getId(), transferEvent.getParameters().get("oldQueueId"));
        assertEquals(targetQueue.getId(), transferEvent.getParameters().get("newQueueId"));
        assertEquals("b1", transferEvent.getParameters().get("branchId"));
        assertEquals("kc-user", transferEvent.getParameters().get("staffId"));
        assertEquals("receptionist", transferEvent.getParameters().get("staffName"));
        assertEquals("reception", transferEvent.getParameters().get("externalSystem"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(20L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("QUEUE_REFRESHED", delayedEvent.getEventType());
        assertEquals(targetQueue.getId(), delayedEvent.getParams().get("queueId"));
        Map<String, String> body = (Map<String, String>) delayedEvent.getBody();
        assertEquals(previousQueue.getId(), body.get("id"));
        assertEquals(previousQueue.getName(), body.get("name"));
        assertEquals("TRANSFER_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitTransferFromQueueToServicePointPoolRespectsAppendFlag() {
        log.info("Готовим отделение с точкой обслуживания и пулом");
        Branch branch = new Branch("b1", "Главное отделение");
        ServicePoint sourcePoint = new ServicePoint("sp-source", "Окно №1");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-1");
        sourcePoint.setUser(operator);
        ServicePoint poolPoint = new ServicePoint("sp-pool", "Пул окна №1");
        branch.getServicePoints().put(sourcePoint.getId(), sourcePoint);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        log.info("Создаём визит для внешнего переноса в пул точки обслуживания");
        Visit visit = Visit.builder()
                .id("visit-pool-append")
                .branchId("b1")
                .queueId("queue-legacy")
                .ticket("A777")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));

        log.info("Переводим визит {} в пул {} с добавлением в конец", visit.getId(), poolPoint.getId());
        Visit result = serviceUnderTest.visitTransferFromQueueToServicePointPool(
                "b1", sourcePoint.getId(), poolPoint.getId(), visit, false, 15L);

        log.info("Проверяем, что визит находится в пуле точки обслуживания и задержка установлена");
        assertSame(visit, result);
        assertNull(visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(poolPoint.getId(), visit.getPoolServicePointId());
        assertEquals(15L, visit.getTransferTimeDelay());
        assertNotNull(visit.getTransferDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Boolean> appendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), appendCaptor.capture());
        assertTrue(appendCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, transferEvent);
        assertEquals("queue-legacy", transferEvent.getParameters().get("queueId"));
        assertEquals(poolPoint.getId(), transferEvent.getParameters().get("poolServicePointId"));
        assertEquals(sourcePoint.getId(), transferEvent.getParameters().get("servicePointId"));
        assertEquals("b1", transferEvent.getParameters().get("branchId"));
        assertEquals("user-1", transferEvent.getParameters().get("staffId"));
        assertEquals("Иван Оператор", transferEvent.getParameters().get("staffName"));
        assertEquals("wp-1", transferEvent.getParameters().get("workProfileId"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(15L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("SERVICEPOINT_POOL_REFRESHED", delayedEvent.getEventType());
        Map<String, String> body = (Map<String, String>) delayedEvent.getBody();
        assertEquals(sourcePoint.getId(), body.get("id"));
        assertEquals(poolPoint.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
    }

    @Test
    void visitBackToServicePointPoolStopsServingAndReturnsToPool() {
        log.info("Готовим отделение с точкой обслуживания, пулом и активным визитом");
        Branch branch = new Branch("b1", "Главное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-1", "Окно №1");
        ServicePoint poolPoint = new ServicePoint("sp-pool", "Пул окна №1");
        User operator = new User();
        operator.setId("user-1");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-1");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        Visit visit = Visit.builder()
                .id("visit-return")
                .branchId("b1")
                .ticket("A005")
                .servicePointId(servicePoint.getId())
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("LastPoolServicePointId", poolPoint.getId());
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));

        log.info("Возвращаем визит {} из точки {} обратно в пул {}", visit.getId(), servicePoint.getId(), poolPoint.getId());
        Visit result = serviceUnderTest.visitBackToServicePointPool("b1", servicePoint.getId(), poolPoint.getId(), 60L);

        log.info("Проверяем, что визит освобождён из точки и помещён в пул");
        assertSame(visit, result);
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolUserId());
        assertNull(visit.getQueueId());
        assertEquals(poolPoint.getId(), visit.getPoolServicePointId());
        assertEquals(60L, visit.getReturnTimeDelay());
        assertNotNull(visit.getTransferDateTime());
        assertNotNull(visit.getReturnDateTime());
        assertNull(visit.getStartServingDateTime());
        assertFalse(visit.getParameterMap().containsKey("LastPoolServicePointId"));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest));
        List<VisitEvent> capturedEvents = eventCaptor.getAllValues();
        assertEquals(2, capturedEvents.size());
        VisitEvent stopServing = capturedEvents.get(0);
        assertSame(VisitEvent.STOP_SERVING, stopServing);
        assertEquals("false", stopServing.getParameters().get("isForced"));
        assertEquals("b1", stopServing.getParameters().get("branchId"));
        assertEquals(poolPoint.getId(), stopServing.getParameters().get("poolServicePointId"));
        assertEquals(servicePoint.getId(), stopServing.getParameters().get("servicePointId"));
        assertEquals("user-1", stopServing.getParameters().get("staffId"));
        assertEquals("Иван Оператор", stopServing.getParameters().get("staffName"));

        VisitEvent backEvent = capturedEvents.get(1);
        assertSame(VisitEvent.BACK_TO_SERVICE_POINT_POOL, backEvent);
        assertEquals("b1", backEvent.getParameters().get("branchId"));
        assertEquals(poolPoint.getId(), backEvent.getParameters().get("poolServicePointId"));
        assertEquals(servicePoint.getId(), backEvent.getParameters().get("servicePointId"));
        assertEquals("user-1", backEvent.getParameters().get("staffId"));
        assertEquals("Иван Оператор", backEvent.getParameters().get("staffName"));
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

    private void resetVisitEvents() {
        eventsToReset.forEach(event -> {
            event.getParameters().clear();
            event.dateTime = null;
        });
    }
}
