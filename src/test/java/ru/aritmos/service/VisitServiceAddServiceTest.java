package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.model.visit.VisitEvent;

/**
 * Юнит-тесты для {@link VisitService#addService(String, String, String)}.
 */
class VisitServiceAddServiceTest {

    @Test
    void addServiceAppendsToUnservedServices() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        Service serviceDef = new Service("s1", "Service", 10, "q1");
        Visit visit = Visit.builder()
                .id("v1")
                .unservedServices(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        sp.setVisit(visit);
        sp.setUser(new User("u1", "User", null));
        branch.getServicePoints().put(sp.getId(), sp);
        branch.getServices().put(serviceDef.getId(), serviceDef);

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        service.eventService = mock(EventService.class);

        Visit result = service.addService("b1", "sp1", "s1");
        assertEquals(1, result.getUnservedServices().size());
        assertSame(serviceDef, result.getUnservedServices().get(0));
        verify(branchService).updateVisit(eq(visit), any(VisitEvent.class), eq(service));
    }

    @Test
    void addServiceThrowsWhenUnknown() {
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP1");
        sp.setVisit(Visit.builder().id("v1").unservedServices(new ArrayList<>()).events(new ArrayList<>()).build());
        branch.getServicePoints().put(sp.getId(), sp);
        branch.getServices().put("s1", new Service("s1", "Service", 10, "q1"));

        BranchService branchService = mock(BranchService.class);
        when(branchService.getBranch("b1")).thenReturn(branch);

        VisitService service = new VisitService();
        service.branchService = branchService;
        EventService eventService = mock(EventService.class);
        service.eventService = eventService;

        assertThrows(HttpStatusException.class, () -> service.addService("b1", "sp1", "missing"));
        verify(eventService).send(anyString(), eq(false), any());
    }
}
