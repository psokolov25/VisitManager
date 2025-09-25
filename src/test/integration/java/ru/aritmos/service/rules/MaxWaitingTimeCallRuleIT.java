package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;

/**
 * Интеграционный тест для {@link MaxWaitingTimeCallRule}.
 */
@MicronautTest(environments = "integration")
class MaxWaitingTimeCallRuleIT {

    @Inject
    MaxWaitingTimeCallRule rule;

    /** Проверяет выбор визита с максимальным временем ожидания. */
    @DisplayName("Selects Visit With Max Waiting Time")
    @Test
    void selectsVisitWithMaxWaitingTime() {
        // отделение с рабочим профилем и очередью
        Branch branch = new Branch("b1", "branch");
        WorkProfile wp = new WorkProfile("wp1", "wp1");
        wp.getQueueIds().add("q1");
        branch.getWorkProfiles().put(wp.getId(), wp);

        // точка обслуживания с пользователем
        ServicePoint sp = new ServicePoint("sp1", "sp1");
        User user = new User("u1", "u1", null);
        user.setCurrentWorkProfileId("wp1");
        sp.setUser(user);
        branch.getServicePoints().put(sp.getId(), sp);

        // очередь с двумя визитами с разным временем ожидания
        Queue queue = new Queue("q1", "q", "A", 1);
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        Visit v1 = Visit.builder()
                .id("v1")
                .status("WAITING")
                .createDateTime(now.minusSeconds(5))
                .parameterMap(new HashMap<>())
                .build();
        Visit v2 = Visit.builder()
                .id("v2")
                .status("WAITING")
                .createDateTime(now.minusSeconds(10))
                .parameterMap(new HashMap<>())
                .build();
        queue.getVisits().add(v1);
        queue.getVisits().add(v2);
        branch.getQueues().put(queue.getId(), queue);

        Optional<Visit> result = rule.call(branch, sp);
        assertTrue(result.isPresent());
        assertEquals("v2", result.get().getId());
    }

    /** Проверяет, что без пользователя выбрасывается исключение. */
    @DisplayName("Call Without User Throws")
    @Test
    void callWithoutUserThrows() {
        Branch branch = new Branch("b1", "branch");
        ServicePoint sp = new ServicePoint("sp1", "sp1");

        io.micronaut.http.exceptions.HttpStatusException ex =
                assertThrows(io.micronaut.http.exceptions.HttpStatusException.class, () -> rule.call(branch, sp));
        assertEquals(io.micronaut.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
