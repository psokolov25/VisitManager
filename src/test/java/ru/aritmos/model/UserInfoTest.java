package ru.aritmos.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserInfoTest {

    @Test
    void билдерИСеттерыРаботаютОдинаково() {
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
