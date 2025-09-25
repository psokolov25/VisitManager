package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class BranchEntityWithVisitsTest {

    @DisplayName("проверяется сценарий «constructor with name should generate id and set name»")
    @Test
    void constructorWithNameShouldGenerateIdAndSetName() {
        BranchEntityWithVisits branch = new BranchEntityWithVisits("МФЦ");

        assertNotNull(branch.getId());
        assertDoesNotThrow(() -> UUID.fromString(branch.getId()));
        assertEquals("МФЦ", branch.getName());
        assertTrue(branch.getVisits().isEmpty());
    }

    @DisplayName("проверяется сценарий «constructor with id and name should use provided values»")
    @Test
    void constructorWithIdAndNameShouldUseProvidedValues() {
        String id = UUID.randomUUID().toString();
        BranchEntityWithVisits branch = new BranchEntityWithVisits(id, "Сбербанк");

        assertEquals(id, branch.getId());
        assertEquals("Сбербанк", branch.getName());
        assertTrue(branch.getVisits().isEmpty());
    }
}