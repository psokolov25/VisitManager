package ru.aritmos.model.visit;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class VisitEventTest {

    @DisplayName("Событие помечается как новое в рамках транзакции")
    @Test
    void testIsNewOfTransaction() {
        assertTrue(VisitEvent.isNewOfTransaction(VisitEvent.STOP_SERVING));
        assertFalse(VisitEvent.isNewOfTransaction(VisitEvent.CREATED));
    }

    @DisplayName("Событие распознаётся как фронтовое")
    @Test
    void testIsFrontEndEvent() {
        assertTrue(VisitEvent.isFrontEndEvent(VisitEvent.CREATED));
        assertFalse(VisitEvent.isFrontEndEvent(VisitEvent.VISIT_END_TRANSACTION));
    }

    @DisplayName("Событие учитывает признак игнорирования в статистике")
    @Test
    void testIsIgnoredInStat() {
        assertFalse(VisitEvent.isIgnoredInStat(VisitEvent.CREATED));
    }

    @DisplayName("Метод `getStatus` возвращает корректный статус события")
    @Test
    void testGetStatus() {
        assertEquals(TransactionCompletionStatus.STOP_SERVING, VisitEvent.getStatus(VisitEvent.STOP_SERVING));
        assertNull(VisitEvent.getStatus(VisitEvent.CREATED));
    }

    @DisplayName("Метод `canBeNext` определяет допустимость следующего события")
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

    @DisplayName("Метод `getState` возвращает состояние события")
    @Test
    void testGetState() {
        assertEquals(VisitState.CREATED, VisitEvent.CREATED.getState());
    }
}
