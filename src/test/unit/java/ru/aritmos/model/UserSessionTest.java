package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class UserSessionTest {

    @DisplayName("Билдер по умолчанию создаёт пустые параметры")
    @Test
    void builderShouldProvideEmptyParamsByDefault() {
        UserSession session = UserSession.builder().login("ivan").build();

        assertEquals("ivan", session.getLogin());
        assertNotNull(session.getParams());
        assertTrue(session.getParams().isEmpty());
    }

    @DisplayName("Сеттеры обновляют изменяемые поля")
    @Test
    void settersShouldUpdateMutableFields() {
        UserSession session = new UserSession();
        session.setLogin("petr");
        session.setSid("sid42");
        session.setParams(new HashMap<>());

        assertEquals("petr", session.getLogin());
        assertEquals("sid42", session.getSid());
        assertNotNull(session.getParams());
        assertTrue(session.getParams().isEmpty());
    }
}
