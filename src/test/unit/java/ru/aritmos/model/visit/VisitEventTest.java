package ru.aritmos.model.visit;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class VisitEventTest {

    @DisplayName("проверяется сценарий «test is new of transaction»")
    @Test
    void testIsNewOfTransaction() {
        assertTrue(VisitEvent.isNewOfTransaction(VisitEvent.STOP_SERVING));
        assertFalse(VisitEvent.isNewOfTransaction(VisitEvent.CREATED));
    }

    @DisplayName("проверяется сценарий «test is front end event»")
    @Test
    void testIsFrontEndEvent() {
        assertTrue(VisitEvent.isFrontEndEvent(VisitEvent.CREATED));
        assertFalse(VisitEvent.isFrontEndEvent(VisitEvent.VISIT_END_TRANSACTION));
    }

    @DisplayName("проверяется сценарий «test is ignored in stat»")
    @Test
    void testIsIgnoredInStat() {
        assertFalse(VisitEvent.isIgnoredInStat(VisitEvent.CREATED));
    }

    @DisplayName("проверяется сценарий «test get status»")
    @Test
    void testGetStatus() {
        assertEquals(TransactionCompletionStatus.STOP_SERVING, VisitEvent.getStatus(VisitEvent.STOP_SERVING));
        assertNull(VisitEvent.getStatus(VisitEvent.CREATED));
    }

    @DisplayName("проверяется сценарий «test can be next»")
    @Test
    void testCanBeNext() {
        VisitEvent current = VisitEvent.CREATED;
        current.getState();
        VisitEvent nextAllowed = VisitEvent.PLACED_IN_QUEUE;
        nextAllowed.getState();
        VisitEvent nextForbidden = VisitEvent.END;
        nextForbidden.getState();
        assertTrue(current.canBeNext(nextAllowed));
        assertFalse(current.canBeNext(nextForbidden));
    }

    @DisplayName("проверяется сценарий «test get state»")
    @Test
    void testGetState() {
        assertEquals(VisitState.CREATED, VisitEvent.CREATED.getState());
    }
}