package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.VisitParameters;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Юнит-тесты для методов {@link VisitService#createVisitFromReception}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceCreateVisitFromReceptionTest {

    @Test
    void createVisitFromReceptionDelegatesToCreateVisit2FromReception() throws SystemException {
        Branch branch = new Branch("b1", "Отделение");
        Service first = new Service("s1", "Услуга 1", 30, "q1");
        Service second = new Service("s2", "Услуга 2", 30, "q2");
        branch.getServices().put(first.getId(), first);
        branch.getServices().put(second.getId(), second);

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        service.eventService = mock(EventService.class);
        service.visit2ReceptionResult = Visit.builder().id("reception").build();

        VisitParameters parameters = VisitParameters.builder()
                .serviceIds(new ArrayList<>(List.of(first.getId(), second.getId())))
                .parameters(new HashMap<>(Map.of("channel", "reception")))
                .build();

        Visit result = service.createVisitFromReception("b1", "printer-1", parameters, true, "sid-1");

        assertSame(service.visit2ReceptionResult, result);
        assertTrue(service.visitAutoCallInvoked);
        assertSame(service.visit2ReceptionResult, service.autoCallArgument);
        assertEquals("b1", service.lastBranchId);
        assertEquals("printer-1", service.lastPrinterId);
        assertEquals(Boolean.TRUE, service.lastPrintTicket);
        assertEquals("sid-1", service.lastSid);
        assertSame(parameters.getParameters(), service.lastParameters);
        assertEquals(List.of("s1", "s2"), service.lastServices.stream().map(Service::getId).toList());
        assertNotSame(first, service.lastServices.get(0));
        assertNotSame(second, service.lastServices.get(1));
    }

    @Test
    void createVisitFromReceptionThrowsWhenServiceMissing() {
        Branch branch = new Branch("b1", "Отделение");

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        VisitParameters parameters = VisitParameters.builder()
                .serviceIds(new ArrayList<>(List.of("absent")))
                .parameters(new HashMap<>(Map.of("channel", "reception")))
                .build();

        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.createVisitFromReception("b1", "printer-1", parameters, false, "sid-2"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any());
        assertFalse(service.visitAutoCallInvoked);
    }

    @Test
    void createVisitFromReceptionThrowsWhenServiceListEmpty() {
        Branch branch = new Branch("b1", "Отделение");

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        VisitParameters parameters = VisitParameters.builder().build();

        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.createVisitFromReception("b1", "printer-1", parameters, true, "sid-3"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any());
        assertFalse(service.visitAutoCallInvoked);
    }

    @Test
    void createVisitFromReceptionWithSegmentationRuleDelegates() {
        Branch branch = new Branch("b1", "Отделение");
        Service only = new Service("s1", "Услуга", 30, "q1");
        branch.getServices().put(only.getId(), only);

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        service.eventService = mock(EventService.class);
        service.visit2ReceptionResult = Visit.builder().id("segmented").build();

        VisitParameters parameters = VisitParameters.builder()
                .serviceIds(new ArrayList<>(List.of(only.getId())))
                .parameters(new HashMap<>(Map.of("channel", "reception")))
                .build();

        Visit result = service.createVisitFromReception("b1", "printer-1", parameters, false, "rule-9", "sid-9");

        assertSame(service.visit2ReceptionResult, result);
        assertEquals("rule-9", service.lastSegmentationRuleId);
        assertEquals("sid-9", service.lastSid);
        assertEquals(List.of("s1"), service.lastServices.stream().map(Service::getId).toList());
        assertNotSame(only, service.lastServices.get(0));
        assertTrue(service.visitAutoCallInvoked);
    }

    @Test
    void createVisitFromReceptionWithSegmentationRuleThrowsWhenServiceMissing() {
        Branch branch = new Branch("b1", "Отделение");

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        VisitParameters parameters = VisitParameters.builder()
                .serviceIds(new ArrayList<>(List.of("missing")))
                .parameters(new HashMap<>(Map.of("channel", "reception")))
                .build();

        HttpStatusException exception = assertThrows(
                HttpStatusException.class,
                () -> service.createVisitFromReception("b1", "printer-1", parameters, false, "rule-9", "sid-5"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any());
        assertFalse(service.visitAutoCallInvoked);
    }

    private BranchService prepareBranchService(Branch branch) {
        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);
        return branchService;
    }

    private static class TestVisitService extends VisitService {
        List<Service> lastServices;
        HashMap<String, String> lastParameters;
        String lastBranchId;
        String lastPrinterId;
        Boolean lastPrintTicket;
        String lastSegmentationRuleId;
        String lastSid;
        Visit visit2ReceptionResult = Visit.builder().id("default").build();
        boolean visitAutoCallInvoked;
        Visit autoCallArgument;

        @Override
        public Visit createVisit2FromReception(
                String branchId,
                String printerId,
                ArrayList<Service> services,
                HashMap<String, String> parametersMap,
                Boolean printTicket,
                String sid) {
            recordReceptionCall(branchId, printerId, services, parametersMap, printTicket, null, sid);
            return visit2ReceptionResult;
        }

        @Override
        public Visit createVisit2FromReception(
                String branchId,
                String printerId,
                ArrayList<Service> services,
                HashMap<String, String> parametersMap,
                Boolean printTicket,
                String segmentationRuleId,
                String sid) {
            recordReceptionCall(branchId, printerId, services, parametersMap, printTicket, segmentationRuleId, sid);
            return visit2ReceptionResult;
        }

        @Override
        public Visit visitAutoCall(Visit visit) {
            visitAutoCallInvoked = true;
            autoCallArgument = visit;
            return visit;
        }

        private void recordReceptionCall(
                String branchId,
                String printerId,
                ArrayList<Service> services,
                HashMap<String, String> parametersMap,
                Boolean printTicket,
                String segmentationRuleId,
                String sid) {
            lastBranchId = branchId;
            lastPrinterId = printerId;
            lastServices = new ArrayList<>(services);
            lastParameters = parametersMap;
            lastPrintTicket = printTicket;
            lastSegmentationRuleId = segmentationRuleId;
            lastSid = sid;
        }
    }
}
