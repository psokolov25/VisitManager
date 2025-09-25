package ru.aritmos.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.aritmos.test.LoggingAssertions.assertEquals;
import static ru.aritmos.test.LoggingAssertions.assertNotNull;
import static ru.aritmos.test.LoggingAssertions.assertNotSame;
import static ru.aritmos.test.LoggingAssertions.assertNull;
import static ru.aritmos.test.LoggingAssertions.assertSame;
import static ru.aritmos.test.LoggingAssertions.assertThrows;
import static ru.aritmos.test.LoggingAssertions.assertTrue;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;
import ru.aritmos.service.rules.SegmentationRule;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Юнит-тесты для {@link VisitService#createVirtualVisit2(String, String, ArrayList, HashMap, String)}.
 */
@ExtendWith(TestLoggingExtension.class)
class VisitServiceCreateVirtualVisit2Test {

    @BeforeEach
    void resetVisitEventParameters() {
        for (VisitEvent event : List.of(VisitEvent.CREATED, VisitEvent.CALLED, VisitEvent.START_SERVING)) {
            event.getParameters().clear();
        }
    }

    @DisplayName("Create Virtual Visit2Creates Visit And Starts Serving With Service Point Staff")
    @Test
    void createVirtualVisit2CreatesVisitAndStartsServingWithServicePointStaff() throws SystemException {
        Branch branch = new Branch("b1", "Отделение №1");
        branch.setPrefix("BR");
        branch.setPath("/branches/br");
        Service primary = new Service("svc1", "Консультация", 15, "q-main");
        branch.getServices().put(primary.getId(), primary);
        Queue queue = new Queue("q-main", "Основная очередь", "A", 120);
        branch.getQueues().put(queue.getId(), queue);
        ServicePoint servicePoint = new ServicePoint("sp1", "Окно 1");
        User staff = new User();
        staff.setId("staff-1");
        staff.setName("Иван Иванов");
        staff.setCurrentWorkProfileId("wp-1");
        servicePoint.setUser(staff);
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

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
        when(segmentationRule.getQueue(any(Visit.class), eq(branch))).thenReturn(Optional.of(queue));
        KeyCloackClient keycloak = mock(KeyCloackClient.class);
        when(keycloak.getUserBySid("sid-1")).thenReturn(Optional.empty());

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.keyCloackClient = keycloak;

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("source", "workstation");
        ArrayList<Service> services = new ArrayList<>(List.of(primary));

        Visit visit = service.createVirtualVisit2(branch.getId(), servicePoint.getId(), services, parameters, "sid-1");

        assertEquals(branch.getId(), visit.getBranchId());
        assertEquals(branch.getName(), visit.getBranchName());
        assertEquals(branch.getPrefix(), visit.getBranchPrefix());
        assertEquals(servicePoint.getId(), visit.getServicePointId());
        assertEquals("A001", visit.getTicket());
        assertSame(parameters, visit.getParameterMap());
        assertEquals(queue.getId(), visit.getParameterMap().get("LastQueueId"));
        assertNull(visit.getQueueId());
        assertEquals(primary.getId(), visit.getCurrentService().getId());
        assertNotSame(primary, visit.getCurrentService());
        assertTrue(visit.getUnservedServices().isEmpty());
        assertNotNull(visit.getCallDateTime());
        assertNotNull(visit.getStartServingDateTime());
        assertEquals(1, queue.getTicketCounter());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(3)).updateVisit(eq(visit), eventCaptor.capture(), eq(service));
        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.CALLED, VisitEvent.START_SERVING), eventCaptor.getAllValues());

        VisitEvent created = VisitEvent.CREATED;
        assertEquals("true", created.getParameters().get("isVirtual"));
        assertEquals("workstation", created.getParameters().get("visitCreator"));
        assertEquals(primary.getId(), created.getParameters().get("serviceId"));
        assertEquals(primary.getName(), created.getParameters().get("serviceName"));
        assertEquals(queue.getId(), created.getParameters().get("queueId"));
        assertEquals(staff.getId(), created.getParameters().get("staffId"));
        assertEquals(staff.getName(), created.getParameters().get("staffName"));
        assertEquals(staff.getCurrentWorkProfileId(), created.getParameters().get("workProfileId"));

        VisitEvent called = VisitEvent.CALLED;
        assertEquals(queue.getId(), called.getParameters().get("queueId"));
        assertEquals("virtual", called.getParameters().get("callMethod"));
        assertEquals(staff.getId(), called.getParameters().get("staffId"));
        assertEquals(staff.getName(), called.getParameters().get("staffName"));

        VisitEvent startServing = VisitEvent.START_SERVING;
        assertEquals(queue.getId(), startServing.getParameters().get("queueId"));
        assertEquals(primary.getId(), startServing.getParameters().get("serviceId"));
        assertEquals(primary.getName(), startServing.getParameters().get("serviceName"));
        assertEquals(staff.getId(), startServing.getParameters().get("staffId"));
        assertEquals(staff.getName(), startServing.getParameters().get("staffName"));
    }

    @DisplayName("Create Virtual Visit2Uses Keycloak Data When Service Point Without User")
    @Test
    void createVirtualVisit2UsesKeycloakDataWhenServicePointWithoutUser() throws SystemException {
        Branch branch = new Branch("b2", "Отделение №2");
        branch.setPrefix("BK");
        branch.setPath("/branches/bk");
        Service primary = new Service("svc1", "Основная услуга", 20, "q-main");
        Service secondary = new Service("svc2", "Дополнительная услуга", 15, "q-main");
        branch.getServices().put(primary.getId(), primary);
        branch.getServices().put(secondary.getId(), secondary);
        Queue queue = new Queue("q-main", "Основная очередь", "B", 90);
        branch.getQueues().put(queue.getId(), queue);
        ServicePoint servicePoint = new ServicePoint("sp2", "Окно 2");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(branchService.incrementTicketCounter(eq(branch.getId()), any(Queue.class)))
            .thenAnswer(invocation -> {
                Queue q = invocation.getArgument(1);
                q.setTicketCounter(27);
                return q.getTicketCounter();
            });

        EventService eventService = mock(EventService.class);
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch))).thenReturn(Optional.of(queue));
        KeyCloackClient keycloak = mock(KeyCloackClient.class);
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setId("kc-1");
        keycloakUser.setUsername("operator");
        when(keycloak.getUserBySid("sid-2")).thenReturn(Optional.of(keycloakUser));

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.keyCloackClient = keycloak;

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("channel", "app");
        ArrayList<Service> services = new ArrayList<>(List.of(primary, secondary));

        Visit visit = service.createVirtualVisit2(branch.getId(), servicePoint.getId(), services, parameters, "sid-2");

        assertEquals("B027", visit.getTicket());
        assertSame(parameters, visit.getParameterMap());
        assertEquals(queue.getId(), visit.getParameterMap().get("LastQueueId"));
        assertEquals(primary.getId(), visit.getCurrentService().getId());
        assertNotSame(primary, visit.getCurrentService());
        assertEquals(1, visit.getUnservedServices().size());
        assertEquals(secondary.getId(), visit.getUnservedServices().get(0).getId());
        assertNotSame(secondary, visit.getUnservedServices().get(0));
        assertNotNull(visit.getCallDateTime());
        assertNotNull(visit.getStartServingDateTime());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(3)).updateVisit(eq(visit), eventCaptor.capture(), eq(service));
        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.CALLED, VisitEvent.START_SERVING), eventCaptor.getAllValues());

        VisitEvent created = VisitEvent.CREATED;
        assertEquals(keycloakUser.getId(), created.getParameters().get("staffId"));
        assertEquals(keycloakUser.getUsername(), created.getParameters().get("staffName"));
        assertEquals("", created.getParameters().get("workProfileId"));

        VisitEvent called = VisitEvent.CALLED;
        assertEquals("", called.getParameters().get("staffId"));
        assertEquals("", called.getParameters().get("staffName"));
        assertEquals(queue.getId(), called.getParameters().get("queueId"));
        assertEquals("virtual", called.getParameters().get("callMethod"));

        VisitEvent startServing = VisitEvent.START_SERVING;
        assertEquals("", startServing.getParameters().get("staffId"));
        assertEquals("", startServing.getParameters().get("staffName"));
        assertEquals(primary.getId(), startServing.getParameters().get("serviceId"));
        assertEquals(primary.getName(), startServing.getParameters().get("serviceName"));
    }

    @DisplayName("Create Virtual Visit2Throws Not Found When Queue Missing In Branch")
    @Test
    void createVirtualVisit2ThrowsNotFoundWhenQueueMissingInBranch() throws SystemException {
        Branch branch = new Branch("b3", "Отделение №3");
        Service primary = new Service("svc1", "Услуга", 10, "q-main");
        branch.getServices().put(primary.getId(), primary);
        ServicePoint servicePoint = new ServicePoint("sp3", "Окно 3");
        branch.getServicePoints().put(servicePoint.getId(), servicePoint);
        Queue externalQueue = new Queue("foreign", "Внешняя очередь", "Z", 100);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch(branch.getId())).thenReturn(branch);
        when(branchService.incrementTicketCounter(eq(branch.getId()), any(Queue.class)))
            .thenAnswer(invocation -> {
                Queue q = invocation.getArgument(1);
                q.setTicketCounter(3);
                return q.getTicketCounter();
            });

        EventService eventService = mock(EventService.class);
        SegmentationRule segmentationRule = mock(SegmentationRule.class);
        when(segmentationRule.getQueue(any(Visit.class), eq(branch))).thenReturn(Optional.of(externalQueue));
        KeyCloackClient keycloak = mock(KeyCloackClient.class);
        when(keycloak.getUserBySid("sid-3")).thenReturn(Optional.empty());

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = eventService;
        service.segmentationRule = segmentationRule;
        service.keyCloackClient = keycloak;

        HashMap<String, String> parameters = new HashMap<>();
        ArrayList<Service> services = new ArrayList<>(List.of(primary));

        HttpStatusException exception = assertThrows(
            HttpStatusException.class,
            () -> service.createVirtualVisit2(branch.getId(), servicePoint.getId(), services, parameters, "sid-3"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Queue not found in branch configuration", exception.getMessage());
        assertEquals(3, externalQueue.getTicketCounter());

        ArgumentCaptor<VisitEvent> eventCaptor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(branchService, times(2)).updateVisit(any(Visit.class), eventCaptor.capture(), eq(service));
        assertEquals(List.of(VisitEvent.CREATED, VisitEvent.CALLED), eventCaptor.getAllValues());
    }
}
