package ru.aritmos.model.tiny;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class TinyServicePointTest {

    @DisplayName("Конструктор заполняет поля")
    @Test
    void constructorShouldPopulateFields() {
        TinyServicePoint servicePoint = new TinyServicePoint("sp1", "Desk 1", true);

        assertEquals("sp1", servicePoint.getId());
        assertEquals("Desk 1", servicePoint.getName());
        assertTrue(servicePoint.getIsAvailable());
    }

    @DisplayName("Сеттеры обновляют признак доступности")
    @Test
    void settersShouldUpdateAvailability() {
        TinyServicePoint servicePoint = new TinyServicePoint("sp2", "Desk 2", false);
        servicePoint.setIsAvailable(true);

        assertTrue(servicePoint.getIsAvailable());
    }
}
