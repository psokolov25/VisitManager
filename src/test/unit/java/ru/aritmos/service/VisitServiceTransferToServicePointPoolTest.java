package ru.aritmos.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

/**
 * Набор тестов для {@link VisitService#visitTransferToServicePointPool(String, String, String,
 * HashMap, Long)}.
 */
class VisitServiceTransferToServicePointPoolTest {

    @BeforeEach
    void resetVisitEventParameters() {
        VisitEvent.STOP_SERVING.getParameters().clear();
        VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL.getParameters().clear();
    }

    @Test
    void visitTransferToServicePointPoolMovesVisitAndPublishesEvents() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sourcePoint = new ServicePoint("sp-main", "Главная ТО");
        User operator = new User("staff-1", "Оператор", null);
        operator.setCurrentWorkProfileId("wp-1");
        sourcePoint.setUser(operator);

        ServicePoint poolPoint = new ServicePoint("sp-pool", "Пул ТО");

        Map<String, String> previousParams = Map.of("queueId", "queue-history");
        VisitEventInformation previousEvent = VisitEventInformation.builder()
                .visitEvent(VisitEvent.TRANSFER_TO_QUEUE)
                .eventDateTime(ZonedDateTime.now().minusMinutes(5))
                .parameters(previousParams)
                .build();

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("LastPoolServicePointId", "old-pool");
        parameters.put("LastQueueId", "queue-last");

        Visit visit = Visit.builder()
                .id("visit-1")
                .ticket("A-001")
                .servicePointId(sourcePoint.getId())
                .queueId("queue-initial")
                .parameterMap(parameters)
                .visitEvents(new ArrayList<>())
                .events(new ArrayList<>(List.of(previousEvent)))
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

        HashMap<String, String> serviceInfo = new HashMap<>();
        serviceInfo.put("externalServiceId", "srv-77");
        serviceInfo.put("externalSystem", "crm");

        Visit result = service.visitTransferToServicePointPool(
                "b1", sourcePoint.getId(), poolPoint.getId(), serviceInfo, 45L);

        assertSame(visit, result);
        assertNull(result.getServicePointId());
        assertNull(result.getQueueId());
        assertNull(result.getPoolUserId());
        assertEquals(poolPoint.getId(), result.getPoolServicePointId());
        assertEquals(45L, result.getTransferTimeDelay());
        assertNotNull(result.getTransferDateTime());
        assertNull(result.getStartServingDateTime());
        assertFalse(result.getParameterMap().containsKey("LastPoolServicePointId"));
        assertEquals("queue-last", result.getParameterMap().get("LastQueueId"));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(eq(visit), eventCaptor.capture(), eq(service));

        List<VisitEvent> capturedEvents = eventCaptor.getAllValues();
        VisitEvent stopEvent = capturedEvents.get(0);
        assertEquals(VisitEvent.STOP_SERVING, stopEvent);
        assertEquals("false", stopEvent.getParameters().get("isForced"));
        assertEquals("b1", stopEvent.getParameters().get("branchId"));
        assertEquals(poolPoint.getId(), stopEvent.getParameters().get("poolServicePointId"));
        assertEquals(sourcePoint.getId(), stopEvent.getParameters().get("servicePointId"));
        assertEquals(sourcePoint.getName(), stopEvent.getParameters().get("servicePointName"));
        assertEquals(operator.getId(), stopEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), stopEvent.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), stopEvent.getParameters().get("workProfileId"));
        assertEquals("srv-77", stopEvent.getParameters().get("externalServiceId"));
        assertEquals("crm", stopEvent.getParameters().get("externalSystem"));

        VisitEvent transferEvent = capturedEvents.get(1);
        assertEquals(VisitEvent.TRANSFER_TO_SERVICE_POINT_POOL, transferEvent);
        assertEquals("b1", transferEvent.getParameters().get("branchId"));
        assertEquals(poolPoint.getId(), transferEvent.getParameters().get("poolServicePointId"));
        assertEquals(sourcePoint.getId(), transferEvent.getParameters().get("servicePointId"));
        assertEquals(operator.getId(), transferEvent.getParameters().get("staffId"));
        assertEquals(operator.getName(), transferEvent.getParameters().get("staffName"));
        assertEquals(operator.getCurrentWorkProfileId(), transferEvent.getParameters().get("workProfileId"));
        assertEquals("queue-history", transferEvent.getParameters().get("queueId"));
        assertEquals("srv-77", transferEvent.getParameters().get("externalServiceId"));
        assertEquals("crm", transferEvent.getParameters().get("externalSystem"));

        ArgumentCaptor<Event> delayedEventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(delayedEvents)
                .delayedEventService(eq("frontend"), eq(false), delayedEventCaptor.capture(), eq(45L), eq(eventService));

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
    void visitTransferToServicePointPoolFailsWhenServicePointMissing() {
        Branch branch = new Branch("b1", "Branch");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = mock(DelayedEvents.class);

        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                service.visitTransferToServicePointPool("b1", "sp-main", "sp-pool", new HashMap<>(), 5L));
        assertEquals("Service point sp-main does not exist", exception.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), eq(service));
        verifyNoInteractions(service.delayedEvents);
    }

    @Test
    void visitTransferToServicePointPoolFailsWhenVisitMissing() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint servicePoint = new ServicePoint("sp-main", "Главная ТО");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.delayedEvents = mock(DelayedEvents.class);

        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                service.visitTransferToServicePointPool("b1", servicePoint.getId(), "sp-pool", new HashMap<>(), 5L));
        assertEquals("Visit in service point sp-main does not exist", exception.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), eq(service));
        verifyNoInteractions(service.delayedEvents);
    }

    @Test
    void visitTransferToServicePointPoolFailsWhenPoolPointMissing() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sourcePoint = new ServicePoint("sp-main", "Главная ТО");
        Visit visit = Visit.builder()
                .id("visit-1")
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

        HttpStatusException exception = assertThrows(HttpStatusException.class, () ->
                service.visitTransferToServicePointPool("b1", sourcePoint.getId(), "sp-pool", new HashMap<>(), 5L));
        assertEquals("Service point not found in branch configuration", exception.getMessage());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), eq(service));
        verifyNoInteractions(service.delayedEvents);
    }
}

