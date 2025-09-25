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
 * Boolean, String)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceCreateVisit2FromReceptionLegacyTest {

    @DisplayName("Creates Visit And Prints Ticket When Segmentation Returns Queue")
    @Test
    void createsVisitAndPrintsTicketWhenSegmentationReturnsQueue() throws Exception {
        VisitService service = new VisitService();

        Branch branch = new Branch("b-main", "Центральное отделение");
        branch.setPrefix("A");
        branch.setPath("/central");
        branch.setReception(
            Reception.builder()
                .branchId("b-main")
                .printers(List.of(Entity.builder().id("printer-1").name("Стойка 1").build()))
                .build());

        Service primary = new Service("service-1", "Регистрация", 10, "queue-1");
        Service secondary = new Service("service-2", "Консультация", 15, "queue-1");
        branch.getServices().put(primary.getId(), primary);
        branch.getServices().put(secondary.getId(), secondary);

        Queue queue = new Queue("queue-1", "Основная очередь", "A", 60);
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
        PrinterService printerService = mock(PrinterService.class);

        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        UserRepresentation staff = new UserRepresentation();
        staff.setId("staff-1");
        staff.setUsername("i.petrov");
        when(keyCloackClient.getUserBySid("sid-1")).thenReturn(Optional.of(staff));

        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch)))
            .thenAnswer(invocation -> Optional.of(queue));

        service.branchService = branchService;
        service.eventService = eventService;
        service.keyCloackClient = keyCloackClient;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> requested = new ArrayList<>(
            List.of(
                new Service(primary.getId(), primary.getName(), primary.getServingSL(), queue.getId()),
                new Service(secondary.getId(), secondary.getName(), secondary.getServingSL(), queue.getId())));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "reception");
        parameters.put("priority", "vip");

        Visit visit =
            service.createVisit2FromReception(
                branch.getId(),
                "printer-1",
                requested,
                parameters,
                true,
                "sid-1");

        assertNotNull(visit);
        assertEquals(branch.getId(), visit.getBranchId());
        assertEquals("WAITING", visit.getStatus());
        assertEquals(queue.getId(), visit.getQueueId());
        assertEquals("A025", visit.getTicket());
        assertSame(parameters, visit.getParameterMap());
        assertEquals(primary.getId(), visit.getCurrentService().getId());
        assertNotSame(primary, visit.getCurrentService());
        assertEquals(1, visit.getUnservedServices().size());
        assertEquals(secondary.getId(), visit.getUnservedServices().get(0).getId());
        assertNotSame(secondary, visit.getUnservedServices().get(0));

        verify(branchService).incrementTicketCounter(branch.getId(), queue);
        verify(keyCloackClient).getUserBySid("sid-1");

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(service));
        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE), eventCaptor.getAllValues());

        VisitEvent createdEvent = eventCaptor.getAllValues().get(0);
        assertEquals("false", createdEvent.getParameters().get("isVirtual"));
        assertEquals("reception", createdEvent.getParameters().get("visitCreator"));
        assertEquals("staff-1", createdEvent.getParameters().get("staffId"));
        assertEquals("i.petrov", createdEvent.getParameters().get("staffName"));
        assertEquals(primary.getId(), createdEvent.getParameters().get("serviceId"));
        assertEquals(primary.getName(), createdEvent.getParameters().get("serviceName"));
        assertEquals("printer-1", createdEvent.getParameters().get("printerId"));

        VisitEvent queueEvent = eventCaptor.getAllValues().get(1);
        assertEquals(primary.getId(), queueEvent.getParameters().get("serviceId"));
        assertEquals(primary.getName(), queueEvent.getParameters().get("serviceName"));
        assertEquals(queue.getId(), queueEvent.getParameters().get("queueId"));

        verify(printerService).print("printer-1", visit);
        verifyNoMoreInteractions(printerService);
    }

    @DisplayName("Does Not Print Ticket When Flag Disabled And Staff Missing")
    @Test
    void doesNotPrintTicketWhenFlagDisabledAndStaffMissing() throws Exception {
        VisitService service = new VisitService();

        Branch branch = new Branch("b-secondary", "Северное отделение");
        branch.setPrefix("B");
        branch.setReception(
            Reception.builder()
                .branchId("b-secondary")
                .printers(List.of(Entity.builder().id("printer-2").name("Стойка 2").build()))
                .build());

        Service serviceModel = new Service("svc", "Оформление", 12, "queue-2");
        branch.getServices().put(serviceModel.getId(), serviceModel);

        Queue queue = new Queue("queue-2", "Очередь", "B", 40);
        queue.setTicketCounter(99);
        branch.getQueues().put(queue.getId(), queue);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(branchService.incrementTicketCounter(branch.getId(), queue))
            .thenAnswer(invocation -> {
                queue.setTicketCounter(queue.getTicketCounter() + 1);
                return queue.getTicketCounter();
            });

        EventService eventService = mock(EventService.class);
        PrinterService printerService = mock(PrinterService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.getUserBySid("sid-absent")).thenReturn(Optional.empty());

        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch)))
            .thenReturn(Optional.of(queue));

        service.branchService = branchService;
        service.eventService = eventService;
        service.keyCloackClient = keyCloackClient;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services = new ArrayList<>(
            List.of(new Service(serviceModel.getId(), serviceModel.getName(), 12, queue.getId())));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "reception");

        Visit visit =
            service.createVisit2FromReception(
                branch.getId(),
                "printer-2",
                services,
                parameters,
                false,
                "sid-absent");

        assertNotNull(visit);
        assertEquals("WAITING", visit.getStatus());
        assertEquals(queue.getId(), visit.getQueueId());
        assertEquals("B100", visit.getTicket());
        assertSame(parameters, visit.getParameterMap());

        verify(keyCloackClient).getUserBySid("sid-absent");

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(service));
        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE), eventCaptor.getAllValues());

        VisitEvent createdEvent = eventCaptor.getAllValues().get(0);
        assertEquals("true", createdEvent.getParameters().get("isVirtual"));
        assertEquals("", createdEvent.getParameters().get("staffId"));
        assertEquals("", createdEvent.getParameters().get("staffName"));

        VisitEvent queueEvent = eventCaptor.getAllValues().get(1);
        assertEquals(serviceModel.getId(), queueEvent.getParameters().get("serviceId"));
        assertEquals(serviceModel.getName(), queueEvent.getParameters().get("serviceName"));
        assertEquals(queue.getId(), queueEvent.getParameters().get("queueId"));

        verify(printerService, never()).print(anyString(), any());
    }

    @DisplayName("Throws Bad Request When Segmentation Returns Empty Queue")
    @Test
    void throwsBadRequestWhenSegmentationReturnsEmptyQueue() throws Exception {
        VisitService service = new VisitService();

        Branch branch = new Branch("b-error", "Южное отделение");
        branch.setPrefix("C");
        branch.setReception(
            Reception.builder()
                .branchId("b-error")
                .printers(List.of(Entity.builder().id("printer-3").name("Стойка 3").build()))
                .build());

        Service serviceModel = new Service("svc-error", "Документы", 20, "queue-3");
        branch.getServices().put(serviceModel.getId(), serviceModel);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);

        EventService eventService = mock(EventService.class);
        PrinterService printerService = mock(PrinterService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.getUserBySid("sid-error")).thenReturn(Optional.empty());

        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch))).thenReturn(Optional.empty());

        service.branchService = branchService;
        service.eventService = eventService;
        service.keyCloackClient = keyCloackClient;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services = new ArrayList<>(
            List.of(new Service(serviceModel.getId(), serviceModel.getName(), 20, "queue-3")));

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
                    "sid-error"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(eventService).send(anyString(), anyBoolean(), any());
        verify(branchService, never()).incrementTicketCounter(anyString(), any());
        verify(printerService, never()).print(anyString(), any());
    }

    @DisplayName("Throws Not Found When Queue Missing In Branch Configuration")
    @Test
    void throwsNotFoundWhenQueueMissingInBranchConfiguration() throws Exception {
        VisitService service = new VisitService();

        Branch branch = new Branch("b-missing", "Западное отделение");
        branch.setPrefix("D");
        branch.setReception(
            Reception.builder()
                .branchId("b-missing")
                .printers(List.of(Entity.builder().id("printer-4").name("Стойка 4").build()))
                .build());

        Service serviceModel = new Service("svc-missing", "Подача документов", 18, "queue-missing");
        branch.getServices().put(serviceModel.getId(), serviceModel);

        Queue returnedQueue = new Queue("queue-external", "Внешняя очередь", "D", 50);
        returnedQueue.setTicketCounter(5);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(branchService.incrementTicketCounter(branch.getId(), returnedQueue))
            .thenAnswer(invocation -> {
                returnedQueue.setTicketCounter(returnedQueue.getTicketCounter() + 1);
                return returnedQueue.getTicketCounter();
            });

        EventService eventService = mock(EventService.class);
        PrinterService printerService = mock(PrinterService.class);
        KeyCloackClient keyCloackClient = mock(KeyCloackClient.class);
        when(keyCloackClient.getUserBySid("sid-missing")).thenReturn(Optional.empty());

        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch)))
            .thenReturn(Optional.of(returnedQueue));

        service.branchService = branchService;
        service.eventService = eventService;
        service.keyCloackClient = keyCloackClient;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services = new ArrayList<>(
            List.of(new Service(serviceModel.getId(), serviceModel.getName(), 18, returnedQueue.getId())));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "reception");

        HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () ->
                service.createVisit2FromReception(
                    branch.getId(),
                    "printer-4",
                    services,
                    parameters,
                    true,
                    "sid-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService).updateVisit(any(Visit.class), eventCaptor.capture(), same(service));
        assertEquals(VisitEvent.CREATED, eventCaptor.getValue());
        verify(branchService).incrementTicketCounter(branch.getId(), returnedQueue);
        verify(printerService, never()).print(anyString(), any());
    }
}
