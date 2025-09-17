package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.Branch;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.model.WorkProfile;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Юнит-тесты для {@link MaxLifeTimeCallRule} с подробным логированием шагов.
 */
@ExtendWith(TestLoggingExtension.class)
class MaxLifeTimeCallRuleTest {

    private static final Logger LOG = LoggerFactory.getLogger(MaxLifeTimeCallRuleTest.class);

    /** Проверяем выбор визита с максимальным временем жизни. */
    @Test
    void selectsVisitWithLongestLifeTime() {
        LOG.info("Шаг 1: формируем отделение с очередью и рабочим профилем");
        Branch branch = new Branch("b1", "branch");
        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        WorkProfile wp = new WorkProfile("wp", "wp");
        wp.getQueueIds().add(queue.getId());
        branch.getWorkProfiles().put(wp.getId(), wp);

        ServicePoint sp = new ServicePoint("sp", "sp");
        User user = new User();
        user.setId("u1");
        user.setCurrentWorkProfileId(wp.getId());
        sp.setUser(user);

        LOG.info("Шаг 2: подготавливаем три ожидающих визита с различной длительностью");
        ZonedDateTime now = ZonedDateTime.now();
        Visit v1 = Visit.builder()
                .id("v1")
                .status("WAITING")
                .returnDateTime(now.minusSeconds(3))
                .createDateTime(now.minusSeconds(20))
                .returnTimeDelay(0L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>())
                .build();
        Visit v2 = Visit.builder()
                .id("v2")
                .status("WAITING")
                .returnDateTime(now.minusSeconds(5))
                .createDateTime(now.minusSeconds(10))
                .returnTimeDelay(0L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>())
                .build();
        Visit v3 = Visit.builder()
                .id("v3")
                .status("WAITING")
                .returnDateTime(now.minusSeconds(5))
                .createDateTime(now.minusSeconds(30))
                .returnTimeDelay(0L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>())
                .build();
        queue.getVisits().addAll(List.of(v1, v2, v3));

        LOG.info("Шаг 3: вызываем правило и ожидаем визит с максимальной длительностью жизни");
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();
        Visit result = rule.call(branch, sp).orElseThrow();
        assertEquals("v3", result.getId());
    }

    /** Проверяем, что без пользователя выбрасывается ошибка. */
    @Test
    void callThrowsWhenNoUser() {
        LOG.info("Шаг 1: создаём правило и подменяем сервис событий");
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();
        EventService eventService = Mockito.mock(EventService.class);
        rule.eventService = eventService;

        Branch branch = new Branch("b1", "branch");
        ServicePoint sp = new ServicePoint("sp1", "sp1");

        LOG.info("Шаг 2: вызываем правило и ожидаем HTTP-исключение");
        HttpStatusException ex = assertThrows(HttpStatusException.class, () -> rule.call(branch, sp));
        LOG.info("Шаг 3: проверяем код ошибки");
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        Mockito.verify(eventService).send(Mockito.eq("*"), Mockito.eq(false), Mockito.any());
    }

    /** Проверяем, что при отсутствующем рабочем профиле результат пустой. */
    @Test
    void callReturnsEmptyWhenWorkProfileMissing() {
        LOG.info("Шаг 1: формируем отделение без нужного рабочего профиля");
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint sp = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("u1");
        user.setCurrentWorkProfileId("wp1");
        sp.setUser(user);

        LOG.info("Шаг 2: вызываем правило и убеждаемся, что визит не найден");
        assertTrue(rule.call(branch, sp).isEmpty());
    }

    /** Проверяем, что визиты с неподходящими таймерами игнорируются. */
    @Test
    void callReturnsEmptyWhenVisitsDoNotMeetTimingCriteria() {
        LOG.info("Шаг 1: формируем очередь с визитом, не успевшим набрать задержку");
        Branch branch = new Branch("b1", "branch");
        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        WorkProfile wp = new WorkProfile("wp1", "wp1");
        wp.getQueueIds().add(queue.getId());
        branch.getWorkProfiles().put(wp.getId(), wp);

        ServicePoint sp = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("u1");
        user.setCurrentWorkProfileId(wp.getId());
        sp.setUser(user);

        Visit notReady = Visit.builder()
                .id("v1")
                .status("WAITING")
                .returnDateTime(ZonedDateTime.now())
                .transferDateTime(ZonedDateTime.now())
                .returnTimeDelay(120L)
                .transferTimeDelay(120L)
                .parameterMap(new HashMap<>())
                .build();
        queue.getVisits().add(notReady);

        LOG.info("Шаг 2: вызываем правило и ожидаем пустой результат");
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();
        Optional<Visit> result = rule.call(branch, sp);
        assertTrue(result.isEmpty());
    }

