package ru.aritmos.model.visit;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.serde.annotation.Serdeable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionCompletionStatusTest {

    @Test
    @DisplayName("перечисление содержит полный список статусов транзакции")
    void verifyFullSetOfStatuses() {
        TransactionCompletionStatus[] ожидаемыеСтатусы = {
            TransactionCompletionStatus.OK,
            TransactionCompletionStatus.NO_SHOW,
            TransactionCompletionStatus.PLACED_IN_QUEUE,
            TransactionCompletionStatus.STOP_SERVING,
            TransactionCompletionStatus.REMOVED_BY_EMP,
            TransactionCompletionStatus.REMOVED_BY_CUSTOMER,
            TransactionCompletionStatus.REMOVED_BY_RESET,
            TransactionCompletionStatus.TRANSFER_TO_QUEUE,
            TransactionCompletionStatus.TRANSFER_TO_SERVICE_POINT_POOL,
            TransactionCompletionStatus.TRANSFER_TO_USER_POOL,
            TransactionCompletionStatus.LOGOUT,
            TransactionCompletionStatus.CLOSE_SP
        };
        assertArrayEquals(ожидаемыеСтатусы, TransactionCompletionStatus.values());
    }

    @Test
    @DisplayName("можно получить статус по его строковому имени")
    void retrievesStatusByName() {
        assertEquals(
            TransactionCompletionStatus.REMOVED_BY_EMP,
            TransactionCompletionStatus.valueOf("REMOVED_BY_EMP"));
    }

    @Test
    @DisplayName("перечисление помечено аннотацией Serdeable")
    void verifySerdeableAnnotation() {
        assertTrue(TransactionCompletionStatus.class.isAnnotationPresent(Serdeable.class));
    }
}
