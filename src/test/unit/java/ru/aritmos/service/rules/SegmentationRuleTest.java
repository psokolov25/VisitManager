package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.exceptions.HttpStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.model.Branch;
import ru.aritmos.model.GroovyScript;
import ru.aritmos.model.Queue;
import ru.aritmos.model.SegmentationRuleData;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServiceGroup;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.BranchService;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Набор тестов для {@link SegmentationRule} с пошаговым логированием сценариев.
 */
@ExtendWith(TestLoggingExtension.class)
class SegmentationRuleTest {

    private static final Logger LOG = LoggerFactory.getLogger(SegmentationRuleTest.class);

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

    /** Проверяем выбор очереди при совпадении правил сегментации. */
    @Test
    void returnsQueueWhenRuleMatches() throws SystemException {
        LOG.info("Шаг 1: создаём отделение с очередью и данными сегментации");
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 1);
        branch.getQueues().put("q1", queue);

        ServiceGroup sg = new ServiceGroup("sg1", "Group", List.of("s1"), null);
        branch.getServiceGroups().put("sg1", sg);

        HashMap<String, String> visitProps = new HashMap<>();
        visitProps.put("city", "M");
        SegmentationRuleData data = SegmentationRuleData.builder()
                .serviceGroupId("sg1")
                .queueId("q1")
                .visitProperty(visitProps)
                .build();
        branch.getSegmentationRules().put("rule1", data);

        LOG.info("Шаг 2: формируем визит с подходящим параметром");
        Service service = new Service("s1", "Service", 1, "q1");
        service.setServiceGroupId("sg1");
        HashMap<String, String> params = new HashMap<>();
        params.put("city", "M");
        Visit visit = Visit.builder().currentService(service).parameterMap(params).build();

