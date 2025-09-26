package ru.aritmos.model.keycloak;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModuleRoleAccessTest {

    @DisplayName("Тест")
    @Test
    void builderAndSettersWorkTheSame() {
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
