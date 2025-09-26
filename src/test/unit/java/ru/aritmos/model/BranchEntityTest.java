package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class BranchEntityTest {

    @DisplayName("Клонирование сущности копирует базовые поля")
    @Test
    void cloneShouldCopyBasicFields() {
        BranchEntity entity = new BranchEntity("id", "name");
        entity.setBranchId("b1");

        BranchEntity clone = entity.clone();

        assertNotSame(entity, clone);
        assertEquals("id", clone.getId());
        assertEquals("name", clone.getName());
        assertEquals("b1", clone.getBranchId());
    }
}
