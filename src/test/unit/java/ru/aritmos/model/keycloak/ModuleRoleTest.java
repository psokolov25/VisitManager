package ru.aritmos.model.keycloak;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static ru.aritmos.test.LoggingAssertions.*;

class ModuleRoleTest {

    @DisplayName("проверяется сценарий «method chains produce correct object»")
    @Test
    void methodChainsProduceCorrectObject() {
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