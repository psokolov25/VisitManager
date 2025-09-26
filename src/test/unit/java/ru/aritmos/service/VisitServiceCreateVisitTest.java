package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

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
 * Юнит-тесты для публичных обёрток {@link VisitService#createVisit}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceCreateVisitTest {

    @DisplayName("Базовое создание визита переиспользует расширенный сценарий с клонированными услугами")
    @Test
    void createVisitDelegatesToCreateVisit2WithClonedServices() throws SystemException {
        Branch branch = new Branch("b1", "Отделение");
        Service first = new Service("s1", "Услуга 1", 30, "q1");
        Service second = new Service("s2", "Услуга 2", 30, "q2");
        branch.getServices().put(first.getId(), first);
        branch.getServices().put(second.getId(), second);

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        service.eventService = mock(EventService.class);
        service.visit2Result = Visit.builder().id("result").build();

        VisitParameters parameters = VisitParameters.builder()
                .serviceIds(new ArrayList<>(List.of(first.getId(), second.getId())))
                .parameters(new HashMap<>(Map.of("source", "terminal")))
                .build();

        Visit result = service.createVisit("b1", "entry", parameters, true);

        assertSame(service.visit2Result, result);
        assertTrue(service.visitAutoCallInvoked);
        assertSame(service.visit2Result, service.autoCallArgument);
        assertEquals("b1", service.lastBranchId);
        assertEquals("entry", service.lastEntryPointId);
        assertEquals(Boolean.TRUE, service.lastPrintTicket);
        assertSame(parameters.getParameters(), service.lastParameters);
        assertEquals(List.of("s1", "s2"), service.lastServices.stream().map(Service::getId).toList());
        assertNotSame(first, service.lastServices.get(0));
        assertNotSame(second, service.lastServices.get(1));
    }

    @DisplayName("Базовое создание визита завершается ошибкой при отсутствии услуги")
    @Test
    void createVisitThrowsWhenServiceMissing() {
        Branch branch = new Branch("b1", "Отделение");

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        VisitParameters parameters = VisitParameters.builder()
                .serviceIds(new ArrayList<>(List.of("absent")))
                .parameters(new HashMap<>(Map.of("source", "terminal")))
                .build();

        HttpStatusException exception = assertThrows(
                HttpStatusException.class, () -> service.createVisit("b1", "entry", parameters, false));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any());
        assertFalse(service.visitAutoCallInvoked);
    }

    @DisplayName("Базовое создание визита завершается ошибкой при пустом списке услуг")
    @Test
    void createVisitThrowsWhenServiceListEmpty() {
        Branch branch = new Branch("b1", "Отделение");

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        VisitParameters parameters = VisitParameters.builder().build();

        HttpStatusException exception = assertThrows(
                HttpStatusException.class, () -> service.createVisit("b1", "entry", parameters, false));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(eventService).send(eq("*"), eq(false), any());
        assertFalse(service.visitAutoCallInvoked);
    }

    @DisplayName("Базовое создание визита с правилом сегментации переиспользует расширенный сценарий")
    @Test
    void createVisitWithSegmentationRuleDelegatesToCreateVisit2() {
        Branch branch = new Branch("b1", "Отделение");
        Service service1 = new Service("s1", "Услуга 1", 30, "q1");
        branch.getServices().put(service1.getId(), service1);

        TestVisitService visitService = new TestVisitService();
        visitService.branchService = prepareBranchService(branch);
        visitService.eventService = mock(EventService.class);
        visitService.visit2Result = Visit.builder().id("segmented").build();

        VisitParameters parameters = VisitParameters.builder()
                .serviceIds(new ArrayList<>(List.of(service1.getId())))
                .parameters(new HashMap<>(Map.of("channel", "app")))
                .build();

        Visit result = visitService.createVisit("b1", "entry", parameters, false, "rule-1");

        assertSame(visitService.visit2Result, result);
        assertEquals("rule-1", visitService.lastSegmentationRuleId);
        assertEquals(List.of("s1"), visitService.lastServices.stream().map(Service::getId).toList());
        assertNotSame(service1, visitService.lastServices.get(0));
        assertTrue(visitService.visitAutoCallInvoked);
    }

    @DisplayName("Базовое создание визита с правилом сегментации завершается ошибкой при отсутствии услуги")
    @Test
    void createVisitWithSegmentationRuleThrowsWhenServiceMissing() {
        Branch branch = new Branch("b1", "Отделение");

        TestVisitService service = new TestVisitService();
        service.branchService = prepareBranchService(branch);
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        VisitParameters parameters = VisitParameters.builder()
                .serviceIds(new ArrayList<>(List.of("missing")))
                .parameters(new HashMap<>(Map.of("channel", "app")))
                .build();

        HttpStatusException exception = assertThrows(
                HttpStatusException.class, () -> service.createVisit("b1", "entry", parameters, true, "rule-1"));

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
        String lastEntryPointId;
        Boolean lastPrintTicket;
        String lastSegmentationRuleId;
        Visit visit2Result = Visit.builder().id("default").build();
        boolean visitAutoCallInvoked;
        Visit autoCallArgument;

        @Override
        public Visit createVisit2(
                String branchId,
                String entryPointId,
                ArrayList<Service> services,
                HashMap<String, String> parametersMap,
                Boolean printTicket) {
            recordCreateVisitCall(branchId, entryPointId, services, parametersMap, printTicket, null);
            return visit2Result;
        }

        @Override
        public Visit createVisit2(
                String branchId,
                String entryPointId,
                ArrayList<Service> services,
                HashMap<String, String> parametersMap,
                Boolean printTicket,
                String segmentationRuleId) {
            recordCreateVisitCall(branchId, entryPointId, services, parametersMap, printTicket, segmentationRuleId);
            return visit2Result;
        }

        @Override
        public Visit visitAutoCall(Visit visit) {
            visitAutoCallInvoked = true;
            autoCallArgument = visit;
            return visit;
        }

        private void recordCreateVisitCall(
                String branchId,
                String entryPointId,
                ArrayList<Service> services,
                HashMap<String, String> parametersMap,
                Boolean printTicket,
                String segmentationRuleId) {
            lastBranchId = branchId;
            lastEntryPointId = entryPointId;
            lastServices = new ArrayList<>(services);
            lastParameters = parametersMap;
            lastPrintTicket = printTicket;
            lastSegmentationRuleId = segmentationRuleId;
        }
    }
}
