package ru.aritmos.model.tiny;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class TinyServicePointTest {

    @DisplayName("проверяется сценарий «constructor should populate fields»")
    @Test
    void constructorShouldPopulateFields() {
        TinyServicePoint servicePoint = new TinyServicePoint("sp1", "Desk 1", true);

        assertEquals("sp1", servicePoint.getId());
        assertEquals("Desk 1", servicePoint.getName());
        assertTrue(servicePoint.getIsAvailable());
    }

    @DisplayName("проверяется сценарий «setters should update availability»")
    @Test
    void settersShouldUpdateAvailability() {
        TinyServicePoint servicePoint = new TinyServicePoint("sp2", "Desk 2", false);
        servicePoint.setIsAvailable(true);

        assertTrue(servicePoint.getIsAvailable());
    }
}