package ru.aritmos.model.keycloak;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RealmAccessTest {

    @Test
    void билдерИСеттерыФормируютСписокРолей() {
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
