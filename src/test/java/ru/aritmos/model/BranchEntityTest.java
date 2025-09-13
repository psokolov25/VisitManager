package ru.aritmos.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

class BranchEntityTest {

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
    @Disabled("Not yet implemented")
    @Test
    void cloneTest() {
        // TODO implement
    }

}
