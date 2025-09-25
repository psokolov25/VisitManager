package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BranchEntityWithVisitsTest {

    @DisplayName("Конструктор только с именем генерирует идентификатор и устанавливает название")
    @Test
    void constructorWithNameShouldGenerateIdAndSetName() {
        BranchEntityWithVisits branch = new BranchEntityWithVisits("МФЦ");

        assertNotNull(branch.getId());
        assertDoesNotThrow(() -> UUID.fromString(branch.getId()));
        assertEquals("МФЦ", branch.getName());
        assertTrue(branch.getVisits().isEmpty());
    }

    @DisplayName("Конструктор с идентификатором и именем использует переданные значения")
    @Test
    void constructorWithIdAndNameShouldUseProvidedValues() {
        String id = UUID.randomUUID().toString();
        BranchEntityWithVisits branch = new BranchEntityWithVisits(id, "Сбербанк");

        assertEquals(id, branch.getId());
        assertEquals("Сбербанк", branch.getName());
        assertTrue(branch.getVisits().isEmpty());
    }
}
