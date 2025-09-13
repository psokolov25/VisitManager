package ru.aritmos.service.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;
import org.junit.jupiter.api.Disabled;

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
    @Disabled("Not yet implemented")
    @Test
    void callTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void getAvailiableServicePointsTest() {
        // TODO implement
    }

}
