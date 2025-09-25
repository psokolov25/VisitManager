package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicePointTest {

    @DisplayName("Конструктор с идентификатором и именем задаёт поля и значения по умолчанию")
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

    @DisplayName("Конструктор только с именем генерирует идентификатор и значения по умолчанию")
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
