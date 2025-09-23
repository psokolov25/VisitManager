package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;


import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Optional;
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

import ru.aritmos.exceptions.BusinessException;
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
            VisitEvent.STOP_SERVING,
            VisitEvent.CALLED);

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
        log.info("Наполнение очереди выполняет branchService, в локальной модели список визитов пока пуст");
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


    @Test
    void visitTransferPlacesVisitAtExactIndex() {
        log.info("Подготавливаем отделение для позиционного переноса визита");
        Branch branch = new Branch("br-idx", "Отделение с сортировкой");
        ServicePoint servicePoint = new ServicePoint("sp-idx", "Окно сортировки");
        User operator = new User();
        operator.setId("staff-idx");
        operator.setName("Татьяна Индексатор");
        operator.setCurrentWorkProfileId("wp-idx");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Queue previousQueue = new Queue("queue-prev-idx", "Предыдущая очередь", "P", 4);
        Queue targetQueue = new Queue("queue-target-idx", "Целевая очередь", "T", 4);
        branch.getQueues().put(previousQueue.getId(), previousQueue);
        branch.getQueues().put(targetQueue.getId(), targetQueue);

        Visit visit = Visit.builder()
                .id("visit-indexed")
                .branchId(branch.getId())
                .queueId(previousQueue.getId())
                .servicePointId(servicePoint.getId())
                .ticket("T007")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("LastQueueId", previousQueue.getId());
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));

        log.info("Переносим визит {} на позицию {} очереди {}", visit.getId(), 1, targetQueue.getId());
        Visit result = serviceUnderTest.visitTransfer(
                branch.getId(), servicePoint.getId(), targetQueue.getId(), visit, 1, 60L);

        log.info("Проверяем состояние визита и очереди после переноса");
        assertSame(visit, result);
        assertEquals(targetQueue.getId(), visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(60L, visit.getTransferTimeDelay());
        assertNotNull(visit.getTransferDateTime());
        log.info("Наполнение очереди выполняет branchService, локальный список визитов остаётся пуст");
        assertNull(visit.getParameterMap().get("isTransferredToStart"));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), indexCaptor.capture());
        assertEquals(1, indexCaptor.getValue());
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
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(60L), same(eventService));
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
    void visitTransferFromQueueToUserPoolIncludesExternalServiceInfo() {
        log.info("Готовим отделение для внешнего переноса визита в пул пользователя");
        Branch branch = new Branch("br-user-ext", "Отделение у вокзала");
        User poolUser = new User();
        poolUser.setId("user-ext");
        poolUser.setName("Роман Оператор");
        branch.getUsers().put(poolUser.getId(), poolUser);

        Visit visit = Visit.builder()
                .id("visit-ext-user")
                .branchId(branch.getId())
                .queueId("queue-source")
                .ticket("U011")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("externalSystem", "reception");
        serviceInfo.put("channel", "mobile-app");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        UserRepresentation representation = new UserRepresentation();
        representation.setId("kc-ext-user");
        representation.setUsername("ext-operator");
        when(keyCloackClient.getUserBySid("sid-user-ext")).thenReturn(Optional.of(representation));

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, keyCloackClient);

        log.info("Переводим визит {} в пул пользователя {} с внешними данными", visit.getId(), poolUser.getId());
        Visit result = serviceUnderTest.visitTransferFromQueueToUserPool(
                branch.getId(), poolUser.getId(), visit, false, serviceInfo, 45L, "sid-user-ext");

        log.info("Проверяем состояние визита и сформированное событие");
        assertSame(visit, result);
        assertNull(visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertEquals(poolUser.getId(), visit.getPoolUserId());
        assertEquals(45L, visit.getTransferTimeDelay());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Boolean> appendCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), appendCaptor.capture());
        assertTrue(appendCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_USER_POOL, transferEvent);
        assertEquals("queue-source", transferEvent.getParameters().get("queueId"));
        assertEquals(poolUser.getId(), transferEvent.getParameters().get("userId"));
        assertEquals(branch.getId(), transferEvent.getParameters().get("branchId"));
        assertEquals("kc-ext-user", transferEvent.getParameters().get("staffId"));
        assertEquals("ext-operator", transferEvent.getParameters().get("staffName"));
        assertEquals("reception", transferEvent.getParameters().get("externalSystem"));
        assertEquals("mobile-app", transferEvent.getParameters().get("channel"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(45L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("USER_POOL_REFRESHED", delayedEvent.getEventType());
        assertEquals(poolUser.getId(), delayedEvent.getParams().get("poolUserId"));
        assertEquals(branch.getId(), delayedEvent.getParams().get("branchId"));
        Map<String, String> body = castBody(delayedEvent.getBody());
        assertEquals(poolUser.getId(), body.get("id"));
        assertEquals(poolUser.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitTransferFromQueueToUserPoolPositionsVisitByIndex() {
        log.info("Формируем отделение для позиционного переноса в пул пользователя");
        Branch branch = new Branch("br-user-index", "Отделение с индивидуальным пулом");
        ServicePoint pointWithUser = new ServicePoint("sp-user-index", "Окно оператора");
        User poolUser = new User();
        poolUser.setId("user-index");
        poolUser.setName("Светлана Пуловая");
        poolUser.setCurrentWorkProfileId("wp-user");
        pointWithUser.setUser(poolUser);
        branch.getServicePoints().put(pointWithUser.getId(), pointWithUser);
        branch.getUsers().put(poolUser.getId(), poolUser);

        Visit visit = Visit.builder()
                .id("visit-user-index")
                .branchId(branch.getId())
                .queueId("queue-from")
                .servicePointId(pointWithUser.getId())
                .ticket("I314")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        UserRepresentation representation = new UserRepresentation();
        representation.setId("kc-user-index");
        representation.setUsername("index-operator");
        when(keyCloackClient.getUserBySid("sid-user-index")).thenReturn(Optional.of(representation));

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, keyCloackClient);

        log.info("Переводим визит {} на позицию {} пула пользователя {}", visit.getId(), 2, poolUser.getId());
        Visit result = serviceUnderTest.visitTransferFromQueueToUserPool(
                branch.getId(), poolUser.getId(), visit, 2, 30L, "sid-user-index");

        log.info("Проверяем состояние визита после переноса в пул");
        assertSame(visit, result);
        assertNull(visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertEquals(poolUser.getId(), visit.getPoolUserId());
        assertEquals(30L, visit.getTransferTimeDelay());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest), indexCaptor.capture());
        assertEquals(2, indexCaptor.getValue());
        VisitEvent transferEvent = eventCaptor.getValue();
        assertSame(VisitEvent.TRANSFER_TO_USER_POOL, transferEvent);
        assertEquals("queue-from", transferEvent.getParameters().get("queueId"));
        assertEquals(poolUser.getId(), transferEvent.getParameters().get("userId"));
        assertEquals(branch.getId(), transferEvent.getParameters().get("branchId"));
        assertEquals("kc-user-index", transferEvent.getParameters().get("staffId"));
        assertEquals("index-operator", transferEvent.getParameters().get("staffName"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(30L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("USER_POOL_REFRESHED", delayedEvent.getEventType());
        assertEquals(poolUser.getId(), delayedEvent.getParams().get("poolUserId"));
        assertEquals(branch.getId(), delayedEvent.getParams().get("branchId"));
        Map<String, String> body = castBody(delayedEvent.getBody());
        assertEquals(poolUser.getId(), body.get("id"));
        assertEquals(poolUser.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitCallForConfirmWithMaxWaitingTimeSelectsVisitAndLogsEvent() {
        log.info("Настраиваем отделение для вызова визита с подтверждением");
        Branch branch = new Branch("br-call", "Отделение у парка");
        ServicePoint servicePoint = new ServicePoint("sp-call", "Окно вызова");
        User operator = new User();
        operator.setId("staff-call");
        operator.setName("Наталья Звонкая");
        operator.setCurrentWorkProfileId("wp-call");
        servicePoint.setUser(operator);
        servicePoint.setName("Окно вызова");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit visit = Visit.builder()
                .id("visit-call")
                .branchId(branch.getId())
                .queueId("queue-call")
                .ticket("C303")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));
        CallRule waitingRule = mock(CallRule.class);
        serviceUnderTest.setWaitingTimeCallRule(waitingRule);
        when(waitingRule.call(branch, servicePoint)).thenReturn(Optional.of(visit));

        log.info("Вызываем визит {} на точку обслуживания {}", visit.getId(), servicePoint.getId());
        Optional<Visit> result = serviceUnderTest.visitCallForConfirmWithMaxWaitingTime(branch.getId(), servicePoint.getId());

        log.info("Проверяем, что визит вызван и событие зафиксировано");
        assertTrue(result.isPresent());
        assertSame(visit, result.get());
        verify(waitingRule).call(branch, servicePoint);

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest));
        VisitEvent calledEvent = eventCaptor.getValue();
        assertSame(VisitEvent.CALLED, calledEvent);
        assertEquals(servicePoint.getId(), calledEvent.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), calledEvent.getParameters().get("servicePointName"));
        assertEquals(branch.getId(), calledEvent.getParameters().get("branchId"));
        log.info("Убеждаемся, что реализация не заполняет идентификатор очереди в событии вызова");
        assertFalse(calledEvent.getParameters().containsKey("queueId"));
        assertEquals(operator.getId(), calledEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), calledEvent.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), calledEvent.getParameters().get("workProfileId"));
        assertEquals("callNext", calledEvent.getParameters().get("callMethod"));
    }

    @Test
    void visitCallForConfirmWithMaxWaitingTimeActivatesAutoCallWhenQueueIsEmpty() {
        log.info("Проверяем включение автодовызова при отсутствии доступных визитов");
        Branch branch = new Branch("br-auto-call", "Отделение автодовызова");
        ServicePoint servicePoint = new ServicePoint("sp-auto-call", "Окно автодовызова");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getParameterMap().put("autoCallMode", "true");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));
        CallRule waitingRule = mock(CallRule.class);
        serviceUnderTest.setWaitingTimeCallRule(waitingRule);
        when(waitingRule.call(branch, servicePoint)).thenReturn(Optional.empty());

        log.info("Запускаем вызов визита при включённом режиме автодовызова");
        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> serviceUnderTest.visitCallForConfirmWithMaxWaitingTime(branch.getId(), servicePoint.getId()));

        log.info("Проверяем, что автодовызов активирован и сохранён в конфигурации");
        assertEquals("Automatic call mode is enabled", exception.getMessage());
        ServicePoint updatedServicePoint = branch.getServicePoints().get(servicePoint.getId());
        assertTrue(Boolean.TRUE.equals(updatedServicePoint.getAutoCallMode()));
        verify(branchService).add(branch.getId(), branch);
    }

    @Test
    void visitCallForConfirmWithMaxWaitingTimeWithQueuesSelectsVisit() {
        log.info("Настраиваем отделение для вызова визита по списку очередей");
        Branch branch = new Branch("br-call-queues", "Отделение у вокзала");
        ServicePoint servicePoint = new ServicePoint("sp-call-queues", "Окно очередей");
        User operator = new User();
        operator.setId("staff-queues");
        operator.setName("Олег Очередной");
        operator.setCurrentWorkProfileId("wp-queues");
        servicePoint.setUser(operator);
        servicePoint.setName("Окно очередей");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit visit = Visit.builder()
                .id("visit-call-queues")
                .branchId(branch.getId())
                .queueId("queue-alpha")
                .ticket("Q515")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        List<String> queueIds = List.of("queue-alpha", "queue-beta");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));
        CallRule waitingRule = mock(CallRule.class);
        serviceUnderTest.setWaitingTimeCallRule(waitingRule);
        when(waitingRule.call(branch, servicePoint, queueIds)).thenReturn(Optional.of(visit));

        log.info("Вызываем визит {} по списку очередей {}", visit.getId(), queueIds);
        Optional<Visit> result = serviceUnderTest.visitCallForConfirmWithMaxWaitingTime(
                branch.getId(), servicePoint.getId(), queueIds);

        log.info("Проверяем параметры события вызова");
        assertTrue(result.isPresent());
        assertSame(visit, result.get());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(serviceUnderTest));
        VisitEvent calledEvent = eventCaptor.getValue();
        assertSame(VisitEvent.CALLED, calledEvent);
        assertEquals(servicePoint.getId(), calledEvent.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), calledEvent.getParameters().get("servicePointName"));
        assertEquals(branch.getId(), calledEvent.getParameters().get("branchId"));
        assertEquals(operator.getId(), calledEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), calledEvent.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), calledEvent.getParameters().get("workProfileId"));
        assertEquals("callNext", calledEvent.getParameters().get("callMethod"));
    }

    @Test
    void visitCallForConfirmWithMaxWaitingTimeWithQueuesEnablesAutoCallMode() {
        log.info("Проверяем включение автодовызова при отсутствии визитов в указанных очередях");
        Branch branch = new Branch("br-auto-queues", "Отделение очередей");
        ServicePoint servicePoint = new ServicePoint("sp-auto-queues", "Окно очередей автодовызова");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        branch.getParameterMap().put("autoCallMode", "true");

        List<String> queueIds = List.of("queue-auto");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService serviceUnderTest = createVisitService(branchService, eventService, delayedEvents, mock(KeyCloackClient.class));
        CallRule waitingRule = mock(CallRule.class);
        serviceUnderTest.setWaitingTimeCallRule(waitingRule);
        when(waitingRule.call(branch, servicePoint, queueIds)).thenReturn(Optional.empty());

        log.info("Запускаем вызов визита по очередям {} при включённом автодовызове", queueIds);
        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> serviceUnderTest.visitCallForConfirmWithMaxWaitingTime(
                        branch.getId(), servicePoint.getId(), queueIds));

        log.info("Убеждаемся, что режим автодовызова активирован и сохранён");
        assertEquals("Automatic call mode is enabled", exception.getMessage());
        ServicePoint updatedServicePoint = branch.getServicePoints().get(servicePoint.getId());
        assertTrue(Boolean.TRUE.equals(updatedServicePoint.getAutoCallMode()));
        verify(branchService).add(branch.getId(), branch);
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
