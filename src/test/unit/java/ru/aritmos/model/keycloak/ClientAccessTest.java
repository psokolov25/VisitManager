package ru.aritmos.model.keycloak;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientAccessTest {

    @DisplayName("Builder And Setters Produce Same Result")
    @Test
    void builderAndSettersProduceSameResult() {
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
