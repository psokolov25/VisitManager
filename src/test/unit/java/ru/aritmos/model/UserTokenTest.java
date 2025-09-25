package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class UserTokenTest {

    @DisplayName("Builder And Setters Create User Token")
    @Test
    void builderAndSettersCreateUserToken() {
        UserInfo info = UserInfo.builder()
                .sub("sub")
                .name("User")
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
        assertTrue(viaBuilder.toString().contains("User"));
    }
}
