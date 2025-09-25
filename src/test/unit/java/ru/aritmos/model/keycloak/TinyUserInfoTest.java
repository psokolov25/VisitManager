package ru.aritmos.model.keycloak;

import org.junit.jupiter.api.Test;

import static ru.aritmos.test.LoggingAssertions.*;

class TinyUserInfoTest {

    @Test
    void methodChainsPopulateFieldsAndString() {
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
