package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Method;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.*;
import ru.aritmos.model.visit.Visit;

/**
 * Юнит-тесты для {@link MaxWaitingTimeCallRule}.
 */
class MaxWaitingTimeCallRuleTest {

    /** Проверяем корректный парсинг даты из строки. */
    @DisplayName("Метод `getDateNyString` корректно парсит дату из строки")
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
    @DisplayName("Метод `getAvailiableServicePoints` фильтрует точки по рабочему профилю")
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
    @DisplayName("Метод `call` выбрасывает исключение при неверном рабочем профиле")
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
    @DisplayName("Метод `call` с очередями выбирает визит с максимальным ожиданием")
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
    @DisplayName("Метод `call` с очередями выбрасывает исключение без оператора")
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
    @DisplayName("Метод `visitComparer` учитывает флаги переноса визита")
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

    /** Проверяем выбор визита с максимальным временем ожидания без списка очередей. */
    @DisplayName("Метод `call` выбирает визит с наибольшим временем ожидания")
    @Test
    void callSelectsVisitWithLongestWaitingTime() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        WorkProfile profile = new WorkProfile("wp1", "wp1");
        profile.getQueueIds().add("q1");
        branch.getWorkProfiles().put(profile.getId(), profile);

        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint servicePoint = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("user");
        user.setCurrentWorkProfileId(profile.getId());
        servicePoint.setUser(user);

        ZonedDateTime now = ZonedDateTime.now();
        Visit shorter = Visit.builder()
                .id("shorter")
                .status("WAITING")
                .createDateTime(now.minusSeconds(10))
                .parameterMap(new HashMap<>())
                .build();
        Visit longer = Visit.builder()
                .id("longer")
                .status("WAITING")
                .createDateTime(now.minusSeconds(120))
                .parameterMap(new HashMap<>())
                .build();
        queue.getVisits().addAll(List.of(shorter, longer));

        Optional<Visit> result = rule.call(branch, servicePoint);

