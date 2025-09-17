package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;

/**
 * Юнит-тесты для {@link MaxLifeTimeCallRule}.
 */
class MaxLifeTimeCallRuleTest {

    /** Проверяем выбор визита с максимальным временем жизни. */
    @Test
    void selectsVisitWithLongestLifeTime() {
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

        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();
        Visit result = rule.call(branch, sp).orElseThrow();
        assertEquals("v3", result.getId());
    }

    /** Проверяем, что без пользователя выбрасывается ошибка. */
    @Test
    void callThrowsWhenNoUser() {
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();
        rule.eventService = org.mockito.Mockito.mock(ru.aritmos.events.services.EventService.class);

        Branch branch = new Branch("b1", "branch");
        ServicePoint sp = new ServicePoint("sp1", "sp1");

        io.micronaut.http.exceptions.HttpStatusException ex =
                assertThrows(io.micronaut.http.exceptions.HttpStatusException.class, () -> rule.call(branch, sp));
        assertEquals(io.micronaut.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /** Проверяем, что при отсутствующем рабочем профиле результат пустой. */
    @Test
    void callReturnsEmptyWhenWorkProfileMissing() {
        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint sp = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("u1");
        user.setCurrentWorkProfileId("wp1");
        sp.setUser(user);

        assertTrue(rule.call(branch, sp).isEmpty());
    }

    /** Проверяем вызов визита по списку очередей и очистку флагов. */
    @Test
    void callWithQueueIdsResetsFlags() {
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

        Optional<Visit> result = rule.call(branch, sp, List.of(queue.getId()));
        assertTrue(result.isPresent());
        assertNull(result.get().getReturnDateTime());
        assertNull(result.get().getTransferDateTime());
        assertFalse(result.get().getParameterMap().containsKey("isTransferredToStart"));
    }

    /** Проверяем фильтрацию доступных точек обслуживания по рабочему профилю. */
    @Test
    void availableServicePointsFilteredByWorkProfile() {
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

        MaxLifeTimeCallRule rule = new MaxLifeTimeCallRule();
        List<ServicePoint> result = rule.getAvailiableServicePoints(branch, visit);

        assertEquals(1, result.size());
        assertEquals("sp1", result.get(0).getId());
    }
}