        LOG.info("Шаг 3: вызываем правило и убеждаемся, что найдена нужная очередь");
        Optional<Queue> result = rule.getQueue(visit, branch);
        assertTrue(result.isPresent());
        assertEquals(queue, result.get());
    }

    /** Проверяем обработку отсутствующего правила сегментации. */
    @Test
    void throwsWhenSegmentationRuleMissing() {
        LOG.info("Шаг 1: настраиваем мок сервиса отделений и визит");
        Branch branch = new Branch("b1", "Branch");
        Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

        Service service = new Service("s1", "Service", 1, "q1");
        Visit visit = Visit.builder().currentService(service).build();

        LOG.info("Шаг 2: вызываем правило и ожидаем BusinessException");
        assertThrows(HttpStatusException.class, () -> rule.getQueue(visit, branch, "missing"));
    }

    /** Проверяем возврат привязанной очереди, если правило не определено. */
    @Test
    void returnsLinkedQueueWhenNoRules() throws SystemException {
        LOG.info("Шаг 1: формируем отделение с единственной очередью");
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 1);
        branch.getQueues().put("q1", queue);

        Service service = new Service("s1", "Service", 1, "q1");
        Visit visit = Visit.builder().currentService(service).build();

        LOG.info("Шаг 2: убеждаемся, что возвращается очередь из услуги");
        Optional<Queue> result = rule.getQueue(visit, branch);
        assertTrue(result.isPresent());
        assertEquals(queue, result.get());
    }

    /** Проверяем выполнение groovy-правила. */
    @Test
    void getQueueByIdExecutesGroovyRule() {
        LOG.info("Шаг 1: настраиваем groovy-скрипт для возврата очереди");
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 1);
        branch.getQueues().put("q1", queue);

        SegmentationRuleData data = SegmentationRuleData.builder()
                .id("rule1")
                .queueId("q1")
                .visitProperty(new HashMap<>())
                .build();
        branch.getSegmentationRules().put("rule1", data);

        HashMap<String, Object> ip = new HashMap<>();
        ip.put("visit", null);
        ip.put("branch", null);
        GroovyScript script = GroovyScript.builder()
                .inputParameters(ip)
                .ruleCode("import java.util.Optional; queue = Optional.of(branch.getQueues()['q1'])")
                .build();
        branch.getCustomSegmentationRules().put("rule1", script);

        Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

        Service service = new Service("s1", "Service", 1, "q1");
        Visit visit = Visit.builder().currentService(service).build();

        LOG.info("Шаг 2: выполняем правило и проверяем результат");
        Optional<Queue> result = rule.getQueue(visit, branch, "rule1");
        assertTrue(result.isPresent());
        assertEquals(queue, result.get());
    }

    /** Проверяем пустой идентификатор groovy-правила. */
    @Test
    void getQueueByIdReturnsEmptyWhenIdNullOrEmpty() {
        LOG.info("Шаг 1: подготавливаем визит без идентификатора правила");
        Branch branch = new Branch("b1", "Branch");
        Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

        Service service = new Service("s1", "Service", 1, "q1");
        Visit visit = Visit.builder().currentService(service).build();

        LOG.info("Шаг 2: вызываем правило с пустыми значениями идентификатора");
        assertTrue(rule.getQueue(visit, branch, null).isEmpty());
        assertTrue(rule.getQueue(visit, branch, "").isEmpty());
    }

    /** Проверяем выброс ошибки при отсутствии входных параметров для groovy. */
    @Test
    void getQueueByIdThrowsWhenInputParamsMissing() {
        LOG.info("Шаг 1: формируем groovy-правило без необходимых параметров");
        Branch branch = new Branch("b1", "Branch");
        Queue queue = new Queue("q1", "Queue", "A", 1);
        branch.getQueues().put("q1", queue);

        SegmentationRuleData data = SegmentationRuleData.builder()
                .id("rule1")
                .queueId("q1")
                .visitProperty(new HashMap<>())
                .build();
        branch.getSegmentationRules().put("rule1", data);

        GroovyScript script = GroovyScript.builder().inputParameters(new HashMap<>()).build();
        branch.getCustomSegmentationRules().put("rule1", script);

        Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

        Service service = new Service("s1", "Service", 1, "q1");
        Visit visit = Visit.builder().currentService(service).build();

        LOG.info("Шаг 2: ожидаем BusinessException из-за отсутствия параметров");
        assertThrows(HttpStatusException.class, () -> rule.getQueue(visit, branch, "rule1"));
    }

    /** Проверяем, что ошибки внутренних вызовов оборачиваются в {@link SystemException}. */
    @Test
    void getQueueWrapsBusinessExceptionIntoSystemException() {
        LOG.info("Шаг 1: готовим отделение с сервисной группой и отсутствующим правилом");
        Branch branch = new Branch("b1", "Branch");
        ServiceGroup serviceGroup = new ServiceGroup("sg1", "Group", List.of("s1"), null);
        serviceGroup.setSegmentationRuleId("ruleX");
        branch.getServiceGroups().put("sg1", serviceGroup);

        Service service = new Service("s1", "Service", 1, "q1");
        service.setServiceGroupId("sg1");
        Visit visit = Visit.builder().currentService(service).parameterMap(new HashMap<>()).build();

        Mockito.when(branchService.getBranch("b1")).thenReturn(branch);

        LOG.info("Шаг 2: вызываем метод и убеждаемся, что исключение обёрнуто");
        assertThrows(SystemException.class, () -> rule.getQueue(visit, branch));
    }

    /** Проверяем возврат пустого результата, когда очередь не найдена. */
    @Test
    void returnsEmptyWhenLinkedQueueMissing() throws SystemException {
        LOG.info("Шаг 1: формируем сервисную группу без связанной очереди");
        Branch branch = new Branch("b1", "Branch");
        ServiceGroup serviceGroup = new ServiceGroup("sg1", "Group", List.of("s1"), null);
        branch.getServiceGroups().put("sg1", serviceGroup);

        HashMap<String, String> visitProps = new HashMap<>();
        visitProps.put("city", "M");
        SegmentationRuleData data = SegmentationRuleData.builder()
                .serviceGroupId("sg1")
                .queueId("q1")
                .visitProperty(visitProps)
                .build();
        branch.getSegmentationRules().put("rule1", data);

        Service service = new Service("s1", "Service", 1, "q1");
        service.setServiceGroupId("sg1");
        Visit visit = Visit.builder().currentService(service).parameterMap(new HashMap<>()).build();

        LOG.info("Шаг 2: вызываем метод и убеждаемся, что очередь не найдена");
        Optional<Queue> result = rule.getQueue(visit, branch);
        assertTrue(result.isEmpty());
    }
}
