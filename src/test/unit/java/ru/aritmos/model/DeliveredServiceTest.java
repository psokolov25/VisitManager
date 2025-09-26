package ru.aritmos.model;

import org.junit.jupiter.api.Test;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

class DeliveredServiceTest {
    @DisplayName("Клонирование формирует независимую копию")
    @Test
    void cloneCreatesIndependentCopy() {
        Outcome outcome = new Outcome("o1", "outcome");
        outcome.setCode(7L);

        DeliveredService service = new DeliveredService("s1", "service");
        service.getServiceIds().add("svc");
        service.getPossibleOutcomes().put("o1", outcome);
        service.setOutcome(outcome);

        DeliveredService clone = service.clone();

        assertNotSame(service, clone);
        assertTrue(clone.getServiceIds().isEmpty());
        assertEquals(service.getPossibleOutcomes(), clone.getPossibleOutcomes());
        assertNotNull(clone.getOutcome());
        assertNotSame(service.getOutcome(), clone.getOutcome());
        assertEquals(service.getOutcome().getCode(), clone.getOutcome().getCode());

        service.getServiceIds().add("another");
        service.getOutcome().setCode(99L);

        assertTrue(clone.getServiceIds().isEmpty());
        assertEquals(7L, clone.getOutcome().getCode());
    }
}