        assertTrue(result.isPresent());
        assertEquals("longer", result.get().getId());
    }

    /** Проверяем очистку флагов переноса/возврата после выбора визита. */
    @DisplayName("Метод `call` сбрасывает временные флаги переноса у выбранного визита")
    @Test
    void callResetsTransferFlagsOnSelectedVisit() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        WorkProfile profile = new WorkProfile("wp1", "wp1");
        profile.getQueueIds().add("q1");
        branch.getWorkProfiles().put(profile.getId(), profile);

        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint servicePoint = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("user");
        user.setCurrentWorkProfileId(profile.getId());
        servicePoint.setUser(user);

        ZonedDateTime flagTime = ZonedDateTime.now().minusMinutes(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        Visit visit = Visit.builder()
                .id("flagged")
                .status("WAITING")
                .returnDateTime(flagTime)
                .transferDateTime(flagTime)
                .returnTimeDelay(0L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>(Map.of("isTransferredToStart", formatter.format(flagTime))))
                .build();
        queue.getVisits().add(visit);

        Optional<Visit> result = rule.call(branch, servicePoint);

        assertTrue(result.isPresent());
        assertNull(result.get().getReturnDateTime());
        assertNull(result.get().getTransferDateTime());
        assertFalse(result.get().getParameterMap().containsKey("isTransferredToStart"));
    }

    /** Проверяем, что при отсутствии пользователя выбрасывается исключение. */
    @DisplayName("Метод `call` выбрасывает исключение при отсутствии оператора")
    @Test
    void callThrowsWhenNoUserAssigned() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();
        rule.eventService = org.mockito.Mockito.mock(ru.aritmos.events.services.EventService.class);

        Branch branch = new Branch("b1", "branch");
        ServicePoint servicePoint = new ServicePoint("sp1", "sp1");

        io.micronaut.http.exceptions.HttpStatusException ex =
                assertThrows(io.micronaut.http.exceptions.HttpStatusException.class, () -> rule.call(branch, servicePoint));
        assertEquals(io.micronaut.http.HttpStatus.FORBIDDEN, ex.getStatus());
        org.mockito.Mockito.verify(rule.eventService)
                .send(org.mockito.Mockito.eq("*"), org.mockito.Mockito.eq(false), org.mockito.Mockito.any());
    }

    /** Проверяем, что компаратор учитывает порядок по датам переноса. */
    @DisplayName("Метод `visitComparer` отдаёт приоритет более ранней дате переноса")
    @Test
    void visitComparerPrefersEarlierTransferDates() throws Exception {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();
        Method comparer = MaxWaitingTimeCallRule.class.getDeclaredMethod("visitComparer", Visit.class, Visit.class);
        comparer.setAccessible(true);

        Visit earlier = Visit.builder()
                .parameterMap(new HashMap<>(Map.of("isTransferredToStart", "Mon, 01 Jan 2024 10:00:00 GMT")))
                .waitingTime(5L)
                .build();
        Visit later = Visit.builder()
                .parameterMap(new HashMap<>(Map.of("isTransferredToStart", "Mon, 01 Jan 2024 10:05:00 GMT")))
                .waitingTime(100L)
                .build();

        int compareForward = (int) comparer.invoke(rule, earlier, later);
        int compareBackward = (int) comparer.invoke(rule, later, earlier);

        assertTrue(compareForward > 0);
        assertTrue(compareBackward < 0);
    }

    /** Проверяем, что визиты без статуса WAITING игнорируются. */
    @DisplayName("Метод `call` игнорирует визиты без статуса ожидания")
    @Test
    void callIgnoresVisitsWithoutWaitingStatus() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        WorkProfile profile = new WorkProfile("wp1", "wp1");
        profile.getQueueIds().add("q1");
        branch.getWorkProfiles().put(profile.getId(), profile);

        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint servicePoint = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("user");
        user.setCurrentWorkProfileId(profile.getId());
        servicePoint.setUser(user);

        Visit served = Visit.builder()
                .id("served")
                .status("SERVED")
                .parameterMap(new HashMap<>())
                .build();
        queue.getVisits().add(served);

        Optional<Visit> result = rule.call(branch, servicePoint);
        assertTrue(result.isEmpty());
    }

    /** Проверяем, что визиты с ненабранной задержкой возвращения пропускаются. */
    @DisplayName("Метод `call` пропускает визиты до истечения задержки возврата")
    @Test
    void callSkipsVisitsUntilReturnDelayReached() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        WorkProfile profile = new WorkProfile("wp1", "wp1");
        profile.getQueueIds().add("q1");
        branch.getWorkProfiles().put(profile.getId(), profile);

        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint servicePoint = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("user");
        user.setCurrentWorkProfileId(profile.getId());
        servicePoint.setUser(user);

        Visit notReady = Visit.builder()
                .id("notReady")
                .status("WAITING")
                .returnDateTime(ZonedDateTime.now())
                .returnTimeDelay(120L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>())
                .build();
        Visit ready = Visit.builder()
                .id("ready")
                .status("WAITING")
                .createDateTime(ZonedDateTime.now().minusSeconds(30))
                .parameterMap(new HashMap<>())
                .returnTimeDelay(0L)
                .transferTimeDelay(0L)
                .build();
        queue.getVisits().add(notReady);
        queue.getVisits().add(ready);

        Optional<Visit> result = rule.call(branch, servicePoint);
        assertTrue(result.isPresent());
        assertEquals("ready", result.get().getId());
    }

    /** Проверяем очистку временных полей при выборе визита через список очередей. */
    @DisplayName("Метод `call` с очередями сбрасывает временные флаги переноса")
    @Test
    void callWithQueueIdsResetsTemporaryFlags() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        WorkProfile profile = new WorkProfile("wp1", "wp1");
        profile.getQueueIds().add("q1");
        branch.getWorkProfiles().put(profile.getId(), profile);

        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint servicePoint = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("user");
        user.setCurrentWorkProfileId(profile.getId());
        servicePoint.setUser(user);

        ZonedDateTime flagTime = ZonedDateTime.now().minusMinutes(2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        Visit visit = Visit.builder()
                .id("flagged")
                .status("WAITING")
                .returnDateTime(flagTime)
                .transferDateTime(flagTime)
                .returnTimeDelay(0L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>(Map.of("isTransferredToStart", formatter.format(flagTime))))
                .build();
        queue.getVisits().add(visit);

        Optional<Visit> result = rule.call(branch, servicePoint, List.of(queue.getId()));
        assertTrue(result.isPresent());
        assertNull(result.get().getReturnDateTime());
        assertNull(result.get().getTransferDateTime());
        assertFalse(result.get().getParameterMap().containsKey("isTransferredToStart"));
    }

    /** Проверяем, что при выборе по списку очередей визиты с задержкой возвращения не подходят. */
    @DisplayName("Метод `call` с очередями возвращает пустой результат до истечения задержки возврата")
    @Test
    void callWithQueueIdsReturnsEmptyWhenReturnDelayNotMet() {
        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();

        Branch branch = new Branch("b1", "branch");
        WorkProfile profile = new WorkProfile("wp1", "wp1");
        profile.getQueueIds().add("q1");
        branch.getWorkProfiles().put(profile.getId(), profile);

        Queue queue = new Queue("q1", "queue", "A", 1);
        branch.getQueues().put(queue.getId(), queue);

        ServicePoint servicePoint = new ServicePoint("sp1", "sp1");
        User user = new User();
        user.setId("user");
        user.setCurrentWorkProfileId(profile.getId());
        servicePoint.setUser(user);

        Visit notReady = Visit.builder()
                .id("notReady")
                .status("WAITING")
                .returnDateTime(ZonedDateTime.now())
                .returnTimeDelay(300L)
                .transferTimeDelay(0L)
                .parameterMap(new HashMap<>())
                .build();
        queue.getVisits().add(notReady);

        Optional<Visit> result = rule.call(branch, servicePoint, List.of(queue.getId()));
        assertTrue(result.isEmpty());
    }

    /** Проверяем, что без подходящих пользователей список точек обслуживания пуст. */
    @DisplayName("Метод `getAvailiableServicePoints` возвращает пустой список без активных операторов")
    @Test
    void availableServicePointsReturnEmptyWhenNoActiveUsers() {
        Branch branch = new Branch("b1", "branch");

        WorkProfile profile = new WorkProfile("wp1", "wp1");
        profile.getQueueIds().add("q1");
        branch.getWorkProfiles().put(profile.getId(), profile);

        ServicePoint withoutUser = new ServicePoint("sp1", "sp1");
        branch.getServicePoints().put(withoutUser.getId(), withoutUser);

        ServicePoint wrongProfile = new ServicePoint("sp2", "sp2");
        User wrongUser = new User();
        wrongUser.setId("u2");
        wrongUser.setName("u2");
        wrongUser.setCurrentWorkProfileId("other");
        wrongProfile.setUser(wrongUser);
        branch.getServicePoints().put(wrongProfile.getId(), wrongProfile);

        Visit visit = Visit.builder()
                .currentService(new Service("s1", "service", 60, "q1"))
                .parameterMap(new HashMap<>())
                .build();

        MaxWaitingTimeCallRule rule = new MaxWaitingTimeCallRule();
        List<ServicePoint> result = rule.getAvailiableServicePoints(branch, visit);
        assertTrue(result.isEmpty());
    }
}

