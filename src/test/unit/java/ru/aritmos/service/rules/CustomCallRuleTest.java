package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.rules.client.CallRuleClient;

/**
 * Юнит-тест для {@link CustomCallRule}, проверяющий делегирование клиенту
 * и поведение методов-заглушек.
 */
class CustomCallRuleTest {

    /** Проверяет, что вызов делегируется клиенту правил. */
    @DisplayName("Вызов правила делегирует выбор визита клиенту правил")
    @Test
    void delegatesCallToClient() {
        CallRuleClient client = mock(CallRuleClient.class);
        CustomCallRule rule = new CustomCallRule();
        rule.callRuleClient = client;
        Branch branch = new Branch("b1", "Branch");
        ServicePoint sp = new ServicePoint("sp1", "SP");
        Visit visit = Visit.builder().id("v1").build();
        when(client.callRule(branch, sp)).thenReturn(Optional.of(visit));

        Optional<Visit> result = rule.call(branch, sp);
        assertTrue(result.isPresent());
        assertEquals(visit, result.get());
        verify(client).callRule(branch, sp);
    }

    /** Метод с идентификаторами очередей пока не реализован. */
    @DisplayName("Вызов правила по списку очередей возвращает пустой результат")
    @Test
    void callWithQueueIdsReturnsEmpty() {
        CustomCallRule rule = new CustomCallRule();
        Optional<Visit> result = rule.call(new Branch("b1", "Branch"), new ServicePoint("sp1", "SP"), List.of("q1"));
        assertTrue(result.isEmpty());
    }

    /** Метод получения доступных точек обслуживания возвращает пустой список. */
    @DisplayName("Определение доступных точек возвращает пустой список")
    @Test
    void getAvailiableServicePointsReturnsEmpty() {
        CustomCallRule rule = new CustomCallRule();
        assertTrue(rule.getAvailiableServicePoints(new Branch("b1", "Branch"), Visit.builder().id("v1").build()).isEmpty());
    }
}
