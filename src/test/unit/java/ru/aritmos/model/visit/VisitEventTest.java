package ru.aritmos.model.visit;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class VisitEventTest {

    @DisplayName("Test Is New Of Transaction")
    @Test
    void testIsNewOfTransaction() {
        assertTrue(VisitEvent.isNewOfTransaction(VisitEvent.STOP_SERVING));
        assertFalse(VisitEvent.isNewOfTransaction(VisitEvent.CREATED));
    }

    @DisplayName("Test Is Front End Event")
    @Test
    void testIsFrontEndEvent() {
        assertTrue(VisitEvent.isFrontEndEvent(VisitEvent.CREATED));
        assertFalse(VisitEvent.isFrontEndEvent(VisitEvent.VISIT_END_TRANSACTION));
    }

    @DisplayName("Test Is Ignored In Stat")
    @Test
    void testIsIgnoredInStat() {
        assertFalse(VisitEvent.isIgnoredInStat(VisitEvent.CREATED));
    }

    @DisplayName("Test Get Status")
    @Test
    void testGetStatus() {
        assertEquals(TransactionCompletionStatus.STOP_SERVING, VisitEvent.getStatus(VisitEvent.STOP_SERVING));
        assertNull(VisitEvent.getStatus(VisitEvent.CREATED));
    }

    @DisplayName("Test Can Be Next")
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

    @DisplayName("Test Get State")
    @Test
    void testGetState() {
        assertEquals(VisitState.CREATED, VisitEvent.CREATED.getState());
    }
}
