package ru.aritmos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BranchEntityWithVisitsTest {

    @Test
    void constructorWithNameShouldGenerateIdAndSetName() {
        BranchEntityWithVisits branch = new BranchEntityWithVisits("МФЦ");

        assertNotNull(branch.getId());
        assertDoesNotThrow(() -> UUID.fromString(branch.getId()));
        assertEquals("МФЦ", branch.getName());
        assertTrue(branch.getVisits().isEmpty());
    }

    @Test
    void constructorWithIdAndNameShouldUseProvidedValues() {
        String id = UUID.randomUUID().toString();
        BranchEntityWithVisits branch = new BranchEntityWithVisits(id, "Сбербанк");

        assertEquals(id, branch.getId());
        assertEquals("Сбербанк", branch.getName());
        assertTrue(branch.getVisits().isEmpty());
    }
}
