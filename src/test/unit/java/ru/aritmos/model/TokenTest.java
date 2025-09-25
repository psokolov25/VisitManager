package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;

class TokenTest {

    @Test
    void builderAndSettersTransferTokenData() {
        Token viaBuilder = Token.builder()
                .access_token("access")
                .expires_at(100L)
                .refresh_expires_in(200L)
                .refresh_token("refresh")
                .session_state("state")
                .enabled(true)
                .id_token("id")
                .build();

        Token viaSetters = new Token();
        viaSetters.setAccess_token("access");
        viaSetters.setExpires_at(100L);
        viaSetters.setRefresh_expires_in(200L);
        viaSetters.setRefresh_token("refresh");
        viaSetters.setSession_state("state");
        viaSetters.setEnabled(true);
        viaSetters.setId_token("id");

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("access"));
    }
}
