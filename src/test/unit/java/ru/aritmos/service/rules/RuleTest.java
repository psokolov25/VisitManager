package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class RuleTest {

    @DisplayName("проверяется сценарий «constants have default values»")
    @Test
    void constantsHaveDefaultValues() {
        assertNotNull(Rule.id, "идентификатор должен быть задан");
        assertFalse(Rule.id.isEmpty(), "идентификатор не должен быть пустым");
        assertEquals("", Rule.name, "имя правила по умолчанию пустое");
    }
}