    /** Проверяем вызов визита по списку очередей и очистку флагов. */
    @Test
    void callWithQueueIdsResetsFlags() {
        LOG.info("Шаг 1: формируем очередь с визитом и рабочий профиль");
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        WorkProfile wp = new WorkProfile("wp1", "wp1");
        wp.getQueueIds().add(queue.getId());
        branch.getWorkProfiles().put(wp.getId(), wp);

        ServicePoint sp = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("u1");
        user.setCurrentWorkProfileId(wp.getId());
        sp.setUser(user);

        ZonedDateTime now = ZonedDateTime.now().minusSeconds(10);
        Visit visit = Visit.builder()
                .id("v1")
                .status("WAITING")
                .returnDateTime(now)
                .transferDateTime(now)
                .returnTimeDelay(0L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>(Map.of("isTransferredToStart", "true")))
                .build();
        queue.getVisits().add(visit);

        LOG.info("Шаг 2: вызываем правило для конкретной очереди");
        Optional<Visit> result = rule.call(branch, sp, List.of(queue.getId()));
        LOG.info("Шаг 3: проверяем, что поля очищены");
        assertTrue(result.isPresent());
        assertNull(result.get().getReturnDateTime());
        assertNull(result.get().getTransferDateTime());
        assertFalse(result.get().getParameterMap().containsKey("isTransferredToStart"));
    }

    /** Проверяем, что пустые очереди пропускаются. */
    @Test
    void callWithQueueIdsSkipsEmptyQueues() {
        LOG.info("Шаг 1: формируем несколько пустых очередей для рабочего профиля");
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        Queue queue1 = new Queue("q1", "queue1", "A", 1);
        Queue queue2 = new Queue("q2", "queue2", "B", 1);
        branch.getQueues().put(queue1.getId(), queue1);
        branch.getQueues().put(queue2.getId(), queue2);

        WorkProfile wp = new WorkProfile("wp1", "wp1");
        wp.getQueueIds().addAll(List.of(queue1.getId(), queue2.getId()));
        branch.getWorkProfiles().put(wp.getId(), wp);

        ServicePoint sp = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("u1");
        user.setCurrentWorkProfileId(wp.getId());
        sp.setUser(user);

        LOG.info("Шаг 2: вызываем правило и убеждаемся, что визит не найден");
        Optional<Visit> result = rule.call(branch, sp, List.of(queue1.getId(), queue2.getId()));
        assertTrue(result.isEmpty());
    }

    /** Проверяем, что при отсутствии пользователя для выбора очереди выбрасывается ошибка. */
    @Test
    void callWithQueueIdsThrowsWhenNoUser() {
        LOG.info("Шаг 1: подготавливаем правило и мок сервиса событий");
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();
        EventService eventService = Mockito.mock(EventService.class);
        rule.eventService = eventService;

        Branch branch = new Branch("b1", "branch");
        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        LOG.info("Шаг 2: вызываем правило для точки без пользователя");
        HttpStatusException ex = assertThrows(
            HttpStatusException.class,
            () -> rule.call(branch, new ServicePoint("sp1", "sp1"), List.of(queue.getId()))
        );
        LOG.info("Шаг 3: проверяем возвращаемый статус");
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        Mockito.verify(eventService).send(Mockito.eq("*"), Mockito.eq(false), Mockito.any());
    }

    /** Проверяем фильтрацию доступных точек обслуживания по рабочему профилю. */
    @Test
    void availableServicePointsFilteredByWorkProfile() {
        LOG.info("Шаг 1: формируем два рабочих профиля и точки обслуживания");
        Branch branch = new Branch("b1", "branch");

        WorkProfile wp1 = new WorkProfile("wp1", "wp1");
        wp1.getQueueIds().add("q1");
        branch.getWorkProfiles().put(wp1.getId(), wp1);

        WorkProfile wp2 = new WorkProfile("wp2", "wp2");
        wp2.getQueueIds().add("q2");
        branch.getWorkProfiles().put(wp2.getId(), wp2);

        ServicePoint sp1 = new ServicePoint("sp1", "sp1");
        User u1 = new User();
        u1.setId("u1");
        u1.setName("u1");
        u1.setCurrentWorkProfileId("wp1");
        sp1.setUser(u1);
        branch.getServicePoints().put(sp1.getId(), sp1);

        ServicePoint sp2 = new ServicePoint("sp2", "sp2");
        User u2 = new User();
        u2.setId("u2");
        u2.setName("u2");
        u2.setCurrentWorkProfileId("wp2");
        sp2.setUser(u2);
        branch.getServicePoints().put(sp2.getId(), sp2);

        Service service = new Service("s1", "service", 60, "q1");
        Visit visit = Visit.builder().currentService(service).parameterMap(new HashMap<>()).build();

        LOG.info("Шаг 2: получаем доступные точки обслуживания для визита");
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();
        List<ServicePoint> result = rule.getAvailiableServicePoints(branch, visit);

        LOG.info("Шаг 3: убеждаемся, что подходит только одна точка");
        assertEquals(1, result.size());
        assertEquals("sp1", result.get(0).getId());
    }
}
