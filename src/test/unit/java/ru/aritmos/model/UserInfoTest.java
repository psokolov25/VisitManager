package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

class UserInfoTest {

    @DisplayName("Builder и сеттеры формируют одинаковый результат")
    @Test
    void builderAndSettersBehaveTheSame() {
        RealmAccess access = RealmAccess.builder()
                .roles(java.util.List.of("admin"))
                .build();

        UserInfo viaBuilder = UserInfo.builder()
                .sub("sub-1")
                .realm_access(access)
                .name("Пользователь")
                .email("user@example.com")
                .description("оператор")
                .build();

        UserInfo viaSetters = new UserInfo();
        viaSetters.setSub("sub-1");
        viaSetters.setRealm_access(access);
        viaSetters.setName("Пользователь");
        viaSetters.setEmail("user@example.com");
        viaSetters.setDescription("оператор");

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("Пользователь"));
    }
}
