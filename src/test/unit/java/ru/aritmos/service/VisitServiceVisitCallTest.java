package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Юнит-тесты для {@link VisitService#visitCall(String, String, Visit, String)} и связанных сценариев
 * вызова визита.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceVisitCallTest {

    @BeforeEach
    void clearVisitEventParametersBeforeTest() {
        clearVisitEventParameters();
    }

    @AfterEach
    void clearVisitEventParametersAfterTest() {
        clearVisitEventParameters();
    }

    private void clearVisitEventParameters() {
        VisitEvent.CALLED.getParameters().clear();
        VisitEvent.START_SERVING.getParameters().clear();
    }

    @Test
    void visitCallMovesVisitFromQueueAndPublishesEvents() {
        Branch branch = new Branch("b1", "Отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно 1");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        Queue queue = new Queue("q1", "Основная очередь", "A", 300);
        branch.getQueues().put(queue.getId(), queue);

        Visit visit = Visit.builder()
                .id("v1")
                .branchId("b1")
                .queueId("q1")
                .parameterMap(new HashMap<>())
                .build();
        visit.getParameterMap().put("isTransferredToStart", "true");
        queue.getVisits().add(visit);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Optional<Visit> result = service.visitCall("b1", "sp1", visit, "auto");

        assertTrue(result.isPresent());
        assertSame(visit, result.get());
        assertEquals("CALLED", visit.getStatus());
        assertNotNull(visit.getCallDateTime());
        assertEquals("sp1", visit.getServicePointId());
        assertNull(visit.getQueueId());
        assertFalse(visit.getParameterMap().containsKey("isTransferredToStart"));
        assertEquals("q1", visit.getParameterMap().get("LastQueueId"));
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
        assertSame(visit, servicePoint.getVisit());
        assertTrue(queue.getVisits().isEmpty());
        assertNotNull(visit.getStartServingDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(service));
        List<VisitEvent> capturedEvents = eventCaptor.getAllValues();
        assertEquals(2, capturedEvents.size());

        VisitEvent calledEvent = capturedEvents.get(0);
        assertSame(VisitEvent.CALLED, calledEvent);
        Map<String, String> calledParams = new HashMap<>(calledEvent.getParameters());
        assertEquals("sp1", calledParams.get("servicePointId"));
        assertEquals("Окно 1", calledParams.get("servicePointName"));
        assertEquals("b1", calledParams.get("branchId"));
        assertEquals("auto", calledParams.get("callMethod"));
        assertEquals("", calledParams.get("staffId"));
        assertEquals("", calledParams.get("staffName"));
        assertEquals("", calledParams.get("workProfileId"));

        VisitEvent startServingEvent = capturedEvents.get(1);
        assertSame(VisitEvent.START_SERVING, startServingEvent);
    }

    @Test
    void visitCallFailsWhenServicePointBusy() {
        Branch branch = new Branch("b1", "Отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно 1");
        servicePoint.setVisit(Visit.builder().id("occupied").parameterMap(new HashMap<>()).build());
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Visit visit = Visit.builder().id("v1").branchId("b1").parameterMap(new HashMap<>()).build();

        HttpStatusException exception = assertThrows(
                HttpStatusException.class, () -> service.visitCall("b1", "sp1", visit, "auto"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), same(service));
    }

    @Test
    void visitCallFailsWhenServicePointMissing() {
        Branch branch = new Branch("b1", "Отделение");
        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Visit visit = Visit.builder().id("v1").branchId("b1").parameterMap(new HashMap<>()).build();

        HttpStatusException exception = assertThrows(
                HttpStatusException.class, () -> service.visitCall("b1", "missing", visit, "auto"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).updateVisit(any(Visit.class), any(VisitEvent.class), same(service));
    }

    @Test
    void visitCallTransfersPoolMetadataToParameters() {
        Branch branch = new Branch("b1", "Отделение");
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно 1");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        Visit visit = Visit.builder()
                .id("v1")
                .branchId("b1")
                .poolServicePointId("poolSp")
                .poolUserId("poolUser")
                .parameterMap(new HashMap<>())
                .build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);
        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        service.visitCall("b1", "sp1", visit, "auto");

        assertEquals("poolSp", visit.getParameterMap().get("LastPoolServicePointId"));
        assertEquals("poolUser", visit.getParameterMap().get("LastPoolUserId"));
        assertNull(visit.getPoolServicePointId());
        assertNull(visit.getPoolUserId());
    }

    @Test
    void visitCallByIdDelegatesToCherryPickAndHandlesMissing() {
        Visit visit = Visit.builder().id("v1").branchId("b1").parameterMap(new HashMap<>()).build();
        HashMap<String, Visit> visits = new HashMap<>();
        visits.put("v1", visit);

        VisitService service = spy(new VisitService());
        service.branchService = mock(BranchService.class);
        service.eventService = mock(EventService.class);

        doReturn(visits).when(service).getAllVisits("b1");
        doReturn(Optional.of(visit)).when(service).visitCall("b1", "sp1", visit, "cherryPick");

        Optional<Visit> found = service.visitCall("b1", "sp1", "v1");
        assertTrue(found.isPresent());
        assertSame(visit, found.get());
        verify(service).visitCall("b1", "sp1", visit, "cherryPick");

        Optional<Visit> missing = service.visitCall("b1", "sp1", "missing");
        assertTrue(missing.isEmpty());
        verify(service, times(1)).visitCall("b1", "sp1", visit, "cherryPick");
    }
}
