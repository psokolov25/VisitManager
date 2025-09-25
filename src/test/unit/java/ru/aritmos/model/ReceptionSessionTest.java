package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class ReceptionSessionTest {

    @DisplayName("проверяется сценарий «builder should populate all fields»")
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