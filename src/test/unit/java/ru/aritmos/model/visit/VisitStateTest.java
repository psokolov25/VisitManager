package ru.aritmos.model.visit;

import static org.junit.jupiter.api.Assertions.*;

import io.micronaut.serde.annotation.Serdeable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisitStateTest {

    @Test
    @DisplayName("перечисление содержит последовательность состояний визита")
    void проверяемСписокСостояний() {
        VisitState[] ожидаемыеСостояния = {
            VisitState.CREATED,
            VisitState.WAITING_IN_QUEUE,
            VisitState.CALLED,
            VisitState.SERVING,
            VisitState.WAITING_IN_USER_POOL,
            VisitState.WAITING_IN_SERVICE_POOL,
            VisitState.END
        };
        assertArrayEquals(ожидаемыеСостояния, VisitState.values());
    }

    @Test
    @DisplayName("можно получить состояние по его имени")
    void получаемСостояниеПоИмени() {
        assertEquals(VisitState.WAITING_IN_QUEUE, VisitState.valueOf("WAITING_IN_QUEUE"));
    }

    @Test
    @DisplayName("перечисление VisitState также аннотировано Serdeable")
    void проверяемАннотациюSerdeable() {
        assertTrue(VisitState.class.isAnnotationPresent(Serdeable.class));
    }
}
