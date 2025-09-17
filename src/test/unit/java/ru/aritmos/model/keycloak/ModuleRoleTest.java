package ru.aritmos.model.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleRoleTest {

    @Test
    void цепочкиМетодовФормируютКорректныйОбъект() {
        ModuleRole direct = new ModuleRole()
                .name("module")
                .role("operator");

        ModuleRole built = new ModuleRole();
        built.setName("module");
        built.setRole("operator");

        assertEquals(direct, built);
        assertEquals(direct.hashCode(), built.hashCode());
        assertEquals("ModuleRole(name: module, role: operator)", direct.toString());
    }
}
