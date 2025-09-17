package ru.aritmos.model.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
