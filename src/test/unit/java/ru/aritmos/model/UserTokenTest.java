package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class UserTokenTest {

    @DisplayName("проверяется сценарий «builder and setters create user token»")
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