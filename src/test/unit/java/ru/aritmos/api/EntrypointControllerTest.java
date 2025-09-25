package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Service;
import ru.aritmos.model.Branch;
import ru.aritmos.model.VisitParameters;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

class EntrypointControllerTest {

    @DisplayName("Получение доступных услуг возвращает список сервисов")
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
        assertEquals(list, controller.getAllAvailableServices("b1"));
    }



    private EntrypointController controller() {
        EntrypointController controller = new EntrypointController();
        controller.services = mock(Services.class);
        controller.branchService = mock(BranchService.class);
        controller.visitService = mock(VisitService.class);
        controller.eventService = mock(EventService.class);
        return controller;
    }

    @DisplayName("Метод createVirtualVisit делегирует создание виртуального визита сервису VisitService")
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

    @DisplayName("Метод createVisit без сегментации делегирует создание визита сервису VisitService")
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

    @DisplayName("Метод createVisit с сегментацией делегирует создание визита сервису VisitService")
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

    @DisplayName("Метод createVisit с параметрами делегирует создание визита сервису VisitService")
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

    @DisplayName("Метод createVisit с параметрами и сегментацией делегирует создание визита сервису VisitService")
    @Test
    void createVisitWithParametersDelegatesWithSegmentation() throws Exception {
        EntrypointController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.getServices().put("s1", new Service("s1", "S", 1, "q1"));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);
        Visit visit = Visit.builder().build();
        VisitParameters params = VisitParameters.builder()
            .serviceIds(new ArrayList<>(List.of("s1")))
            .parameters(new HashMap<>())
            .build();
        when(controller.visitService.createVisit(eq("b1"), eq("e1"), same(params), eq(true), eq("seg")))
            .thenReturn(visit);

        assertSame(visit, controller.createVisit("b1", "e1", params, true, "seg"));
    }

    @DisplayName("Метод createVisitFromReception без сегментации делегирует создание визита сервису VisitService")
    @Test
    void createVisitFromReceptionDelegatesWithoutSegmentation() throws Exception {
        EntrypointController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.getServices().put("s1", new Service("s1", "S", 1, "q1"));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);
        Visit visit = Visit.builder().build();
        VisitParameters params = VisitParameters.builder()
            .serviceIds(new ArrayList<>(List.of("s1")))
            .parameters(new HashMap<>())
            .build();
        when(controller.visitService.createVisitFromReception(
                eq("b1"), eq("p1"), same(params), eq(true), eq("sid")))
            .thenReturn(visit);

        assertSame(visit, controller.createVisitFromReception("b1", "p1", params, true, null, "sid"));
        verify(controller.visitService)
            .createVisitFromReception("b1", "p1", params, true, "sid");
    }

    @DisplayName("Метод createVisitFromReception с сегментацией делегирует создание визита сервису VisitService")
    @Test
    void createVisitFromReceptionDelegatesWithSegmentation() throws Exception {
        EntrypointController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.getServices().put("s1", new Service("s1", "S", 1, "q1"));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);
        Visit visit = Visit.builder().build();
        VisitParameters params = VisitParameters.builder()
            .serviceIds(new ArrayList<>(List.of("s1")))
            .parameters(new HashMap<>())
            .build();
        when(controller.visitService.createVisitFromReception(
                eq("b1"), eq("p1"), same(params), eq(false), eq("seg"), eq("sid")))
            .thenReturn(visit);

        assertSame(visit, controller.createVisitFromReception("b1", "p1", params, false, "seg", "sid"));
        verify(controller.visitService)
            .createVisitFromReception("b1", "p1", params, false, "seg", "sid");
    }

    @DisplayName("Метод setParameterMap обновляет параметры визита и сохраняет изменения через BranchService")
    @Test
    void setParameterMapUpdatesVisitAndDelegates() {
        EntrypointController controller = controller();
        Visit visit = Visit.builder().parameterMap(new HashMap<>()).build();
        when(controller.visitService.getVisit("b1", "v1")).thenReturn(visit);
        HashMap<String, String> params = new HashMap<>(Map.of("k", "v"));

        Visit result = controller.setParameterMap("b1", "v1", params);

        assertSame(visit, result);
        assertEquals(params, visit.getParameterMap());
        verify(controller.branchService)
            .updateVisit(visit, "VISIT_SET_PARAMETER_MAP", controller.visitService);
    }

    @DisplayName("Получение всех услуг делегируется сервису Services")
    @Test
    void getAllServicesDelegatesToServices() {
        EntrypointController controller = controller();
        List<Service> services = List.of(new Service("s1", "Service", 1, "q1"));
        when(controller.services.getAllServices("b1")).thenReturn(services);

        assertSame(services, controller.getAllServices("b1"));
    }
}
