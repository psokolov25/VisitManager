package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BranchEntityWithVisitsTest {

    @DisplayName("Constructor With Name Should Generate Id And Set Name")
    @Test
    void constructorWithNameShouldGenerateIdAndSetName() {
        BranchEntityWithVisits branch = new BranchEntityWithVisits("МФЦ");

        assertNotNull(branch.getId());
        assertDoesNotThrow(() -> UUID.fromString(branch.getId()));
        assertEquals("МФЦ", branch.getName());
        assertTrue(branch.getVisits().isEmpty());
    }

    @DisplayName("Constructor With Id And Name Should Use Provided Values")
    @Test
    void constructorWithIdAndNameShouldUseProvidedValues() {
        String id = UUID.randomUUID().toString();
        BranchEntityWithVisits branch = new BranchEntityWithVisits(id, "Сбербанк");

        assertEquals(id, branch.getId());
        assertEquals("Сбербанк", branch.getName());
        assertTrue(branch.getVisits().isEmpty());
    }
}
