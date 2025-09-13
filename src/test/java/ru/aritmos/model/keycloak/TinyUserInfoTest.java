package ru.aritmos.model.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

class TinyUserInfoTest {

    @Test
    void цепочкиМетодовФормируютПолеИСтроку() {
        TinyUserInfo expected = new TinyUserInfo()
                .name("Иванов Иван")
                .description("админ")
                .email("ivan@example.com")
                .login("ivan");

        TinyUserInfo actual = TinyUserInfo.builder()
                .name("Иванов Иван")
                .description("админ")
                .email("ivan@example.com")
                .login("ivan")
                .build();

        assertEquals(expected, actual);
        assertEquals(expected.hashCode(), actual.hashCode());
        assertTrue(expected.toString().contains("Иванов Иван"));
    }
    @Disabled("Not yet implemented")
    @Test
    void nameTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void descriptionTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void emailTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void loginTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void equalsTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void hashCodeTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void toStringTest() {
        // TODO implement
    }

}
