package ru.aritmos.model.visit;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.serde.annotation.Serdeable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisitStateTest {

    @Test
    @DisplayName("Перечисление содержит последовательность состояний визита")
    void verifyListOfStates() {
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
    @DisplayName("Состояние можно получить по его имени")
    void retrievesStateByName() {
        assertEquals(VisitState.WAITING_IN_QUEUE, VisitState.valueOf("WAITING_IN_QUEUE"));
    }

    @Test
    @DisplayName("Перечисление состояний визита также помечено аннотацией сериализации")
    void verifySerdeableAnnotation() {
        assertTrue(VisitState.class.isAnnotationPresent(Serdeable.class));
    }
}
