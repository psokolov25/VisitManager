package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.DeliveredService;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

/**
 * Юнит-тесты для {@link VisitService#getDeliveredServices(String, String)}.
 */
class VisitServiceDeliveredServicesTest {

    @Test
    void returnsDeliveredServicesOfCurrentService() {
        VisitService service = new VisitService();
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");

        Service current = new Service("s1", "Service", 10, "q1");
        DeliveredService delivered = new DeliveredService("ds1", "Delivered");
        current.getDeliveredServices().put(delivered.getId(), delivered);

        Visit visit = Visit.builder().id("v1").currentService(current).build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Map<String, DeliveredService> result = service.getDeliveredServices("b1", "sp1");
        assertEquals(1, result.size());
        assertSame(delivered, result.get("ds1"));
    }

    @Test
    void throwsWhenCurrentServiceMissing() {
        VisitService service = new VisitService();
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setVisit(Visit.builder().id("v1").build());
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = new BranchService();
        branchService.eventService = mock(EventService.class);
        branchService.keyCloackClient = mock(KeyCloackClient.class);
        branchService.branches.put(branch.getId(), branch);

        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.getDeliveredServices("b1", "sp1"));
        verify(eventService).send(anyString(), eq(false), any());
    }

    @Test
    void addDeliveredServiceAddsToCurrent() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        Service current = new Service("s1", "Service", 10, "q1");
        Visit visit = Visit.builder().id("v1").currentService(current).visitEvents(List.of()).events(List.of()).build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        DeliveredService delivered = new DeliveredService("ds1", "Delivered");
        delivered.setServiceIds(List.of("s1"));
        branch.getPossibleDeliveredServices().put(delivered.getId(), delivered);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.addDeliveredService("b1", "sp1", "ds1");
        assertTrue(result.getCurrentService().getDeliveredServices().containsKey("ds1"));
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }

    @Test
    void addDeliveredServiceThrowsWhenUnknown() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        Service current = new Service("s1", "Service", 10, "q1");
        Visit visit = Visit.builder().id("v1").currentService(current).build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.addDeliveredService("b1", "sp1", "missing"));
        verify(eventService).send(anyString(), eq(false), any());
    }

    @Test
    void deleteDeliveredServiceRemovesFromCurrent() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        Service current = new Service("s1", "Service", 10, "q1");
        DeliveredService delivered = new DeliveredService("ds1", "Delivered");
        delivered.setServiceIds(List.of("s1"));
        current.getDeliveredServices().put(delivered.getId(), delivered);
        Visit visit = Visit.builder().id("v1").currentService(current).visitEvents(List.of()).events(List.of()).build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);
        branch.getPossibleDeliveredServices().put(delivered.getId(), delivered);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.deleteDeliveredService("b1", "sp1", "ds1");
        assertTrue(result.getCurrentService().getDeliveredServices().isEmpty());
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }

    @Test
    void deleteDeliveredServiceThrowsWhenUnknown() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        Service current = new Service("s1", "Service", 10, "q1");
        Visit visit = Visit.builder().id("v1").currentService(current).build();
        sp.setVisit(visit);
        branch.getServicePoints().put(sp.getId(), sp);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.deleteDeliveredService("b1", "sp1", "ds1"));
        verify(eventService).send(anyString(), eq(false), any());
    }
}

