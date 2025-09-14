package ru.aritmos.service.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServiceGroup;
import ru.aritmos.model.SegmentationRuleData;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.exceptions.SystemException;
import io.micronaut.http.exceptions.HttpStatusException;
import ru.aritmos.service.BranchService;

class SegmentationRuleTest {
  SegmentationRule rule;
  BranchService branchService;
  EventService eventService;

  @BeforeEach
  void setUp() {
    branchService = Mockito.mock(BranchService.class);
    eventService = Mockito.mock(EventService.class);
    rule = new SegmentationRule();
    rule.branchService = branchService;
    rule.eventService = eventService;
  }

  @Test
  void returnsQueueWhenRuleMatches() throws SystemException {
    Branch branch = new Branch("b1", "Branch");
    Queue queue = new Queue("q1", "Queue", "A", 1);
    branch.getQueues().put("q1", queue);

    ServiceGroup sg = new ServiceGroup("sg1", "Group", List.of("s1"), "b1");
    branch.getServiceGroups().put("sg1", sg);

    HashMap<String, String> visitProps = new HashMap<>();
    visitProps.put("city", "M");
    SegmentationRuleData data =
        SegmentationRuleData.builder()
            .serviceGroupId("sg1")
            .queueId("q1")
            .visitProperty(visitProps)
            .build();
    branch.getSegmentationRules().put("rule1", data);

    Service service = new Service("s1", "Service", 1, "q1");
    service.setServiceGroupId("sg1");

    HashMap<String, String> params = new HashMap<>();
    params.put("city", "M");
    Visit visit = Visit.builder().currentService(service).parameterMap(params).build();

    Optional<Queue> result = rule.getQueue(visit, branch);
    assertTrue(result.isPresent());
    assertEquals(queue, result.get());
  }

  @Test
  void throwsWhenSegmentationRuleMissing() {
    Branch branch = new Branch("b1", "Branch");
    Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

    Service service = new Service("s1", "Service", 1, "q1");
    Visit visit = Visit.builder().currentService(service).build();

    assertThrows(
        HttpStatusException.class,
        () -> rule.getQueue(visit, branch, "missing"));
  }

  @Test
  void returnsLinkedQueueWhenNoRules() throws SystemException {
    Branch branch = new Branch("b1", "Branch");
    Queue queue = new Queue("q1", "Queue", "A", 1);
    branch.getQueues().put("q1", queue);

    Service service = new Service("s1", "Service", 1, "q1");
    Visit visit = Visit.builder().currentService(service).build();

    Optional<Queue> result = rule.getQueue(visit, branch);
    assertTrue(result.isPresent());
    assertEquals(queue, result.get());
  }

  @Test
  void getQueueByIdExecutesGroovyRule() {
    Branch branch = new Branch("b1", "Branch");
    Queue queue = new Queue("q1", "Queue", "A", 1);
    branch.getQueues().put("q1", queue);

    SegmentationRuleData data =
        SegmentationRuleData.builder().id("rule1").queueId("q1").visitProperty(new HashMap<>()).build();
    branch.getSegmentationRules().put("rule1", data);

    HashMap<String, Object> ip = new HashMap<>();
    ip.put("visit", null);
    ip.put("branch", null);
    ru.aritmos.model.GroovyScript script =
        ru.aritmos.model.GroovyScript.builder()
            .inputParameters(ip)
            .ruleCode("import java.util.Optional; queue = Optional.of(branch.getQueues()['q1'])")
            .build();
    branch.getCustomSegmentationRules().put("rule1", script);

    Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

    Service service = new Service("s1", "Service", 1, "q1");
    Visit visit = Visit.builder().currentService(service).build();

    Optional<Queue> result = rule.getQueue(visit, branch, "rule1");
    assertTrue(result.isPresent());
    assertEquals(queue, result.get());
  }

  @Test
  void getQueueByIdReturnsEmptyWhenIdNullOrEmpty() {
    Branch branch = new Branch("b1", "Branch");
    Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

    Service service = new Service("s1", "Service", 1, "q1");
    Visit visit = Visit.builder().currentService(service).build();

    assertTrue(rule.getQueue(visit, branch, null).isEmpty());
    assertTrue(rule.getQueue(visit, branch, "").isEmpty());
  }

  @Test
  void getQueueByIdThrowsWhenInputParamsMissing() {
    Branch branch = new Branch("b1", "Branch");
    Queue queue = new Queue("q1", "Queue", "A", 1);
    branch.getQueues().put("q1", queue);

    SegmentationRuleData data =
        SegmentationRuleData.builder().id("rule1").queueId("q1").visitProperty(new HashMap<>()).build();
    branch.getSegmentationRules().put("rule1", data);

    ru.aritmos.model.GroovyScript script =
        ru.aritmos.model.GroovyScript.builder().inputParameters(new HashMap<>()).build();
    branch.getCustomSegmentationRules().put("rule1", script);

    Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

    Service service = new Service("s1", "Service", 1, "q1");
    Visit visit = Visit.builder().currentService(service).build();

    assertThrows(
        HttpStatusException.class,
        () -> rule.getQueue(visit, branch, "rule1"));
  }
}
