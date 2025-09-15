package ru.aritmos.model.keycloak;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientAccessTest {

    @Test
    void билдерИСеттерыДаютОдинаковыйРезультат() {
        ClientAccess viaBuilder = ClientAccess.builder()
                .roles(Map.of("printer", List.of("print", "status")))
                .build();

        ClientAccess viaSetters = new ClientAccess();
        viaSetters.setRoles(Map.of("printer", List.of("print", "status")));

        assertEquals(viaBuilder, viaSetters);
        assertEquals(viaBuilder.hashCode(), viaSetters.hashCode());
        assertTrue(viaBuilder.toString().contains("printer"));
    }
}
