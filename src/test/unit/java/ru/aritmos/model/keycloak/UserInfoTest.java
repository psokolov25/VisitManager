package ru.aritmos.model.keycloak;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class UserInfoTest {

    @DisplayName("Построитель и методы установки заполняют все поля профиля")
    @Test
    void builderAndSettersSupportAllFields() {
        RealmAccess realmAccess = RealmAccess.builder()
                .roles(java.util.List.of("admin"))
                .build();

        UserInfo viaBuilder = UserInfo.builder()
                .sub("sub-1")
                .realm_access(realmAccess)
                .name("Иван Иванов")
                .email("ivan@example.com")
                .description("администратор")
                .build();

        UserInfo viaSetters = new UserInfo();
        viaSetters.setSub("sub-1");
        viaSetters.setRealm_access(realmAccess);
        viaSetters.setName("Иван Иванов");
        viaSetters.setEmail("ivan@example.com");
        viaSetters.setDescription("администратор");

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("Иван"));
    }
}
