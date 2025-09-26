package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkProfileTest {

    @DisplayName("Конструктор с именем генерирует идентификатор")
    @Test
    void constructorWithNameShouldGenerateIdentifier() {
        WorkProfile profile = new WorkProfile("Operator");

        assertEquals("Operator", profile.getName());
        assertNotNull(profile.getId());
        assertFalse(profile.getId().isEmpty());
    }

    @DisplayName("Конструктор с явным идентификатором сохраняет значения без изменений")
    @Test
    void constructorWithExplicitIdShouldKeepValues() {
        WorkProfile profile = new WorkProfile("wp1", "Operator");
        profile.setBranchId("branch42");

        assertEquals("wp1", profile.getId());
        assertEquals("Operator", profile.getName());
        assertEquals("branch42", profile.getBranchId());
    }

    @DisplayName("Список идентификаторов очередей остаётся изменяемым")
    @Test
    void queueIdsShouldBeMutableList() {
        WorkProfile profile = new WorkProfile("wp1", "Operator");
        profile.getQueueIds().addAll(List.of("q1", "q2"));

        assertEquals(List.of("q1", "q2"), profile.getQueueIds());
    }
}
