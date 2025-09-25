package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class UserSessionTest {

    @DisplayName("проверяется сценарий «builder should provide empty params by default»")
    @Test
    void builderShouldProvideEmptyParamsByDefault() {
        UserSession session = UserSession.builder().login("ivan").build();

        assertEquals("ivan", session.getLogin());
        assertNotNull(session.getParams());
        assertTrue(session.getParams().isEmpty());
    }

    @DisplayName("проверяется сценарий «setters should update mutable fields»")
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