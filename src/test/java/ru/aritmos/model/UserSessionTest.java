package ru.aritmos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class UserSessionTest {

    @Test
    void builderShouldProvideEmptyParamsByDefault() {
        UserSession session = UserSession.builder().login("ivan").build();

        assertEquals("ivan", session.getLogin());
        assertNotNull(session.getParams());
        assertTrue(session.getParams().isEmpty());
    }

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
