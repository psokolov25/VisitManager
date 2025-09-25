package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.DelayedEvents;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.service.rules.CallRule;
import ru.aritmos.service.rules.SegmentationRule;

class VisitServiceAutoCallModeTest {

    private VisitService visitService;
    private BranchService branchService;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        visitService = new VisitService();
        branchService = mock(BranchService.class);
        eventService = mock(EventService.class);
        visitService.branchService = branchService;
        visitService.eventService = eventService;
        visitService.delayedEvents = mock(DelayedEvents.class);
        visitService.printerService = mock(PrinterService.class);
        visitService.keyCloackClient = mock(KeyCloackClient.class);
        visitService.segmentationRule = mock(SegmentationRule.class);
        visitService.setWaitingTimeCallRule(mock(CallRule.class));
        visitService.setLifeTimeCallRule(mock(CallRule.class));
    }

    @DisplayName("Start Auto Call Mode Of Service Point Turns On Mode When Branch Allows")
    @Test
    void startAutoCallModeOfServicePointTurnsOnModeWhenBranchAllows() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");
        ServicePoint servicePoint = new ServicePoint("sp1", "Service point");
        branch.getServicePoints().put("sp1", servicePoint);
        when(branchService.getBranch("b1")).thenReturn(branch);

        Optional<ServicePoint> actual = visitService.startAutoCallModeOfServicePoint("b1", "sp1");

        assertTrue(actual.isPresent());
        assertSame(servicePoint, actual.get());
        assertTrue(Boolean.TRUE.equals(servicePoint.getAutoCallMode()));
        verify(branchService).add(eq("b1"), same(branch));
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("frontend"), eq(false), captor.capture());
        Event event = captor.getValue();
        assertEquals("SERVICEPOINT_AUTOCALL_MODE_TURN_ON", event.getEventType());
        assertEquals("b1", event.getParams().get("branchId"));
        assertEquals("sp1", event.getParams().get("servicePointId"));
        assertSame(servicePoint, event.getBody());
    }

    @DisplayName("Start Auto Call Mode Of Service Point Throws Conflict When Auto Call Disabled")
    @Test
    void startAutoCallModeOfServicePointThrowsConflictWhenAutoCallDisabled() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "false");
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "Service point"));
        when(branchService.getBranch("b1")).thenReturn(branch);

        HttpStatusException exception =
            assertThrows(HttpStatusException.class,
                () -> visitService.startAutoCallModeOfServicePoint("b1", "sp1"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(eventService, never()).send(eq("frontend"), anyBoolean(), any(Event.class));
        verify(branchService, never()).add(anyString(), any(Branch.class));
    }

    @DisplayName("Start Auto Call Mode Of Service Point Throws Not Found When Service Point Missing")
    @Test
    void startAutoCallModeOfServicePointThrowsNotFoundWhenServicePointMissing() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");
        when(branchService.getBranch("b1")).thenReturn(branch);

        HttpStatusException exception =
            assertThrows(HttpStatusException.class,
                () -> visitService.startAutoCallModeOfServicePoint("b1", "sp1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any(Event.class));
        verify(branchService, never()).add(anyString(), any(Branch.class));
    }
}

