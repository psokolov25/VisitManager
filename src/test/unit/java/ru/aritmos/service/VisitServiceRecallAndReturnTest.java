package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

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
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.services.DelayedEvents;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.model.visit.VisitEventInformation;
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.service.rules.SegmentationRule;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты ранее непокрытых сценариев {@link VisitService}, связанных с повторным вызовом визита
 * и возвратом в очереди и пулы.
 */
@Slf4j
@ExtendWith(TestLoggingExtension.class)
class VisitServiceRecallAndReturnTest {

    private final List<VisitEvent> eventsToReset = List.of(
            VisitEvent.RECALLED,
            VisitEvent.NO_SHOW,
            VisitEvent.BACK_TO_QUEUE,
            VisitEvent.BACK_TO_USER_POOL,
            VisitEvent.BACK_TO_SERVICE_POINT_POOL);

    @BeforeEach
    void resetEventsBefore() {
        resetVisitEvents();
    }

    @AfterEach
    void resetEventsAfter() {
        resetVisitEvents();
    }

    @Test
    void visitReCallForConfirmUpdatesEventWithServicePointData() {
        log.info("Готовим отделение с точкой обслуживания и оператором для повторного вызова");
        Branch branch = new Branch("br-rc", "Головное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-rc", "Окно повторного вызова");
        User operator = new User();
        operator.setId("staff-rc");
        operator.setName("Мария Повторова");
        operator.setCurrentWorkProfileId("wp-rc");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit visit = Visit.builder()
                .id("visit-rc")
                .branchId(branch.getId())
                .queueId("queue-rc")
                .parameterMap(new HashMap<>(Map.of("isTransferredToStart", "true")))
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = createVisitService(branchService, eventService);

        log.info("Выполняем повторный вызов визита {} в точку {}", visit.getId(), servicePoint.getId());
        Visit result = service.visitReCallForConfirm(branch.getId(), servicePoint.getId(), visit);

        log.info("Проверяем обновление визита и сформированного события повторного вызова");
        assertSame(visit, result);
        assertNotNull(visit.getCallDateTime());
        assertFalse(visit.getParameterMap().containsKey("isTransferredToStart"));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(service));
        VisitEvent event = eventCaptor.getValue();
        assertSame(VisitEvent.RECALLED, event);
        assertEquals(servicePoint.getId(), event.getParameters().get("servicePointId"));
        assertEquals(servicePoint.getName(), event.getParameters().get("servicePointName"));
        assertEquals(branch.getId(), event.getParameters().get("branchId"));
        assertEquals(visit.getQueueId(), event.getParameters().get("queueId"));
        assertEquals(operator.getId(), event.getParameters().get("staffId"));
        assertEquals(operator.getName(), event.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), event.getParameters().get("workProfileId"));
        assertEquals("cherryPick", event.getParameters().get("callMethod"));
        assertNotNull(event.dateTime);
    }

    @Test
    void visitNoShowResetsVisitStateAndProducesEvent() {
        log.info("Готовим визит и точку обслуживания для фиксации неявки клиента");
        Branch branch = new Branch("br-ns", "Отделение у вокзала");
        ServicePoint servicePoint = new ServicePoint("sp-ns", "Окно 7");
        User operator = new User();
        operator.setId("staff-ns");
        operator.setName("Олег Контрольный");
        operator.setCurrentWorkProfileId("wp-ns");
        servicePoint.setUser(operator);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit visit = Visit.builder()
                .id("visit-ns")
                .branchId(branch.getId())
                .status("CALLED")
                .queueId("queue-ns")
                .servicePointId(servicePoint.getId())
                .startServingDateTime(ZonedDateTime.now())
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = createVisitService(branchService, eventService);

        log.info("Фиксируем, что клиент на визит {} не явился", visit.getId());
        Optional<Visit> result = service.visitNoShow(branch.getId(), servicePoint.getId(), visit);

        log.info("Проверяем обновление статуса визита и параметров события неявки");
        assertTrue(result.isPresent());
        assertSame(visit, result.get());
        assertEquals("NO_SHOW", visit.getStatus());
        assertNull(visit.getQueueId());
        assertNull(visit.getServicePointId());
        assertNull(visit.getStartServingDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(service));
        VisitEvent event = eventCaptor.getValue();
        assertSame(VisitEvent.NO_SHOW, event);
        assertEquals(branch.getId(), event.getParameters().get("branchId"));
        assertEquals(servicePoint.getId(), event.getParameters().get("servicePointId"));
        assertEquals(operator.getId(), event.getParameters().get("staffId"));
        assertEquals(operator.getName(), event.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), event.getParameters().get("workProfileId"));
    }

    @Test
    void backCalledVisitReturnsVisitToQueueWithLastServiceData() {
        log.info("Настраиваем отделение с визитом для возврата вызванного клиента в очередь");
        Branch branch = new Branch("br-back", "Северное отделение");
        Service currentService = new Service("srv-back", "Оформление кредита", 300, null);
        Visit visit = Visit.builder()
                .id("visit-back")
                .branchId(branch.getId())
                .queueId("queue-back")
                .currentService(currentService)
                .parameterMap(new HashMap<>())
                .events(new ArrayList<>())
                .build();
        ServicePoint servicePoint = new ServicePoint("sp-back", "Окно возврата");
        servicePoint.setVisit(visit);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        visit.getEvents().add(VisitEventInformation.builder()
                .visitEvent(VisitEvent.CALLED)
                .eventDateTime(ZonedDateTime.now().minusMinutes(1))
                .parameters(Map.of("servicePointId", servicePoint.getId()))
                .build());

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = createVisitService(branchService, eventService);

        log.info("Возвращаем визит {} в очередь после вызова", visit.getId());
        Visit result = service.backCalledVisit(branch.getId(), visit.getId(), 45L);

        log.info("Проверяем заполнение атрибутов возврата и созданного события");
        assertSame(visit, result);
        assertNotNull(visit.getReturnDateTime());
        assertEquals(45L, visit.getReturnTimeDelay());
        assertNull(visit.getStartServingDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(same(visit), eventCaptor.capture(), same(service));
        VisitEvent event = eventCaptor.getValue();
        assertSame(VisitEvent.BACK_TO_QUEUE, event);
        assertEquals(branch.getId(), event.getParameters().get("branchId"));
        assertEquals(servicePoint.getId(), event.getParameters().get("servicePointId"));
        assertEquals(currentService.getId(), event.getParameters().get("serviceId"));
        assertEquals(currentService.getName(), event.getParameters().get("serviceName"));
    }

    @Test
    void visitPutBackDelegatesToServicePointPoolWhenPreviousPoolStored() {
        log.info("Готовим визит в точке обслуживания с последним пулом точки");
        Branch branch = new Branch("br-put", "Западное отделение");
        ServicePoint servicePoint = new ServicePoint("sp-put", "Окно возврата");
        Visit visit = Visit.builder()
                .id("visit-put")
                .branchId(branch.getId())
                .parameterMap(new HashMap<>())
                .build();
        visit.getParameterMap().put("LastPoolServicePointId", "pool-sp-1");
        servicePoint.setVisit(visit);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = spy(createVisitService(branchService, eventService));
        doReturn(visit)
                .when(service)
                .visitBackToServicePointPool(branch.getId(), servicePoint.getId(), "pool-sp-1", 30L);
        doThrow(new AssertionError("Не ожидаем возврат в пул сотрудника"))
                .when(service)
                .visitBackToUserPool(anyString(), anyString(), anyString(), anyLong());
        doThrow(new AssertionError("Не ожидаем возврат в очередь"))
                .when(service)
                .stopServingAndBackToQueue(anyString(), anyString(), anyLong());

        log.info("Возвращаем визит {} в предыдущий пул точки обслуживания", visit.getId());
        Visit result = service.visitPutBack(branch.getId(), servicePoint.getId(), 30L);

        log.info("Проверяем, что выбран путь возврата в пул точки обслуживания");
        assertSame(visit, result);
        verify(service)
                .visitBackToServicePointPool(branch.getId(), servicePoint.getId(), "pool-sp-1", 30L);
        verify(service, never()).visitBackToUserPool(anyString(), anyString(), anyString(), anyLong());
        verify(service, never()).stopServingAndBackToQueue(anyString(), anyString(), anyLong());
    }

    private VisitService createVisitService(BranchService branchService, EventService eventService) {
        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = mock(DelayedEvents.class);
        service.printerService = mock(PrinterService.class);
        service.keyCloackClient = mock(KeyCloackClient.class);
        service.segmentationRule = mock(SegmentationRule.class);
        service.setWaitingTimeCallRule(mock(CallRule.class));
        service.setLifeTimeCallRule(mock(CallRule.class));
        return service;
    }

    private void resetVisitEvents() {
        eventsToReset.forEach(event -> {
            event.getParameters().clear();
            event.dateTime = null;
        });
    }
}
