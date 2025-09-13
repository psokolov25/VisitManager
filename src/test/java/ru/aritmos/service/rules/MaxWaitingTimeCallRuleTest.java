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

