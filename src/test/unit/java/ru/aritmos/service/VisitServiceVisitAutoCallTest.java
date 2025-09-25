package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.rules.CallRule;

/**
 * Юнит-тесты для {@link VisitService#visitAutoCall(ru.aritmos.model.visit.Visit)}.
 */
class VisitServiceVisitAutoCallTest {

    @DisplayName("проверяется сценарий «auto call disables mode after successful call»")
    @Test
    void autoCallDisablesModeAfterSuccessfulCall() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        servicePoint.setAutoCallMode(true);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        CallRule waitingRule = mock(CallRule.class);
        when(waitingRule.getAvailiableServicePoints(branch, visit)).thenReturn(List.of(servicePoint));

        Visit calledVisit = Visit.builder().id("called").build();

        VisitService service = spy(new VisitService());
        service.branchService = branchService;
        service.eventService = mock(ru.aritmos.events.services.EventService.class);
        service.setWaitingTimeCallRule(waitingRule);
        doReturn(Optional.of(calledVisit))
                .when(service)
                .visitCall(eq("b1"), eq("sp1"), same(visit), eq("autoCall"));

        Visit result = service.visitAutoCall(visit);

        assertSame(calledVisit, result);
        assertFalse(servicePoint.getAutoCallMode());
        verify(waitingRule).getAvailiableServicePoints(same(branch), same(visit));
        verify(service, never()).visitCallForConfirmWithMaxWaitingTime(anyString(), anyString(), any(Visit.class));
    }

    @DisplayName("проверяется сценарий «auto call uses confirm flow when required»")
    @Test
    void autoCallUsesConfirmFlowWhenRequired() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        servicePoint.setAutoCallMode(true);
        servicePoint.setIsConfirmRequired(true);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        CallRule waitingRule = mock(CallRule.class);
        when(waitingRule.getAvailiableServicePoints(branch, visit)).thenReturn(List.of(servicePoint));

        Visit confirmVisit = Visit.builder().id("confirm").build();

        VisitService service = spy(new VisitService());
        service.branchService = branchService;
        service.eventService = mock(ru.aritmos.events.services.EventService.class);
        service.setWaitingTimeCallRule(waitingRule);
        doReturn(Optional.empty())
                .when(service)
                .visitCall(anyString(), anyString(), any(Visit.class), anyString());
        doReturn(Optional.of(confirmVisit))
                .when(service)
                .visitCallForConfirmWithMaxWaitingTime(eq("b1"), eq("sp1"), same(visit));

        Visit result = service.visitAutoCall(visit);

        assertSame(confirmVisit, result);
        assertFalse(servicePoint.getAutoCallMode());
        verify(service).visitCallForConfirmWithMaxWaitingTime(eq("b1"), eq("sp1"), same(visit));
    }

    @DisplayName("проверяется сценарий «auto call returns original when mode disabled»")
    @Test
    void autoCallReturnsOriginalWhenModeDisabled() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        servicePoint.setAutoCallMode(true);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        CallRule waitingRule = mock(CallRule.class);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(ru.aritmos.events.services.EventService.class);
        service.setWaitingTimeCallRule(waitingRule);

        Visit result = service.visitAutoCall(visit);

        assertSame(visit, result);
        verifyNoInteractions(waitingRule);
    }

    @DisplayName("проверяется сценарий «auto call returns original when no service point found»")
    @Test
    void autoCallReturnsOriginalWhenNoServicePointFound() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        servicePoint.setAutoCallMode(true);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        CallRule waitingRule = mock(CallRule.class);
        when(waitingRule.getAvailiableServicePoints(branch, visit)).thenReturn(List.of());

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(ru.aritmos.events.services.EventService.class);
        service.setWaitingTimeCallRule(waitingRule);

        Visit result = service.visitAutoCall(visit);

        assertSame(visit, result);
        assertTrue(servicePoint.getAutoCallMode());
        verify(waitingRule).getAvailiableServicePoints(same(branch), same(visit));
    }

    @DisplayName("проверяется сценарий «auto call keeps mode when call returns empty»")
    @Test
    void autoCallKeepsModeWhenCallReturnsEmpty() {
        Branch branch = new Branch("b1", "Branch");
        branch.getParameterMap().put("autoCallMode", "true");
        ServicePoint servicePoint = new ServicePoint("sp1", "SP1");
        servicePoint.setAutoCallMode(true);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        Visit visit = Visit.builder().id("v1").branchId("b1").build();

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        CallRule waitingRule = mock(CallRule.class);
        when(waitingRule.getAvailiableServicePoints(branch, visit)).thenReturn(List.of(servicePoint));

        VisitService service = spy(new VisitService());
        service.branchService = branchService;
        service.eventService = mock(ru.aritmos.events.services.EventService.class);
        service.setWaitingTimeCallRule(waitingRule);
        doReturn(Optional.empty())
                .when(service)
                .visitCall(eq("b1"), eq("sp1"), same(visit), eq("autoCall"));

        Visit result = service.visitAutoCall(visit);

        assertSame(visit, result);
        assertTrue(servicePoint.getAutoCallMode());
        verify(service, never()).visitCallForConfirmWithMaxWaitingTime(anyString(), anyString(), any(Visit.class));
    }
}