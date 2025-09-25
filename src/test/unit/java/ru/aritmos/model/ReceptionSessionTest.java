package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ReceptionSessionTest {

    @DisplayName("Билдер заполняет все поля")
    @Test
    void builderShouldPopulateAllFields() {
        User user = new User();
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime end = start.plusHours(1);

        ReceptionSession session =
                ReceptionSession.builder().user(user).startTime(start).endTime(end).build();

        assertSame(user, session.getUser());
        assertEquals(start, session.getStartTime());
        assertEquals(end, session.getEndTime());
    }
}
