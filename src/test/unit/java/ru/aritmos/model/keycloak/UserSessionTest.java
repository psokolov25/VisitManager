package ru.aritmos.model.keycloak;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserSessionTest {

    @Test
    void билдерИСеттерыСоздаютПолнуюСессию() {
        UserToken token = UserToken.builder()
                .user(UserInfo.builder().sub("sub").build())
                .tokenInfo(Token.builder().access_token("access").build())
                .build();

        UserSession viaBuilder = UserSession.builder()
                .login("user")
                .create_session(1L)
                .last_update(2L)
                .userToken(token)
                .sid("sid-1")
                .build();

        UserSession viaSetters = new UserSession();
        viaSetters.setLogin("user");
        viaSetters.setCreate_session(1L);
        viaSetters.setLast_update(2L);
        viaSetters.setUserToken(token);
        viaSetters.setSid("sid-1");

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("sid-1"));
    }
}
