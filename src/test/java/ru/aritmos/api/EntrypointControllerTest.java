package ru.aritmos.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Service;
import ru.aritmos.model.Branch;
import ru.aritmos.model.VisitParameters;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

class EntrypointControllerTest {

    @Test
    void getAllAvailableServicesReturnsList() {
        Services services = mock(Services.class);
        List<Service> list = List.of(new Service("s1", "Service", 1, "q1"));
        when(services.getAllAvailableServices("b1")).thenReturn(list);
        EntrypointController controller = new EntrypointController();
        controller.services = services;
        controller.branchService = mock(BranchService.class);
        controller.visitService = mock(VisitService.class);
        controller.eventService = mock(EventService.class);
        assertEquals(list, controller.getAllAvilableServies("b1"));
    }

    @Test
    void getAllAvailableServicesMapsBusinessException() {
        Services services = mock(Services.class);
        BusinessException ex = mock(BusinessException.class);
        when(ex.getMessage()).thenReturn("not found");
        when(services.getAllAvailableServices("b1")).thenThrow(ex);
        EntrypointController controller = new EntrypointController();
        controller.services = services;
        controller.branchService = mock(BranchService.class);
        controller.visitService = mock(VisitService.class);
        controller.eventService = mock(EventService.class);
        HttpStatusException thrown =
            assertThrows(HttpStatusException.class, () -> controller.getAllAvilableServies("b1"));
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
    }

    private EntrypointController controller() {
        EntrypointController controller = new EntrypointController();
        controller.services = mock(Services.class);
        controller.branchService = mock(BranchService.class);
        controller.visitService = mock(VisitService.class);
        controller.eventService = mock(EventService.class);
        return controller;
    }

    @Test
    void createVirtualVisitDelegatesToService() throws Exception {
        EntrypointController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.getServices().put("s1", new Service("s1", "S", 1, "q1"));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);
        Visit visit = Visit.builder().build();
        when(controller.visitService.createVirtualVisit(eq("b1"), eq("sp1"), any(), eq("sid")))
            .thenReturn(visit);

        ArrayList<String> ids = new ArrayList<>(List.of("s1"));
        assertSame(visit, controller.createVirtualVisit("b1", "sp1", ids, "sid"));
    }

    @Test
    void createVisitDelegatesToServiceWhenSegmentationEmpty() throws Exception {
        EntrypointController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.getServices().put("s1", new Service("s1", "S", 1, "q1"));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);
        Visit visit = Visit.builder().build();
        when(controller.visitService.createVisit(eq("b1"), eq("e1"), any(VisitParameters.class), eq(false)))
            .thenReturn(visit);

        ArrayList<String> ids = new ArrayList<>(List.of("s1"));
        assertSame(visit, controller.createVisit("b1", "e1", ids, false, null));
    }

    @Test
    void createVisitDelegatesToServiceWhenSegmentationProvided() throws Exception {
        EntrypointController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.getServices().put("s1", new Service("s1", "S", 1, "q1"));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);
        Visit visit = Visit.builder().build();
        when(controller.visitService.createVisit(eq("b1"), eq("e1"), any(VisitParameters.class), eq(false), eq("seg")))
            .thenReturn(visit);

        ArrayList<String> ids = new ArrayList<>(List.of("s1"));
        assertSame(visit, controller.createVisit("b1", "e1", ids, false, "seg"));
    }

    @Test
    void createVisitWithParametersDelegates() throws Exception {
        EntrypointController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.getServices().put("s1", new Service("s1", "S", 1, "q1"));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);
        Visit visit = Visit.builder().build();
        when(controller.visitService.createVisit(eq("b1"), eq("e1"), any(VisitParameters.class), eq(true)))
            .thenReturn(visit);

        VisitParameters params = VisitParameters.builder()
            .serviceIds(new ArrayList<>(List.of("s1")))
            .parameters(new HashMap<>())
            .build();
        assertSame(visit, controller.createVisit("b1", "e1", params, true, null));
    }
}
