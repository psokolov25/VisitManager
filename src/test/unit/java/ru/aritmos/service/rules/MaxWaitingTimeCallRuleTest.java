package ru.aritmos.service.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;

/**
 * Юнит-тесты для {@link MaxWaitingTimeCallRule}.
 */
class MaxWaitingTimeCallRuleTest {

    /** Проверяем корректный парсинг даты из строки. */
    @Test
    void parseDateFromString() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();
        ZonedDateTime expected = ZonedDateTime.parse("2024-01-01T10:15:30+03:00[Europe/Moscow]");
        String formatted = expected.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US));

        ZonedDateTime actual = rule.getDateNyString(formatted);
        assertEquals(expected, actual);
    }

    /**
     * Проверяем отбор доступных точек обслуживания по рабочему профилю.
     */
    @Test
    void availableServicePointsFilteredByWorkProfile() {
        // создаем отделение
        Branch branch = new Branch("b1", "branch");

        // рабочий профиль, связанный с очередью q1
        WorkProfile wp1 = new WorkProfile("wp1", "wp1");
        wp1.getQueueIds().add("q1");
        branch.getWorkProfiles().put(wp1.getId(), wp1);

        // второй профиль с другой очередью
        WorkProfile wp2 = new WorkProfile("wp2", "wp2");
        wp2.getQueueIds().add("q2");
        branch.getWorkProfiles().put(wp2.getId(), wp2);

        // точка обслуживания с пользователем wp1
        ServicePoint sp1 = new ServicePoint("sp1", "sp1");
        User u1 = new User();
        u1.setId("u1");
        u1.setName("u1");
        u1.setCurrentWorkProfileId("wp1");
        sp1.setUser(u1);
        branch.getServicePoints().put(sp1.getId(), sp1);

        // точка обслуживания с пользователем wp2
        ServicePoint sp2 = new ServicePoint("sp2", "sp2");
        User u2 = new User();
        u2.setId("u2");
        u2.setName("u2");
        u2.setCurrentWorkProfileId("wp2");
        sp2.setUser(u2);
        branch.getServicePoints().put(sp2.getId(), sp2);

        // визит с услугой, привязанной к очереди q1
        Service service = new Service("s1", "service", 60, "q1");
        Visit visit = Visit.builder()
                .currentService(service)
                .parameterMap(new HashMap<>())
                .build();

        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();
        List<ServicePoint> result = rule.getAvailiableServicePoints(branch, visit);

        assertEquals(1, result.size());
        assertEquals("sp1", result.get(0).getId());
    }

    /**
     * Проверяем, что при неверном рабочем профиле выбрасывается исключение.
     */
    @Test
    void callThrowsWhenWorkProfileIsWrong() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();
        rule.eventService = org.mockito.Mockito.mock(ru.aritmos.events.services.EventService.class);

        Branch branch = new Branch("b1", "branch");
        ServicePoint sp = new ServicePoint("sp1", "sp1");
        User u1 = new User();
        u1.setId("u1");
        u1.setName("u1");
        u1.setCurrentWorkProfileId("wp1");
        sp.setUser(u1);

        io.micronaut.http.exceptions.HttpStatusException ex =
                assertThrows(io.micronaut.http.exceptions.HttpStatusException.class, () -> rule.call(branch, sp));
        assertEquals(io.micronaut.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /** Проверяем вызов визита по максимальному ожиданию из списка очередей. */
    @Test
    void callWithQueueIdsSelectsVisit() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        WorkProfile wp = new WorkProfile("wp1", "wp1");
        wp.getQueueIds().add("q1");
        branch.getWorkProfiles().put(wp.getId(), wp);

        ServicePoint sp = new ServicePoint("sp1", "sp1");
        User user = new User("u1", "u1", null);
        user.setCurrentWorkProfileId("wp1");
        sp.setUser(user);

        Queue queue = new Queue("q1", "q", "A", 1);
        Visit v1 = Visit.builder()
                .id("v1")
                .status("WAITING")
                .createDateTime(java.time.ZonedDateTime.now().minusSeconds(5))
                .parameterMap(new HashMap<>())
                .build();
        Visit v2 = Visit.builder()
                .id("v2")
                .status("WAITING")
                .createDateTime(java.time.ZonedDateTime.now().minusSeconds(10))
                .parameterMap(new HashMap<>())
                .build();
        queue.getVisits().add(v1);
        queue.getVisits().add(v2);
        branch.getQueues().put(queue.getId(), queue);

        Optional<Visit> result = rule.call(branch, sp, List.of("q1"));
        assertTrue(result.isPresent());
        assertEquals("v2", result.get().getId());
    }

    /** Проверяем, что без пользователя выбрасывается исключение при вызове с очередями. */
    @Test
    void callWithQueueIdsWithoutUserThrows() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();
        rule.eventService = org.mockito.Mockito.mock(ru.aritmos.events.services.EventService.class);

        Branch branch = new Branch("b1", "branch");
        ServicePoint sp = new ServicePoint("sp1", "sp1");

        io.micronaut.http.exceptions.HttpStatusException ex =
                assertThrows(io.micronaut.http.exceptions.HttpStatusException.class, () -> rule.call(branch, sp, List.of("q1")));
        assertEquals(io.micronaut.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    /** Проверяем работу компаратора перенесённых визитов. */
    @Test
    void visitComparerHandlesTransferFlags() throws Exception {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();
        Method comparer = MaxWaitingTimeCallRule.class.getDeclaredMethod("visitComparer", Visit.class, Visit.class);
        comparer.setAccessible(true);

        Visit transferred = Visit.builder()
                .parameterMap(new HashMap<>(Map.of("isTransferredToStart", "Mon, 01 Jan 2024 10:00:00 GMT")))
                .waitingTime(1L)
                .build();
        Visit regular = Visit.builder()
                .parameterMap(new HashMap<>())
                .waitingTime(10L)
                .build();

        int result1 = (int) comparer.invoke(rule, transferred, regular);
        int result2 = (int) comparer.invoke(rule, regular, transferred);

        assertTrue(result1 > 0);
        assertTrue(result2 < 0);
    }
}

