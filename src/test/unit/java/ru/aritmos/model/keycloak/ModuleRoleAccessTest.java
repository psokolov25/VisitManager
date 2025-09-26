package ru.aritmos.model.keycloak;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static ru.aritmos.test.LoggingAssertions.*;

class ModuleRoleAccessTest {

    @DisplayName("Построитель и цепочка методов создают одинаковый объект доступа к роли")
    @Test
    void builderИСеттерыРаботаютОдинаково() {
        ModuleRoleAccess viaBuilder = ModuleRoleAccess.builder()
                .name("module")
                .role("operator")
                .access(true)
                .build();

        ModuleRoleAccess viaSetters = new ModuleRoleAccess()
                .name("module")
                .role("operator")
                .access(true);

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("access: true"));
    }
}
