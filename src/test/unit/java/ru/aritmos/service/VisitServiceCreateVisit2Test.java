package ru.aritmos.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
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
import ru.aritmos.exceptions.SystemException;
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
 * Юнит-тесты для {@link VisitService#createVisit2(String, String, ArrayList, HashMap, Boolean)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceCreateVisit2Test {

    @DisplayName("Создание визита второй версией печатает талон при найденной очереди")
    @Test
    void createsVisitAndPrintsTicketWhenQueueFound() throws SystemException {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-terminal", "Терминал самообслуживания");
        branch.setPrefix("T");
        branch.setPath("/terminal");

        Service mainService = new Service("service-main", "Основная услуга", 5, "queue-main");
        Service extraService = new Service("service-extra", "Дополнительная услуга", 7, "queue-main");
        branch.getServices().put(mainService.getId(), mainService);
        branch.getServices().put(extraService.getId(), extraService);

        EntryPoint entryPoint = new EntryPoint();
        entryPoint.setId("entry-terminal");
        entryPoint.setName("Терминал у входа");
        entryPoint.setPrinter(Entity.builder().id("printer-01").name("Принтер терминала").build());
        branch.getEntryPoints().put(entryPoint.getId(), entryPoint);

        Queue queue = new Queue("queue-main", "Основная очередь", "T", 60);
        queue.setTicketCounter(17);
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
        when(segmentationRule.getQueue(any(Visit.class), eq(branch)))
            .thenAnswer(invocation -> Optional.of(queue));

        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> requestedServices = new ArrayList<>(
            List.of(
                new Service(
                    mainService.getId(),
                    mainService.getName(),
                    mainService.getServingSL(),
                    mainService.getLinkedQueueId()),
                new Service(
                    extraService.getId(),
                    extraService.getName(),
                    extraService.getServingSL(),
                    extraService.getLinkedQueueId())));

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "terminal");
        parameters.put("priority", "standard");

        Visit visit =
            service.createVisit2(
                branch.getId(), entryPoint.getId(), requestedServices, parameters, true);

        assertNotNull(visit);
        assertEquals("WAITING", visit.getStatus());
        assertEquals(branch.getId(), visit.getBranchId());
        assertEquals(branch.getName(), visit.getBranchName());
        assertEquals(branch.getPrefix(), visit.getBranchPrefix());
        assertEquals(branch.getPath(), visit.getBranchPath());
        assertEquals(queue.getId(), visit.getQueueId());
        assertEquals("T018", visit.getTicket());
        assertSame(parameters, visit.getParameterMap());
        assertEquals(mainService.getId(), visit.getCurrentService().getId());
        assertNotSame(mainService, visit.getCurrentService());
        assertEquals(1, visit.getUnservedServices().size());
        assertEquals(extraService.getId(), visit.getUnservedServices().get(0).getId());
        assertNotSame(extraService, visit.getUnservedServices().get(0));
        assertEquals(entryPoint, visit.getEntryPoint());
        assertEquals(Boolean.TRUE, visit.getPrintTicket());

        ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
        verify(segmentationRule, times(2)).getQueue(visitCaptor.capture(), eq(branch));
        assertSame(visit, visitCaptor.getAllValues().get(0));
        assertSame(visit, visitCaptor.getAllValues().get(1));

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2))
            .updateVisit(same(visit), (VisitEvent) eventCaptor.capture(), same(service));
        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.PLACED_IN_QUEUE), eventCaptor.getAllValues());

        VisitEvent createdEvent = eventCaptor.getAllValues().get(0);
        assertEquals("terminal", createdEvent.getParameters().get("visitCreator"));
        assertEquals(mainService.getId(), createdEvent.getParameters().get("serviceId"));
        assertEquals(mainService.getName(), createdEvent.getParameters().get("serviceName"));
        assertEquals(entryPoint.getPrinter().getId(), createdEvent.getParameters().get("printerId"));

        VisitEvent queueEvent = eventCaptor.getAllValues().get(1);
        assertEquals(mainService.getId(), queueEvent.getParameters().get("serviceId"));
        assertEquals(mainService.getName(), queueEvent.getParameters().get("serviceName"));
        assertEquals(queue.getId(), queueEvent.getParameters().get("queueId"));

        verify(branchService).incrementTicketCounter(branch.getId(), queue);
        verify(printerService).print(entryPoint.getPrinter().getId(), visit);
        verifyNoInteractions(eventService);
    }

    @DisplayName("Вторая версия создания визита возвращает 404 при пустом списке услуг")
    @Test
    void throwsNotFoundWhenServicesListEmpty() throws SystemException {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-empty", "Пустое отделение");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);

        EventService eventService = mock(EventService.class);
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services = new ArrayList<>();
        HashMap<String, String> parameters = new HashMap<>();

        HttpStatusException exception =
            assertThrows(
                HttpStatusException.class,
                () -> service.createVisit2(branch.getId(), "entry", services, parameters, false));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(branchService).getBranch(branch.getId());
        verify(eventService).send(eq("*"), eq(false), any());
        verifyNoMoreInteractions(eventService);
        verify(segmentationRule, never()).getQueue(any(Visit.class), any(Branch.class));
        verify(branchService, never()).incrementTicketCounter(anyString(), any());
        verify(printerService, never()).print(anyString(), any());
    }

    @DisplayName("Вторая версия создания визита возвращает 404 при отсутствии основной услуги в отделении")
    @Test
    void throwsNotFoundWhenPrimaryServiceMissingInBranch() throws SystemException {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-missing-service", "Отделение без услуги");

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);

        EventService eventService = mock(EventService.class);
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services =
            new ArrayList<>(List.of(new Service("unknown", "Неизвестная", 5, "queue")));
        HashMap<String, String> parameters = new HashMap<>();

        HttpStatusException exception =
            assertThrows(
                HttpStatusException.class,
                () -> service.createVisit2(branch.getId(), "entry", services, parameters, true));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(branchService).getBranch(branch.getId());
        verify(eventService).send(eq("*"), eq(false), any());
        verifyNoMoreInteractions(eventService);
        verify(segmentationRule, never()).getQueue(any(Visit.class), any(Branch.class));
        verify(branchService, never()).incrementTicketCounter(anyString(), any());
        verify(printerService, never()).print(anyString(), any());
    }

    @DisplayName("Вторая версия создания визита возвращает 404 при отсутствии терминала")
    @Test
    void throwsNotFoundWhenEntryPointMissing() throws SystemException {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-without-entry", "Отделение без терминала");
        Service serviceModel = new Service("service", "Регистрация", 10, "queue-main");
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

        ArrayList<Service> services =
            new ArrayList<>(
                List.of(
                    new Service(
                        serviceModel.getId(),
                        serviceModel.getName(),
                        serviceModel.getServingSL(),
                        serviceModel.getLinkedQueueId())));
        HashMap<String, String> parameters = new HashMap<>();

        HttpStatusException exception =
            assertThrows(
                HttpStatusException.class,
                () -> service.createVisit2(branch.getId(), "absent-entry", services, parameters, true));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(branchService).getBranch(branch.getId());
        verify(eventService).send(eq("*"), eq(false), any());
        verifyNoMoreInteractions(eventService);
        verify(segmentationRule, never()).getQueue(any(Visit.class), any(Branch.class));
        verify(branchService, never()).incrementTicketCounter(anyString(), any());
        verify(printerService, never()).print(anyString(), any());
    }

    @DisplayName("Вторая версия создания визита возвращает 400, если правило сегментации не подобрало очередь")
    @Test
    void throwsBadRequestWhenSegmentationRuleReturnsEmptyQueue() throws SystemException {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-empty-queue", "Отделение без очереди");
        branch.setPrefix("E");
        Service serviceModel = new Service("service", "Документы", 12, "queue" );
        branch.getServices().put(serviceModel.getId(), serviceModel);

        EntryPoint entryPoint = new EntryPoint();
        entryPoint.setId("entry-terminal");
        entryPoint.setName("Терминал");
        entryPoint.setPrinter(Entity.builder().id("printer").name("Принтер").build());
        branch.getEntryPoints().put(entryPoint.getId(), entryPoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);

        EventService eventService = mock(EventService.class);
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch))).thenReturn(Optional.empty());

        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services =
            new ArrayList<>(
                List.of(
                    new Service(
                        serviceModel.getId(),
                        serviceModel.getName(),
                        serviceModel.getServingSL(),
                        serviceModel.getLinkedQueueId())));
        HashMap<String, String> parameters = new HashMap<>();

        HttpStatusException exception =
            assertThrows(
                HttpStatusException.class,
                () -> service.createVisit2(
                        branch.getId(), entryPoint.getId(), services, parameters, true));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verify(branchService).getBranch(branch.getId());
        verify(segmentationRule).getQueue(any(Visit.class), eq(branch));
        verify(eventService).send(eq("*"), eq(false), any());
        verifyNoMoreInteractions(eventService);
        verify(branchService, never()).incrementTicketCounter(anyString(), any());
        verify(branchService, never())
            .updateVisit(any(Visit.class), any(VisitEvent.class), any(VisitService.class));
        verify(printerService, never()).print(anyString(), any());
    }

    @DisplayName("Вторая версия создания визита возвращает 404, если очередь отсутствует в конфигурации отделения")
    @Test
    void throwsNotFoundWhenQueueMissingInBranchConfiguration() throws SystemException {
        VisitService service = new VisitService();

        Branch branch = new Branch("branch-no-queue", "Отделение без конфигурации очереди");
        branch.setPrefix("N");
        branch.setPath("/no-queue");
        Service serviceModel = new Service("service", "Получение", 9, "queue-linked");
        branch.getServices().put(serviceModel.getId(), serviceModel);

        EntryPoint entryPoint = new EntryPoint();
        entryPoint.setId("entry-terminal");
        entryPoint.setName("Терминал");
        entryPoint.setPrinter(Entity.builder().id("printer-noqueue").name("Принтер").build());
        branch.getEntryPoints().put(entryPoint.getId(), entryPoint);

        Queue externalQueue = new Queue("queue-external", "Чужая очередь", "N", 45);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(branchService.incrementTicketCounter(eq(branch.getId()), any(Queue.class)))
            .thenAnswer(invocation -> {
                Queue q = invocation.getArgument(1);
                q.setTicketCounter(q.getTicketCounter() + 1);
                return q.getTicketCounter();
            });

        EventService eventService = mock(EventService.class);
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch)))
            .thenAnswer(invocation -> Optional.of(externalQueue));

        PrinterService printerService = mock(PrinterService.class);

        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.printerService = printerService;

        ArrayList<Service> services =
            new ArrayList<>(
                List.of(
                    new Service(
                        serviceModel.getId(),
                        serviceModel.getName(),
                        serviceModel.getServingSL(),
                        serviceModel.getLinkedQueueId())));
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "terminal");

        HttpStatusException exception =
            assertThrows(
                HttpStatusException.class,
                () -> service.createVisit2(
                        branch.getId(), entryPoint.getId(), services, parameters, true));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        ArgumentCaptor<Visit> visitCaptor = ArgumentCaptor.forClass(Visit.class);
        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService)
            .updateVisit(visitCaptor.capture(), (VisitEvent) eventCaptor.capture(), same(service));
        Visit capturedVisit = visitCaptor.getValue();
        assertEquals(externalQueue.getId(), capturedVisit.getQueueId());
        assertEquals("N001", capturedVisit.getTicket());
        assertEquals(parameters, capturedVisit.getParameterMap());
        assertEquals(VisitEvent.CREATED, eventCaptor.getValue());

        verify(branchService).incrementTicketCounter(branch.getId(), externalQueue);
        verify(segmentationRule, times(2)).getQueue(any(Visit.class), eq(branch));
        verify(printerService, never()).print(anyString(), any());
        verify(eventService).send(eq("*"), eq(false), any());
        verifyNoMoreInteractions(eventService);
    }
}
