package ru.aritmos.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

class OutcomeTest {
    @Test
    void cloneCreatesIndependentCopy() {
        Outcome original = new Outcome("1", "name");
        original.setCode(42L);

        Outcome clone = original.clone();

        assertNotSame(original, clone);
        assertEquals(original.getCode(), clone.getCode());
        assertEquals(original.getName(), clone.getName());

        original.setCode(100L);
        assertNotEquals(original.getCode(), clone.getCode());
    }
    @Disabled("Not yet implemented")
    @Test
    void cloneTest() {
        // TODO implement
    }

}
