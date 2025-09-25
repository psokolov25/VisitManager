package ru.aritmos.model.keycloak;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;

class UserTokenTest {

    @Test
    void builderAndSettersConstructObject() {
        UserInfo info = UserInfo.builder()
                .sub("sub")
                .name("Test User")
                .build();
        Token token = Token.builder()
                .access_token("access")
                .build();

        UserToken viaBuilder = UserToken.builder()
                .user(info)
                .tokenInfo(token)
                .build();

        UserToken viaSetters = new UserToken();
        viaSetters.setUser(info);
        viaSetters.setTokenInfo(token);

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("Test User"));
    }
}
