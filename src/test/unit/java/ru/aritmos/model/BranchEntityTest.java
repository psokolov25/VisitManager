package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class BranchEntityTest {

    @DisplayName("проверяется сценарий «clone should copy basic fields»")
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