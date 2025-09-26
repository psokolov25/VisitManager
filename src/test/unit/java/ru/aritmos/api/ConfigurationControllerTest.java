package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.SegmentationRuleData;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServiceGroup;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Configuration;
import ru.aritmos.service.VisitService;

class ConfigurationControllerTest {

    @DisplayName("Контроллер конфигурации делегирует обновление сервису настроек")
    @Test
    void updateDelegatesToConfiguration() {
        ConfigurationController controller = new ConfigurationController();
        controller.branchService = mock(BranchService.class);
        controller.visitService = mock(VisitService.class);
        Configuration config = mock(Configuration.class);
        controller.configuration = config;

        HashMap<String, Branch> branches = new HashMap<>();
        when(config.createBranchConfiguration(branches)).thenReturn(branches);

        assertSame(branches, controller.update(branches));
        verify(config).createBranchConfiguration(branches);
    }

    @DisplayName("Контроллер без параметров использует демонстрационную конфигурацию")
    @Test
    void updateHardcodeUsesDemoConfig() {
        ConfigurationController controller = new ConfigurationController();
        controller.branchService = mock(BranchService.class);
        controller.visitService = mock(VisitService.class);
        Configuration config = mock(Configuration.class);
        controller.configuration = config;

        HashMap<String, Branch> demo = new HashMap<>();
        when(config.createDemoBranch()).thenReturn(demo);
        when(config.createBranchConfiguration(demo)).thenReturn(demo);

        assertSame(demo, controller.update());
        verify(config).createDemoBranch();
        verify(config).createBranchConfiguration(demo);
    }

    private ConfigurationController controller() {
        ConfigurationController controller = new ConfigurationController();
        controller.branchService = mock(BranchService.class);
        controller.visitService = mock(VisitService.class);
        controller.configuration = mock(Configuration.class);
        return controller;
    }

    @DisplayName("Контроллер делегирует добавление и обновление услуг сервису отделений")
    @Test
    void addUpdateServiceDelegatesToBranchService() {
        ConfigurationController controller = controller();
        HashMap<String, Service> services = new HashMap<>();
        controller.addUpdateService("b1", services, true);
        verify(controller.branchService)
            .addUpdateService("b1", services, true, controller.visitService);
    }

    @DisplayName("Контроллер поручает получение причин перерывов сервису отделений")
    @Test
    void getBreakReasonsUsesBranchService() {
        ConfigurationController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.setBreakReasons(new HashMap<>(Map.of("1", "Reason")));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);

        assertEquals("Reason", controller.getBreakReasons("b1").get("1"));
        verify(controller.branchService).getBranch("b1");
    }

    @DisplayName("Контроллер делегирует удаление услуг сервису отделений")
    @Test
    void deleteServicesDelegates() {
        ConfigurationController controller = controller();
        List<String> ids = List.of("s1");
        controller.deleteServices("b1", ids, false);
        verify(controller.branchService)
            .deleteServices("b1", ids, false, controller.visitService);
    }

    @DisplayName("Контроллер делегирует управление точками обслуживания сервису отделений")
    @Test
    void addUpdateServicePointDelegates() {
        ConfigurationController controller = controller();
        HashMap<String, ServicePoint> points = new HashMap<>();
        controller.addUpdateServicePoint("b1", points, true, false);
        verify(controller.branchService)
            .addUpdateServicePoint("b1", points, true, false);
    }

    @DisplayName("Контроллер делегирует управление группами услуг сервису отделений")
    @Test
    void addUpdateServiceGroupsDelegates() {
        ConfigurationController controller = controller();
        HashMap<String, ServiceGroup> groups = new HashMap<>();
        controller.addUpdateServiceGroups("b1", groups);
        verify(controller.branchService).addUpdateServiceGroups("b1", groups);
    }

    @DisplayName("Контроллер делегирует управление правилами сегментации сервису отделений")
    @Test
    void addUpdateSegmentationRulesDelegates() {
        ConfigurationController controller = controller();
        HashMap<String, SegmentationRuleData> rules = new HashMap<>();
        controller.addUpdateSegmentationRules("b1", rules);
        verify(controller.branchService).addUpdateSegmentationRules("b1", rules);
    }

    @DisplayName("Контроллер делегирует удаление точек обслуживания сервису отделений")
    @Test
    void deleteServicePointsDelegates() {
        ConfigurationController controller = controller();
        List<String> ids = List.of("sp1");
        controller.deleteServicePoints("b1", ids);
        verify(controller.branchService).deleteServicePoints("b1", ids);
    }

    @DisplayName("Контроллер поручает сервису визитов включить автообзвон отделения")
    @Test
    void setAutoCallModeOnDelegates() {
        ConfigurationController controller = controller();
        controller.setAutoCallModeOfBranchOn("b1");
        verify(controller.visitService).setAutoCallModeOfBranch("b1", true);
    }

    @DisplayName("Контроллер поручает сервису визитов отключить автообзвон отделения")
    @Test
    void setAutoCallModeOffDelegates() {
        ConfigurationController controller = controller();
        controller.setAutoCallModeOfBranchOff("b1");
        verify(controller.visitService).setAutoCallModeOfBranch("b1", false);
    }

    @DisplayName("Контроллер делегирует управление очередями сервису отделений")
    @Test
    void addUpdateQueuesDelegates() {
        ConfigurationController controller = controller();
        HashMap<String, Queue> queues = new HashMap<>();
        controller.addUpdateQueues("b1", queues, true);
        verify(controller.branchService).addUpdateQueues("b1", queues, true);
    }

    @DisplayName("Контроллер делегирует удаление очередей сервису отделений")
    @Test
    void deleteQueuesDelegates() {
        ConfigurationController controller = controller();
        List<String> ids = List.of("q1");
        controller.deleteQueues("b1", ids);
        verify(controller.branchService).deleteQueues("b1", ids);
    }
}
