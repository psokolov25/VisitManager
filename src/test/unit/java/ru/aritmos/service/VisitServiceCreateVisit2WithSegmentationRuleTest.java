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
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.EntryPoint;
import ru.aritmos.model.Entity;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.rules.SegmentationRule;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты для {@link VisitService#createVisit2(String, String, ArrayList, HashMap, Boolean, String)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceCreateVisit2WithSegmentationRuleTest {

    @DisplayName("Создание визита второй версией с правилом сегментации печатает талон при найденной очереди")
    @Test
    void createsVisitAndPrintsTicketWhenSegmentationRuleProvidesQueue() throws Exception {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-main", "Главное отделение");
        branch.setPrefix("G");
        branch.setPath("/main");

        Service primary = new Service("service-primary", "Регистрация", 10, "queue-main");
        Service secondary = new Service("service-secondary", "Консультация", 15, "queue-main");
        branch.getServices().put(primary.getId(), primary);
        branch.getServices().put(secondary.getId(), secondary);

        EntryPoint entryPoint = new EntryPoint();
        entryPoint.setId("entry-1");
        entryPoint.setName("Терминал у входа");
        entryPoint.setPrinter(Entity.builder().id("printer-entrance").name("Принтер у входа").build());
        branch.getEntryPoints().put(entryPoint.getId(), entryPoint);

        Queue queue = new Queue("queue-main", "Основная очередь", "G", 60);
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
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch), eq("rule-42")))
            .thenAnswer(invocation -> Optional.of(queue));

        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> requestedServices = new ArrayList<>(
            List.of(
                new Service(
                    primary.getId(),
                    primary.getName(),
                    primary.getServingSL(),
                    primary.getLinkedQueueId()),
                new Service(
                    secondary.getId(),
                    secondary.getName(),
                    secondary.getServingSL(),
                    secondary.getLinkedQueueId())));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "terminal");
        parameters.put("priority", "standard");

        Visit visit =
            service.createVisit2(
                branch.getId(),
                entryPoint.getId(),
                requestedServices,
                parameters,
                true,
                "rule-42");

        assertNotNull(visit);
        assertEquals(branch.getId(), visit.getBranchId());
        assertEquals("WAITING", visit.getStatus());
        assertEquals(queue.getId(), visit.getQueueId());
        assertEquals("G025", visit.getTicket());
        assertSame(parameters, visit.getParameterMap());
        assertEquals(primary.getId(), visit.getCurrentService().getId());
        assertNotSame(primary, visit.getCurrentService());
        assertEquals(1, visit.getUnservedServices().size());
        assertEquals(secondary.getId(), visit.getUnservedServices().get(0).getId());
        assertNotSame(secondary, visit.getUnservedServices().get(0));

        ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
        verify(segmentationRule).getQueue(visitCaptor.capture(), eq(branch), eq("rule-42"));
        assertSame(visit, visitCaptor.getValue());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(same(visit), eventCaptor.capture(), same(service));
        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE), eventCaptor.getAllValues());

        VisitEvent createdEvent = eventCaptor.getAllValues().get(0);
        assertEquals("terminal", createdEvent.getParameters().get("visitCreator"));
        assertEquals(primary.getId(), createdEvent.getParameters().get("serviceId"));
        assertEquals(primary.getName(), createdEvent.getParameters().get("serviceName"));
        assertEquals(entryPoint.getPrinter().getId(), createdEvent.getParameters().get("printerId"));

        VisitEvent queueEvent = eventCaptor.getAllValues().get(1);
        assertEquals(primary.getId(), queueEvent.getParameters().get("serviceId"));
        assertEquals(primary.getName(), queueEvent.getParameters().get("serviceName"));
        assertEquals(queue.getId(), queueEvent.getParameters().get("queueId"));

        verify(branchService).incrementTicketCounter(branch.getId(), queue);
        verify(printerService).print(entryPoint.getPrinter().getId(), visit);
        verifyNoMoreInteractions(printerService);
        verifyNoInteractions(eventService);
    }

    @DisplayName("Создание визита второй версией с правилом сегментации возвращает 400, если правило не подобрало очередь")
    @Test
    void throwsBadRequestWhenSegmentationRuleReturnsEmptyQueue() {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-error", "Отделение без очереди");
        branch.setPrefix("E");

        Service serviceModel = new Service("svc", "Документы", 12, "queue-unknown");
        branch.getServices().put(serviceModel.getId(), serviceModel);

        EntryPoint entryPoint = new EntryPoint();
        entryPoint.setId("entry-empty");
        entryPoint.setName("Терминал без очереди");
        entryPoint.setPrinter(Entity.builder().id("printer-empty").name("Принтер").build());
        branch.getEntryPoints().put(entryPoint.getId(), entryPoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);

        EventService eventService = mock(EventService.class);
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch), eq("rule-empty")))
            .thenReturn(Optional.empty());

        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services = new ArrayList<>(
            List.of(new Service(serviceModel.getId(), serviceModel.getName(), 12, serviceModel.getLinkedQueueId())));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "terminal");

        HttpStatusException exception =
            assertThrows(
                HttpStatusException.class,
                () ->
                    service.createVisit2(
                        branch.getId(),
                        entryPoint.getId(),
                        services,
                        parameters,
                        true,
                        "rule-empty"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verify(segmentationRule).getQueue(any(Visit.class), eq(branch), eq("rule-empty"));
        verify(eventService).send(anyString(), anyBoolean(), any());
        verify(branchService, never()).incrementTicketCounter(anyString(), any());
        verify(printerService, never()).print(anyString(), any());
    }

    @DisplayName("Создание визита второй версией с правилом сегментации возвращает 404 при отсутствии терминала в отделении")
    @Test
    void throwsNotFoundWhenEntryPointMissingInBranch() {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-missing", "Отделение без терминала");
        branch.setPrefix("M");

        Service serviceModel = new Service("svc-m", "Консультация", 8, "queue-m");
        branch.getServices().put(serviceModel.getId(), serviceModel);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);

        EventService eventService = mock(EventService.class);
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services = new ArrayList<>(
            List.of(new Service(serviceModel.getId(), serviceModel.getName(), 8, serviceModel.getLinkedQueueId())));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "terminal");

        HttpStatusException exception =
            assertThrows(
                HttpStatusException.class,
                () ->
                    service.createVisit2(
                        branch.getId(),
                        "absent-entry",
                        services,
                        parameters,
                        true,
                        "rule-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(eventService).send(anyString(), anyBoolean(), any());
        verify(segmentationRule, never()).getQueue(any(), any(), any());
        verify(branchService, never()).incrementTicketCounter(anyString(), any());
        verify(printerService, never()).print(anyString(), any());
    }
}
