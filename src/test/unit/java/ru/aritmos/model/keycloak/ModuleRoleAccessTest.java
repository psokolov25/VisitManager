package ru.aritmos.model.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleRoleAccessTest {

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
