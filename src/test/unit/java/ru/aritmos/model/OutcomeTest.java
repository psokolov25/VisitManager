package ru.aritmos.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static ru.aritmos.test.LoggingAssertions.*;

class OutcomeTest {
    @DisplayName("проверяется сценарий «clone creates independent copy»")
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
}