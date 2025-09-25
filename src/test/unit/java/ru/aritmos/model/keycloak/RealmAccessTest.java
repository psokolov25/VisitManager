package ru.aritmos.model.keycloak;

import static ru.aritmos.test.LoggingAssertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RealmAccessTest {

    @Test
    void builderAndSettersFormRoleList() {
        RealmAccess viaBuilder = RealmAccess.builder()
                .roles(List.of("admin", "operator"))
                .build();

        RealmAccess viaSetters = new RealmAccess();
        viaSetters.setRoles(List.of("admin", "operator"));

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("admin"));
    }
}
