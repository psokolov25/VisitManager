package ru.aritmos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link User}. */
class UserTest {

    @Test
    void isOnBreakChecksStartAndEnd() {
        User user = new User();
        user.setLastBreakStartTime(ZonedDateTime.now().minusMinutes(1));
        assertTrue(user.isOnBreak());
        user.setLastBreakEndTime(ZonedDateTime.now());
        assertFalse(user.isOnBreak());
    }

    @Test
    void getLastBreakDurationCalculatesSeconds() {
        User user = new User();
        ZonedDateTime start = ZonedDateTime.now().minusSeconds(10);
        user.setLastBreakStartTime(start);
        user.setLastBreakEndTime(start.plusSeconds(5));
        assertEquals(5L, user.getLastBreakDuration());
    }
}
