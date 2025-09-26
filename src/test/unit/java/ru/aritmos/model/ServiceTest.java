package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class ServiceTest {

    @DisplayName("Клонирование копирует поля и связанные карты независимо")
    @Test
    void cloneShouldCopyFieldsAndMapsIndependently() {
        Outcome outcome = new Outcome("1", "outcome");
        outcome.setCode(5L);
        Service service = new Service("s1", "Service", 30, "q1");
        service.setOutcome(outcome);
        service.getPossibleOutcomes().put("ok", outcome);
        DeliveredService delivered = new DeliveredService("d1", "Delivered");
        service.getDeliveredServices().put("d1", delivered);

        Service clone = service.clone();

        service.getDeliveredServices().clear();
        service.getPossibleOutcomes().clear();
        service.getOutcome().setCode(7L);

        assertNotSame(service, clone);
        assertEquals(30, clone.getServingSL());
        assertEquals("q1", clone.getLinkedQueueId());
        assertTrue(clone.getDeliveredServices().containsKey("d1"));
        assertTrue(clone.getPossibleOutcomes().containsKey("ok"));
        assertNotSame(service.getDeliveredServices(), clone.getDeliveredServices());
        assertNotSame(service.getPossibleOutcomes(), clone.getPossibleOutcomes());
        assertNotSame(service.getOutcome(), clone.getOutcome());
        assertEquals(5L, clone.getOutcome().getCode());
    }
}
