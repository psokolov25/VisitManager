package ru.aritmos.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.DelayedEvents;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.model.visit.VisitEventInformation;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты для {@link VisitService#visitTransferToServicePointPool(String, String, String, Long)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceTransferToServicePointPoolSimpleTest {

    @BeforeEach
    void resetVisitEventParameters() {
        VisitEvent.STOP_SERVING.getParameters().clear();
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getParameters().clear();
    }

    @Test
    void visitTransferToServicePointPoolMovesVisitAndSchedulesRefresh() {
        Branch branch = new Branch("b1", "Отделение");
        ServicePoint sourcePoint = new ServicePoint("sp-main", "Главное окно");
        User operator = new User("staff-1", "Оператор", null);
        operator.setCurrentWorkProfileId("wp-1");
        sourcePoint.setUser(operator);

        ServicePoint poolPoint = new ServicePoint("sp-pool", "Пул ТО");

        Map<String, String> historyParams = Map.of("oldQueueId", "queue-hist");
        VisitEventInformation historyEvent = VisitEventInformation.builder()
                .visitEvent(VisitEvent.TRANSFER_TO_QUEUE)
                .eventDateTime(ZonedDateTime.now().minusMinutes(3))
                .parameters(historyParams)
                .build();

        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put("LastPoolServicePointId", "pool-old");
        parameterMap.put("LastQueueId", "queue-latest");

        Visit visit = Visit.builder()
                .id("visit-1")
                .ticket("A-007")
                .servicePointId(sourcePoint.getId())
                .queueId("queue-current")
                .poolUserId("pool-user")
                .parameterMap(parameterMap)
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>(List.of(historyEvent)))
                .startServingDateTime(ZonedDateTime.now().minusMinutes(1))
                .build();
        sourcePoint.setVisit(visit);

        branch.getServicePoints().put(sourcePoint.getId(), sourcePoint);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;

        Visit result = service.visitTransferToServicePointPool("b1", sourcePoint.getId(), poolPoint.getId(), 30L);

        assertSame(visit, result);
        assertNull(result.getServicePointId());
        assertNull(result.getQueueId());
        assertNull(result.getPoolUserId());
        assertEquals(poolPoint.getId(), result.getPoolServicePointId());
        assertEquals(30L, result.getTransferTimeDelay());
        assertNotNull(result.getTransferDateTime());
        assertNull(result.getStartServingDateTime());
        assertFalse(result.getParameterMap().containsKey("LastPoolServicePointId"));
        assertEquals("queue-latest", result.getParameterMap().get("LastQueueId"));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(service));

        List<VisitEvent> capturedEvents = eventCaptor.getAllValues();
        assertEquals(2, capturedEvents.size());

        VisitEvent stopEvent = capturedEvents.get(0);
        assertSame(VisitEvent.STOP_SERVING, stopEvent);
        Map<String, String> stopParams = new HashMap<>(stopEvent.getParameters());
        assertEquals("false", stopParams.get("isForced"));
        assertEquals("b1", stopParams.get("branchId"));
        assertEquals(poolPoint.getId(), stopParams.get("poolServicePointId"));
        assertEquals(sourcePoint.getId(), stopParams.get("servicePointId"));
        assertEquals(sourcePoint.getName(), stopParams.get("servicePointName"));
        assertEquals(operator.getId(), stopParams.get("staffId"));
        assertEquals(operator.getName(), stopParams.get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), stopParams.get("workProfileId"));
        assertFalse(stopParams.containsKey("externalServiceId"));

        VisitEvent transferEvent = capturedEvents.get(1);
        assertSame(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, transferEvent);
        Map<String, String> transferParams = new HashMap<>(transferEvent.getParameters());
        assertEquals("b1", transferParams.get("branchId"));
        assertEquals(poolPoint.getId(), transferParams.get("poolServicePointId"));
        assertEquals(sourcePoint.getId(), transferParams.get("servicePointId"));
        assertEquals(operator.getId(), transferParams.get("staffId"));
        assertEquals(operator.getName(), transferParams.get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), transferParams.get("workProfileId"));
        assertEquals("queue-hist", transferParams.get("queueId"));
        assertFalse(transferParams.containsKey("externalServiceId"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents)
                .delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(30L), same(eventService));

        Event delayedEvent = delayedEventCaptor.getValue();
        assertEquals("SERVICEPOINT_POOL_REFRESHED", delayedEvent.getEventType());
        assertEquals(Map.of("poolServicePointId", poolPoint.getId(), "branchId", branch.getId()), delayedEvent.getParams());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) delayedEvent.getBody();
        assertEquals(poolPoint.getId(), body.get("id"));
        assertEquals(poolPoint.getName(), body.get("name"));
        assertEquals(branch.getId(), body.get("branchId"));
        assertEquals("TRANSFER_TIME_DELAY_FINISHED", body.get("reason"));
        assertEquals(visit.getId(), body.get("visitId"));
        assertEquals(visit.getTicket(), body.get("ticket"));
    }

    @Test
    void visitTransferToServicePointPoolOmitsQueueParameterWhenHistoryMissing() {
        Branch branch = new Branch("b1", "Отделение");
        ServicePoint sourcePoint = new ServicePoint("sp-main", "Главное окно");
        User operator = new User("staff-2", "Оператор 2", null);
        operator.setCurrentWorkProfileId("wp-2");
        sourcePoint.setUser(operator);

        ServicePoint poolPoint = new ServicePoint("sp-pool", "Пул ТО");

        HashMap<String, String> parameterMap = new HashMap<>();
        parameterMap.put("LastPoolServicePointId", "pool-old");

        Visit visit = Visit.builder()
                .id("visit-2")
                .ticket("A-008")
                .servicePointId(sourcePoint.getId())
                .parameterMap(parameterMap)
                .poolUserId("pool-user")
                .startServingDateTime(ZonedDateTime.now())
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sourcePoint.setVisit(visit);

        branch.getServicePoints().put(sourcePoint.getId(), sourcePoint);
        branch.getServicePoints().put(poolPoint.getId(), poolPoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);
        DelayedEvents delayedEvents = mock(DelayedEvents.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = delayedEvents;

        Visit result = service.visitTransferToServicePointPool("b1", sourcePoint.getId(), poolPoint.getId(), 15L);

        assertSame(visit, result);
        assertNull(result.getServicePointId());
        assertNull(result.getQueueId());
        assertNull(result.getPoolUserId());
        assertEquals(poolPoint.getId(), result.getPoolServicePointId());
        assertEquals(15L, result.getTransferTimeDelay());
        assertNull(result.getStartServingDateTime());
        assertFalse(result.getParameterMap().containsKey("LastPoolServicePointId"));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(service));

        VisitEvent transferEvent = eventCaptor.getAllValues().get(1);
        Map<String, String> transferParams = new HashMap<>(transferEvent.getParameters());
        assertFalse(transferParams.containsKey("queueId"));

        verify(delayedEvents)
                .delayedEventService(eq("frontend"), eq(false), any(Event.class), eq(15L), same(eventService));
    }

    @Test
    void visitTransferToServicePointPoolFailsWhenServicePointMissing() {
        Branch branch = new Branch("b1", "Отделение");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = mock(DelayedEvents.class);

        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.visitTransferToServicePointPool("b1", "sp-main", "sp-pool", 5L));
        assertEquals("ServicePoint sp-main! not exist!", exception.getMessage());

        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), same(service));
        verifyNoInteractions(service.delayedEvents);
    }

    @Test
    void visitTransferToServicePointPoolFailsWhenVisitMissing() {
        Branch branch = new Branch("b1", "Отделение");
        ServicePoint servicePoint = new ServicePoint("sp-main", "Главное окно");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = mock(DelayedEvents.class);

        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.visitTransferToServicePointPool("b1", servicePoint.getId(), "sp-pool", 5L));
        assertEquals("Visit in ServicePoint sp-main! not exist!", exception.getMessage());

        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), same(service));
        verifyNoInteractions(service.delayedEvents);
    }

    @Test
    void visitTransferToServicePointPoolFailsWhenPoolPointMissing() {
        Branch branch = new Branch("b1", "Отделение");
        ServicePoint sourcePoint = new ServicePoint("sp-main", "Главное окно");
        Visit visit = Visit.builder()
                .id("visit-3")
                .parameterMap(new HashMap<>())
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sourcePoint.setVisit(visit);
        branch.getServicePoints().put(sourcePoint.getId(), sourcePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = mock(DelayedEvents.class);

        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.visitTransferToServicePointPool("b1", sourcePoint.getId(), "sp-pool", 5L));
        assertEquals("Service point not found in branch configuration!", exception.getMessage());

        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), same(service));
        verifyNoInteractions(service.delayedEvents);
    }
}
