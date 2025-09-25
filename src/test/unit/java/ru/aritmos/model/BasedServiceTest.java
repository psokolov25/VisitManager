package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class BasedServiceTest {

    @DisplayName("проверяется сценарий «clone should clone outcome independently»")
    @Test
    void cloneShouldCloneOutcomeIndependently() {
        Outcome outcome = new Outcome("1", "name");
        outcome.setCode(10L);
        BasedService service = new BasedService("id", "service");
        service.setOutcome(outcome);

        BasedService clone = service.clone();
        clone.getOutcome().setCode(20L);

        assertNotSame(service.getOutcome(), clone.getOutcome());
        assertEquals(10L, service.getOutcome().getCode());
        assertEquals(20L, clone.getOutcome().getCode());
    }
}