package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.DelayedEvents;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.service.rules.SegmentationRule;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты для ранее непокрытых сценариев {@link VisitService}, связанных с вызовом визита и
 * возвратом в очередь.
 */
@Slf4j
@ExtendWith(TestLoggingExtension.class)
class VisitServiceCallAndReturnScenariosTest {

    private final List<VisitEvent> eventsToReset = List.of(
            VisitEvent.CALLED,
            VisitEvent.STOP_SERVING,
            VisitEvent.BACK_TO_QUEUE);

    @BeforeEach
    void resetEventsBefore() {
        resetVisitEvents();
    }

    @AfterEach
    void resetEventsAfter() {
        resetVisitEvents();
    }

    @DisplayName("Вызов визита на подтверждение с максимальным ожиданием обновляет визит и событие")
    @Test
    void visitCallForConfirmWithMaxWaitingTimeUpdatesVisitAndEvent() {
        log.info("Готовим отделение, точку обслуживания и визит для вызова с подтверждением");
        Branch branch = new Branch("br-1", "Центральное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-1", "Окно №1");
        User operator = new User();
        operator.setId("staff-1");
        operator.setName("Иван Оператор");
        operator.setCurrentWorkProfileId("wp-1");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit visit = Visit.builder()
                .id("visit-1")
                .branchId(branch.getId())
                .queueId("queue-1")
                .poolServicePointId("pool-sp-1")
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("isTransferredToStart", "true");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = createVisitService(
                branchService, eventService, mock(DelayedEvents.class), mock(CallRule.class), mock(CallRule.class));

        log.info("Вызываем визит {} в точку {}", visit.getId(), servicePoint.getId());
        Optional<Visit> result = service.visitCallForConfirmWithMaxWaitingTime(
                branch.getId(), servicePoint.getId(), visit);

        log.info("Проверяем, что визит помечен как вызванный и событие сформировано корректно");
        assertTrue(result.isPresent());
        assertSame(visit, result.get());
        assertEquals("queue-1", visit.getParameterMap().get("LastQueueId"));
        assertFalse(visit.getParameterMap().containsKey("isTransferredToStart"));
        assertNotNull(visit.getCallDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(service));
        VisitEvent event = eventCaptor.getValue();
        assertSame(VisitEvent.CALLED, event);
        assertEquals(servicePoint.getId(), event.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), event.getParameters().get("servicePointName"));
        assertEquals(branch.getId(), event.getParameters().get("branchId"));
        assertEquals("queue-1", event.getParameters().get("queueId"));
        assertEquals("pool-sp-1", event.getParameters().get("PoolServicePointId"));
        assertEquals(operator.getId(), event.getParameters().get("staffId"));
        assertEquals(operator.getName(), event.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), event.getParameters().get("workProfileId"));
        assertEquals("callNext", event.getParameters().get("callMethod"));
    }

    @DisplayName("Вызов визита на подтверждение с максимальным временем жизни вызывает правило и обновляет визит")
    @Test
    void visitCallForConfirmWithMaxLifeTimeCallsRuleAndUpdatesVisit() {
        log.info("Настраиваем отделение и визит для вызова по максимальному времени жизни");
        Branch branch = new Branch("br-2", "Отделение на площади");
        ServicePoint servicePoint = new ServicePoint("sp-2", "Окно №2");
        User operator = new User();
        operator.setId("staff-2");
        operator.setName("Анна Специалист");
        operator.setCurrentWorkProfileId("wp-2");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit visit = Visit.builder()
                .id("visit-2")
                .branchId(branch.getId())
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule lifeTimeRule = mock(CallRule.class);
        when(lifeTimeRule.call(branch, servicePoint)).thenReturn(Optional.of(visit));

        VisitService service = createVisitService(
                branchService, eventService, mock(DelayedEvents.class), mock(CallRule.class), lifeTimeRule);

        log.info("Вызываем визит по правилу максимального времени жизни");
        Optional<Visit> result = service.visitCallForConfirmWithMaxLifeTime(branch.getId(), servicePoint.getId());

        log.info("Проверяем, что визит получен от правила и событие вызова обновлено");
        assertTrue(result.isPresent());
        assertSame(visit, result.get());
        verify(lifeTimeRule).call(same(branch), same(servicePoint));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(service));
        VisitEvent event = eventCaptor.getValue();
        assertSame(VisitEvent.CALLED, event);
        assertEquals(servicePoint.getId(), event.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), event.getParameters().get("servicePointName"));
        assertEquals(branch.getId(), event.getParameters().get("branchId"));
        assertEquals(operator.getId(), event.getParameters().get("staffId"));
        assertEquals(operator.getName(), event.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), event.getParameters().get("workProfileId"));
        assertEquals("callNext", event.getParameters().get("callMethod"));
    }

    @DisplayName("Вызов визита на подтверждение с ограничением времени и очередями распространяет идентификаторы")
    @Test
    void visitCallForConfirmWithMaxLifeTimeAndQueuesPropagatesIds() {
        log.info("Подготавливаем отделение с несколькими очередями для вызова визита");
        Branch branch = new Branch("br-3", "Отделение у вокзала");
        ServicePoint servicePoint = new ServicePoint("sp-3", "Окно №3");
        User operator = new User();
        operator.setId("staff-3");
        operator.setName("Пётр Консультант");
        operator.setCurrentWorkProfileId("wp-3");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        List<String> queueIds = List.of("queue-a", "queue-b");
        Visit visit = Visit.builder()
                .id("visit-3")
                .branchId(branch.getId())
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        CallRule lifeTimeRule = mock(CallRule.class);
        when(lifeTimeRule.call(branch, servicePoint, queueIds)).thenReturn(Optional.of(visit));

        VisitService service = createVisitService(
                branchService, eventService, mock(DelayedEvents.class), mock(CallRule.class), lifeTimeRule);

        log.info("Запускаем вызов по максимальному времени жизни с фильтром очередей {}", queueIds);
        Optional<Visit> result = service.visitCallForConfirmWithMaxLifeTime(
                branch.getId(), servicePoint.getId(), queueIds);

        log.info("Проверяем, что правило вызвано с нужными очередями и событие подготовлено");
        assertTrue(result.isPresent());
        assertSame(visit, result.get());
        verify(lifeTimeRule).call(same(branch), same(servicePoint), eq(queueIds));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(service));
        VisitEvent event = eventCaptor.getValue();
        assertSame(VisitEvent.CALLED, event);
        assertEquals(servicePoint.getId(), event.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), event.getParameters().get("servicePointName"));
        assertEquals(branch.getId(), event.getParameters().get("branchId"));
        assertEquals(operator.getId(), event.getParameters().get("staffId"));
        assertEquals(operator.getName(), event.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), event.getParameters().get("workProfileId"));
        assertEquals("callNext", event.getParameters().get("callMethod"));
    }

    @DisplayName("Завершение обслуживания и возврат в очередь планируют отложенное обновление")
    @Test
    void stopServingAndBackToQueueSchedulesDelayedRefresh() {
        log.info("Формируем отделение с очередью и визитом, находящимся на обслуживании");
        Branch branch = new Branch("br-4", "Отделение у реки");
        ServicePoint servicePoint = new ServicePoint("sp-4", "Окно №4");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Queue queue = new Queue("queue-main", "Основная очередь", "A", 10);
        branch.getQueues().put(queue.getId(), queue);

        Service currentService = new Service("svc-1", "Оформление", 300, queue.getId());
        Visit visit = Visit.builder()
                .id("visit-4")
                .branchId(branch.getId())
                .queueId(queue.getId())
                .ticket("A100")
                .currentService(currentService)
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        visit.getParameterMap().put("LastQueueId", queue.getId());
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = createVisitService(
                branchService, eventService, delayedEvents, mock(CallRule.class), mock(CallRule.class));

        log.info("Останавливаем обслуживание визита {} с возвратом в очередь", visit.getId());
        Visit result = service.stopServingAndBackToQueue(branch.getId(), servicePoint.getId(), 45L);

        log.info("Проверяем, что визит возвращён в очередь и отложенное событие отправлено");
        assertSame(visit, result);
        assertEquals(queue.getId(), visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(45L, visit.getReturnTimeDelay());
        assertNotNull(visit.getReturnDateTime());
        assertTrue(queue.getVisits().contains(visit));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(service));
        List<VisitEvent> events = eventCaptor.getAllValues();
        assertEquals(2, events.size());
        VisitEvent stopEvent = events.get(0);
        assertSame(VisitEvent.STOP_SERVING, stopEvent);
        assertEquals("false", stopEvent.getParameters().get("isForced"));
        assertEquals(servicePoint.getId(), stopEvent.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), stopEvent.getParameters().get("servicePointName"));
        assertEquals(branch.getId(), stopEvent.getParameters().get("branchId"));
        VisitEvent backEvent = events.get(1);
        assertSame(VisitEvent.BACK_TO_QUEUE, backEvent);
        assertEquals(queue.getId(), backEvent.getParameters().get("queueId"));
        assertEquals(branch.getId(), backEvent.getParameters().get("branchId"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents).delayedEventService(
                eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(45L), same(eventService));
        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("QUEUE_REFRESHED", delayedEvent.getEventType());
        assertEquals(queue.getId(), delayedEvent.getParams().get("queueId"));
        assertEquals(branch.getId(), delayedEvent.getParams().get("branchId"));
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) delayedEvent.getBody();
        assertEquals(queue.getId(), body.get("id"));
        assertEquals(queue.getName(), body.get("name"));
        assertEquals("RETURN_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @DisplayName("Возврат визита перемещает его в очередь и сбрасывает назначения")
    @Test
    void visitBackMovesVisitToQueueAndResetsAssignments() {
        log.info("Готовим отделение и визит для возврата в очередь без задержки");
        Branch branch = new Branch("br-5", "Северное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-5", "Окно №5");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Queue queue = new Queue("queue-2", "Очередь обслуживания", "B", 5);
        branch.getQueues().put(queue.getId(), queue);

        User operator = new User();
        operator.setId("staff-5");
        operator.setName("Мария Специалист");
        servicePoint.setUser(operator);

        Service currentService = new Service("svc-2", "Консультация", 200, queue.getId());
        Visit visit = Visit.builder()
                .id("visit-5")
                .branchId(branch.getId())
                .queueId(queue.getId())
                .ticket("B007")
                .currentService(currentService)
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        servicePoint.setVisit(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = createVisitService(
                branchService, eventService, mock(DelayedEvents.class), mock(CallRule.class), mock(CallRule.class));

        log.info("Возвращаем визит {} в очередь {}", visit.getId(), queue.getId());
        Visit result = service.visitBack(branch.getId(), servicePoint.getId(), queue.getId(), 30L);

        log.info("Проверяем, что визит перемещён в очередь и событие возврата создано");
        assertSame(visit, result);
        assertEquals(queue.getId(), visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertEquals(30L, visit.getReturnTimeDelay());
        assertNotNull(visit.getReturnDateTime());
        assertTrue(queue.getVisits().contains(visit));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(service));
        VisitEvent event = eventCaptor.getValue();
        assertSame(VisitEvent.BACK_TO_QUEUE, event);
        assertEquals(branch.getId(), event.getParameters().get("branchId"));
        assertEquals(queue.getId(), event.getParameters().get("queueId"));
        assertEquals(servicePoint.getId(), event.getParameters().get("servicePointId"));
        assertEquals(operator.getId(), event.getParameters().get("staffId"));
        assertEquals(operator.getName(), event.getParameters().get("staffName"));
    }

    private VisitService createVisitService(
            BranchService branchService,
            EventService eventService,
            DelayedEvents delayedEvents,
            CallRule waitingTimeRule,
            CallRule lifeTimeRule) {
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;
        service.printerService = mock(PrinterService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        service.segmentationRule = mock(SegmentationRule.class);
        service.setWaitingTimeCallRule(waitingTimeRule);
        service.setLifeTimeCallRule(lifeTimeRule);
        return service;
    }

    private void resetVisitEvents() {
        eventsToReset.forEach(event -> {
            event.getParameters().clear();
            event.dateTime = null;
        });
    }
}
