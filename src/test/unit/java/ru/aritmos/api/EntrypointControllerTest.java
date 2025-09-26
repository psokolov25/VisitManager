package ru.aritmos.api;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Service;
import ru.aritmos.model.VisitParameters;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;

class EntrypointControllerTest {

    @DisplayName("Получение доступных услуг возвращает полный список")
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

    @DisplayName("Создание виртуального визита выполняет сервис визитов")
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

    @DisplayName("Создание визита без сегментации выполняет сервис визитов")
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

    @DisplayName("Создание визита с сегментацией выполняет сервис визитов")
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

    @DisplayName("Создание визита с объектом параметров выполняет сервис визитов")
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

    @DisplayName("Создание визита с параметрами и сегментацией выполняет сервис визитов")
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

    @DisplayName("Создание визита из приёмной без сегментации выполняет сервис визитов")
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

    @DisplayName("Создание визита из приёмной с сегментацией выполняет сервис визитов")
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

    @DisplayName("Обновление параметров визита сохраняет изменения через сервис")
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

    @DisplayName("Получение всех услуг обращается к агрегирующему сервису")
    @Test
    void getAllServicesDelegatesToServices() {
        EntrypointController controller = controller();
        List<Service> services = List.of(new Service("s1", "Service", 1, "q1"));
        when(controller.services.getAllServices("b1")).thenReturn(services);

        assertSame(services, controller.getAllServices("b1"));
    }
}
