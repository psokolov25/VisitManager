package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicePointTest {

    @DisplayName("Constructor With Id And Name Should Set Fields And Defaults")
    @Test
    void constructorWithIdAndNameShouldSetFieldsAndDefaults() {
        String id = UUID.randomUUID().toString();
        ServicePoint servicePoint = new ServicePoint(id, "Окно 1");

        assertEquals(id, servicePoint.getId());
        assertEquals("Окно 1", servicePoint.getName());
        assertFalse(servicePoint.getAutoCallMode());
        assertFalse(servicePoint.getIsConfirmRequired());
        assertNull(servicePoint.getVisit());
        assertNull(servicePoint.getUser());
        assertTrue(servicePoint.getVisits().isEmpty());
    }

    @DisplayName("Constructor With Name Should Generate Id And Set Defaults")
    @Test
    void constructorWithNameShouldGenerateIdAndSetDefaults() {
        ServicePoint servicePoint = new ServicePoint("Окно 2");

        assertNotNull(servicePoint.getId());
        assertDoesNotThrow(() -> UUID.fromString(servicePoint.getId()));
        assertEquals("Окно 2", servicePoint.getName());
        assertFalse(servicePoint.getAutoCallMode());
        assertFalse(servicePoint.getIsConfirmRequired());
        assertNull(servicePoint.getVisit());
        assertNull(servicePoint.getUser());
        assertTrue(servicePoint.getVisits().isEmpty());
    }
}
