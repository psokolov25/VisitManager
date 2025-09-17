package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;

class RuleTest {

    @Test
    void constantsHaveDefaultValues() {
        assertNotNull(Rule.id, "идентификатор должен быть задан");
        assertFalse(Rule.id.isEmpty(), "идентификатор не должен быть пустым");
        assertEquals("", Rule.name, "имя правила по умолчанию пустое");
    }
}

