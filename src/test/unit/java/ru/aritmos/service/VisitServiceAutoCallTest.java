package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;

class VisitServiceAutoCallTest {

    @DisplayName("проверяется сценарий «branch auto call mode disables service points»")
    @Test
    void branchAutoCallModeDisablesServicePoints() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setAutoCallMode(true);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Optional<Branch> result = service.setAutoCallModeOfBranch("b1", false);

        assertTrue(result.isPresent());
        assertEquals("false", branch.getParameterMap().get("autoCallMode"));
        assertFalse(branch.getServicePoints().get("sp1").getAutoCallMode());
        verify(branchService).add("b1", branch);
    }

    @DisplayName("проверяется сценарий «service point auto call mode enables when branch mode on»")
    @Test
    void servicePointAutoCallModeEnablesWhenBranchModeOn() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Optional<ServicePoint> result = service.setAutoCallModeOfServicePoint("b1", "sp1", true);

        assertTrue(result.isPresent());
        assertTrue(branch.getServicePoints().get("sp1").getAutoCallMode());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("frontend"), eq(false), captor.capture());
        assertEquals("SERVICEPOINT_AUTOCALL_MODE_TURN_ON", captor.getValue().getEventType());
        verify(branchService).add("b1", branch);
    }

    @DisplayName("проверяется сценарий «service point auto call mode fails when branch mode off»")
    @Test
    void servicePointAutoCallModeFailsWhenBranchModeOff() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "false");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        assertThrows(HttpStatusException.class,
                () -> service.setAutoCallModeOfServicePoint("b1", "sp1", true));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @DisplayName("проверяется сценарий «service point auto call mode throws when service point missing»")
    @Test
    void servicePointAutoCallModeThrowsWhenServicePointMissing() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        assertThrows(HttpStatusException.class,
                () -> service.setAutoCallModeOfServicePoint("b1", "missing", true));
        verify(eventService).send(eq("*"), eq(false), any());
    }

    @DisplayName("проверяется сценарий «service point auto call mode returns existing when disabling with branch mode off»")
    @Test
    void servicePointAutoCallModeReturnsExistingWhenDisablingWithBranchModeOff() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "false");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Optional<ServicePoint> result = service.setAutoCallModeOfServicePoint("b1", "sp1", false);

        assertTrue(result.isPresent());
        assertSame(servicePoint, result.get());
        assertFalse(branch.getServicePoints().get("sp1").getAutoCallMode());
        verify(branchService, never()).add(anyString(), any());
        verifyNoInteractions(eventService);
    }

    @DisplayName("проверяется сценарий «cancel auto call mode disables point and emits event»")
    @Test
    void cancelAutoCallModeDisablesPointAndEmitsEvent() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        servicePoint.setAutoCallMode(true);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        EventService eventService = mock(EventService.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;

        Optional<ServicePoint> result = service.cancelAutoCallModeOfServicePoint("b1", "sp1");

        assertTrue(result.isPresent());
        assertFalse(branch.getServicePoints().get("sp1").getAutoCallMode());
        verify(branchService).add("b1", branch);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("frontend"), eq(false), captor.capture());
        Event published = captor.getValue();
        assertEquals("SERVICEPOINT_AUTOCALL_MODE_TURN_ON", published.getEventType());
        assertEquals("b1", published.getParams().get("branchId"));
        assertEquals("sp1", published.getParams().get("servicePointId"));
        assertSame(servicePoint, published.getBody());
    }
}
