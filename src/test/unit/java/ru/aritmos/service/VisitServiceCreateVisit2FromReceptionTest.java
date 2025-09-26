package ru.aritmos.service;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Entity;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Reception;
import ru.aritmos.model.Service;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.rules.SegmentationRule;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Юнит-тесты для {@link VisitService#createVisit2FromReception(String, String, ArrayList, HashMap,
 * Boolean, String, String)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceCreateVisit2FromReceptionTest {

    @DisplayName("Метод `createVisit2FromReception` с правилом сегментации создаёт визит и печатает талон")
    @Test
    void createsVisitWithSegmentationRuleAndPrintsTicket() {
        VisitService service = new VisitService();

        Branch branch = new Branch("b1", "Центральное отделение");
        branch.setPrefix("A");
        branch.setPath("/central");
        branch.setReception(
            Reception.builder()
                .branchId("b1")
                .printers(List.of(Entity.builder().id("printer-1").name("Стойка 1").build()))
                .build());

        Service primaryService = new Service("s1", "Консультация", 15, "q1");
        Service secondaryService = new Service("s2", "Оформление", 20, "q1");
        branch.getServices().put(primaryService.getId(), primaryService);
        branch.getServices().put(secondaryService.getId(), secondaryService);

        Queue queue = new Queue("q1", "Основная очередь", "A", 60);
        queue.setTicketCounter(24);
        branch.getQueues().put(queue.getId(), queue);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(branchService.incrementTicketCounter(branch.getId(), queue))
            .thenAnswer(invocation -> {
                queue.setTicketCounter(queue.getTicketCounter() + 1);
                return queue.getTicketCounter();
            });

        EventService eventService = mock(EventService.class);

        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        UserRepresentation user = new UserRepresentation();
        user.setId("staff-1");
        user.setUsername("i.petrov");
        when(keyCloackClient.getUserBySid("sid-1")).thenReturn(Optional.of(user));

        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch), eq("rule-1")))
            .thenAnswer(invocation -> Optional.of(queue));

        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.keyCloackClient = keyCloackClient;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "reception");
        parameters.put("priority", "vip");

        ArrayList<Service> requestedServices = new ArrayList<>(
            List.of(
                new Service(primaryService.getId(), primaryService.getName(), 15, queue.getId()),
                new Service(secondaryService.getId(), secondaryService.getName(), 20, queue.getId())));

        Visit visit =
            service.createVisit2FromReception(
                branch.getId(),
                "printer-1",
                requestedServices,
                parameters,
                true,
                "rule-1",
                "sid-1");

        assertNotNull(visit);
        assertEquals(branch.getId(), visit.getBranchId());
        assertEquals("WAITING", visit.getStatus());
        assertEquals(queue.getId(), visit.getQueueId());
        assertEquals("A025", visit.getTicket());
        assertSame(parameters, visit.getParameterMap());
        assertEquals(primaryService.getId(), visit.getCurrentService().getId());
        assertNotSame(primaryService, visit.getCurrentService());
        assertEquals(1, visit.getUnservedServices().size());
        assertEquals(secondaryService.getId(), visit.getUnservedServices().get(0).getId());
        assertNotSame(secondaryService, visit.getUnservedServices().get(0));

        verify(branchService).incrementTicketCounter(branch.getId(), queue);
        verify(keyCloackClient).getUserBySid("sid-1");

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2))
            .updateVisit(same(visit), eventCaptor.capture(), same(service));
        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE), eventCaptor.getAllValues());

        VisitEvent createdEvent = eventCaptor.getAllValues().get(0);
        assertEquals("false", createdEvent.getParameters().get("isVirtual"));
        assertEquals("reception", createdEvent.getParameters().get("visitCreator"));
        assertEquals("staff-1", createdEvent.getParameters().get("staffId"));
        assertEquals("i.petrov", createdEvent.getParameters().get("staffName"));
        assertEquals(primaryService.getId(), createdEvent.getParameters().get("serviceId"));
        assertEquals(primaryService.getName(), createdEvent.getParameters().get("serviceName"));
        assertEquals("printer-1", createdEvent.getParameters().get("printerId"));

        VisitEvent queueEvent = eventCaptor.getAllValues().get(1);
        assertEquals(primaryService.getId(), queueEvent.getParameters().get("serviceId"));
        assertEquals(primaryService.getName(), queueEvent.getParameters().get("serviceName"));
        assertEquals(queue.getId(), queueEvent.getParameters().get("queueId"));

        verify(printerService).print("printer-1", visit);
        verifyNoMoreInteractions(printerService);
    }

    @DisplayName("Метод `createVisit2FromReception` возвращает код 400, если правило сегментации не подобрало очередь")
    @Test
    void throwsBadRequestWhenSegmentationRuleReturnsEmptyQueue() {
        VisitService service = new VisitService();

        Branch branch = new Branch("b2", "Южное отделение");
        branch.setPrefix("B");
        branch.setReception(
            Reception.builder()
                .branchId("b2")
                .printers(List.of(Entity.builder().id("printer-2").name("Стойка 2").build()))
                .build());

        Service serviceModel = new Service("svc", "Окно", 10, "queue-2");
        branch.getServices().put(serviceModel.getId(), serviceModel);
        branch.getQueues().put("queue-2", new Queue("queue-2", "Очередь", "B", 40));

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);

        EventService eventService = mock(EventService.class);
        doNothing().when(eventService).send(anyString(), anyBoolean(), any());

        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.getUserBySid("sid-2")).thenReturn(Optional.empty());

        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch), eq("rule-2")))
            .thenReturn(Optional.empty());

        service.branchService = branchService;
        service.eventService = eventService;
        service.keyCloackClient = keyCloackClient;
        service.segmentationRule = segmentationRule;
        service.printerService = mock(PrinterService.class);

        ArrayList<Service> services = new ArrayList<>(
            List.of(new Service(serviceModel.getId(), serviceModel.getName(), 10, "queue-2")));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "reception");

        HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () ->
                service.createVisit2FromReception(
                    branch.getId(),
                    "printer-2",
                    services,
                    parameters,
                    false,
                    "rule-2",
                    "sid-2"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(eventService).send(anyString(), anyBoolean(), any());
        verify(branchService, never()).incrementTicketCounter(anyString(), any());
        verify(service.printerService, never()).print(anyString(), any());
    }

    @DisplayName("Метод `createVisit2FromReception` возвращает код 404 при отсутствии очереди в конфигурации отделения")
    @Test
    void throwsNotFoundWhenQueueMissingInBranchConfiguration() {
        VisitService service = new VisitService();

        Branch branch = new Branch("b3", "Северное отделение");
        branch.setPrefix("C");
        branch.setReception(
            Reception.builder()
                .branchId("b3")
                .printers(List.of(Entity.builder().id("printer-3").name("Стойка 3").build()))
                .build());

        Service serviceModel = new Service("svc-3", "Подача документов", 12, "queue-x");
        branch.getServices().put(serviceModel.getId(), serviceModel);
        // Очередь с таким идентификатором отсутствует в конфигурации отделения

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(branchService.incrementTicketCounter(eq(branch.getId()), any(Queue.class))).thenReturn(7);

        EventService eventService = mock(EventService.class);
        doNothing().when(eventService).send(anyString(), anyBoolean(), any());

        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.getUserBySid("sid-3")).thenReturn(Optional.empty());

        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        Queue externalQueue = new Queue("queue-x", "Внешняя очередь", "C", 50);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch), eq("rule-3")))
            .thenReturn(Optional.of(externalQueue));

        service.branchService = branchService;
        service.eventService = eventService;
        service.keyCloackClient = keyCloackClient;
        service.segmentationRule = segmentationRule;
        service.printerService = mock(PrinterService.class);

        ArrayList<Service> services = new ArrayList<>(
            List.of(new Service(serviceModel.getId(), serviceModel.getName(), 12, externalQueue.getId())));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "reception");

        HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () ->
                service.createVisit2FromReception(
                    branch.getId(),
                    "printer-3",
                    services,
                    parameters,
                    true,
                    "rule-3",
                    "sid-3"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(branchService).incrementTicketCounter(eq(branch.getId()), any(Queue.class));
        verify(eventService).send(anyString(), anyBoolean(), any());
        verify(service.printerService, never()).print(anyString(), any());
    }
}
