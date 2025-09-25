package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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

    @DisplayName("проверяется сценарий «update delegates to configuration»")
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

    @DisplayName("проверяется сценарий «update hardcode uses demo config»")
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

    @DisplayName("проверяется сценарий «add update service delegates to branch service»")
    @Test
    void addUpdateServiceDelegatesToBranchService() {
        ConfigurationController controller = controller();
        HashMap<String, Service> services = new HashMap<>();
        controller.addUpdateService("b1", services, true);
        verify(controller.branchService)
            .addUpdateService("b1", services, true, controller.visitService);
    }

    @DisplayName("проверяется сценарий «get break reasons uses branch service»")
    @Test
    void getBreakReasonsUsesBranchService() {
        ConfigurationController controller = controller();
        Branch branch = new Branch("b1", "Branch");
        branch.setBreakReasons(new HashMap<>(Map.of("1", "Reason")));
        when(controller.branchService.getBranch("b1")).thenReturn(branch);

        assertEquals("Reason", controller.getBreakReasons("b1").get("1"));
        verify(controller.branchService).getBranch("b1");
    }

    @DisplayName("проверяется сценарий «delete services delegates»")
    @Test
    void deleteServicesDelegates() {
        ConfigurationController controller = controller();
        List<String> ids = List.of("s1");
        controller.deleteServices("b1", ids, false);
        verify(controller.branchService)
            .deleteServices("b1", ids, false, controller.visitService);
    }

    @DisplayName("проверяется сценарий «add update service point delegates»")
    @Test
    void addUpdateServicePointDelegates() {
        ConfigurationController controller = controller();
        HashMap<String, ServicePoint> points = new HashMap<>();
        controller.addUpdateServicePoint("b1", points, true, false);
        verify(controller.branchService)
            .addUpdateServicePoint("b1", points, true, false);
    }

    @DisplayName("проверяется сценарий «add update service groups delegates»")
    @Test
    void addUpdateServiceGroupsDelegates() {
        ConfigurationController controller = controller();
        HashMap<String, ServiceGroup> groups = new HashMap<>();
        controller.addUpdateServiceGroups("b1", groups);
        verify(controller.branchService).addUpdateServiceGroups("b1", groups);
    }

    @DisplayName("проверяется сценарий «add update segmentation rules delegates»")
    @Test
    void addUpdateSegmentationRulesDelegates() {
        ConfigurationController controller = controller();
        HashMap<String, SegmentationRuleData> rules = new HashMap<>();
        controller.addUpdateSegmentationRules("b1", rules);
        verify(controller.branchService).addUpdateSegmentationRules("b1", rules);
    }

    @DisplayName("проверяется сценарий «delete service points delegates»")
    @Test
    void deleteServicePointsDelegates() {
        ConfigurationController controller = controller();
        List<String> ids = List.of("sp1");
        controller.deleteServicePoints("b1", ids);
        verify(controller.branchService).deleteServicePoints("b1", ids);
    }

    @DisplayName("проверяется сценарий «set auto call mode on delegates»")
    @Test
    void setAutoCallModeOnDelegates() {
        ConfigurationController controller = controller();
        controller.setAutoCallModeOfBranchOn("b1");
        verify(controller.visitService).setAutoCallModeOfBranch("b1", true);
    }

    @DisplayName("проверяется сценарий «set auto call mode off delegates»")
    @Test
    void setAutoCallModeOffDelegates() {
        ConfigurationController controller = controller();
        controller.setAutoCallModeOfBranchOff("b1");
        verify(controller.visitService).setAutoCallModeOfBranch("b1", false);
    }

    @DisplayName("проверяется сценарий «add update queues delegates»")
    @Test
    void addUpdateQueuesDelegates() {
        ConfigurationController controller = controller();
        HashMap<String, Queue> queues = new HashMap<>();
        controller.addUpdateQueues("b1", queues, true);
        verify(controller.branchService).addUpdateQueues("b1", queues, true);
    }

    @DisplayName("проверяется сценарий «delete queues delegates»")
    @Test
    void deleteQueuesDelegates() {
        ConfigurationController controller = controller();
        List<String> ids = List.of("q1");
        controller.deleteQueues("b1", ids);
        verify(controller.branchService).deleteQueues("b1", ids);
    }
}