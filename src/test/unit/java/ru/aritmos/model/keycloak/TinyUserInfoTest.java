package ru.aritmos.model.keycloak;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TinyUserInfoTest {

    @DisplayName("Цепочка методов заполняет поля и строковое представление")
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
