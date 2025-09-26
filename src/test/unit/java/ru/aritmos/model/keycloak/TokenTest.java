package ru.aritmos.model.keycloak;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenTest {

    @DisplayName("Построитель и сеттеры заполняют поля токена")
    @Test
    void builderAndSettersPopulateTokenFields() {
        Token viaBuilder = Token.builder()
                .access_token("access")
                .expires_at(60L)
                .refresh_expires_in(120L)
                .refresh_token("refresh")
                .session_state("state")
                .enabled(true)
                .id_token("id")
                .build();

        Token viaSetters = new Token();
        viaSetters.setAccess_token("access");
        viaSetters.setExpires_at(60L);
        viaSetters.setRefresh_expires_in(120L);
        viaSetters.setRefresh_token("refresh");
        viaSetters.setSession_state("state");
        viaSetters.setEnabled(true);
        viaSetters.setId_token("id");

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("access"));
    }
}
