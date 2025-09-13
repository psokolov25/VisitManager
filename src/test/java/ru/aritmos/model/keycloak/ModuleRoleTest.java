package ru.aritmos.model.keycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

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
    @Disabled("Not yet implemented")
    @Test
    void nameTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void roleTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void equalsTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void hashCodeTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void toStringTest() {
        // TODO implement
    }

}
