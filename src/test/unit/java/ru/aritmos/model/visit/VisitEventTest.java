package ru.aritmos.model.visit;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class VisitEventTest {

    @DisplayName("Проверяет, что событие считается новым в транзакции")
    @Test
    void testIsNewOfTransaction() {
        assertTrue(VisitEvent.isNewOfTransaction(VisitEvent.STOP_SERVING));
        assertFalse(VisitEvent.isNewOfTransaction(VisitEvent.CREATED));
    }

    @DisplayName("Проверяет принадлежность события фронтовой категории")
    @Test
    void testIsFrontEndEvent() {
        assertTrue(VisitEvent.isFrontEndEvent(VisitEvent.CREATED));
        assertFalse(VisitEvent.isFrontEndEvent(VisitEvent.VISIT_END_TRANSACTION));
    }

    @DisplayName("Проверяет игнорирование события в статистике")
    @Test
    void testIsIgnoredInStat() {
        assertFalse(VisitEvent.isIgnoredInStat(VisitEvent.CREATED));
    }

    @DisplayName("Проверяет получение статуса события")
    @Test
    void testGetStatus() {
        assertEquals(TransactionCompletionStatus.STOP_SERVING, VisitEvent.getStatus(VisitEvent.STOP_SERVING));
        assertNull(VisitEvent.getStatus(VisitEvent.CREATED));
    }

    @DisplayName("Проверяет допустимость последующего события")
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

    @DisplayName("Проверяет получение состояния для события")
    @Test
    void testGetState() {
        assertEquals(VisitState.CREATED, VisitEvent.CREATED.getState());
    }
}
