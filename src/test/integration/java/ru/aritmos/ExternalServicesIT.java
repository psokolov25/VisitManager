package ru.aritmos;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.keycloack.service.KeyCloackClient;

@MicronautTest(environments = "integration")
class ExternalServicesIT {

    @Inject DataBusClient dataBusClient;
    @Inject KeyCloackClient keyCloackClient;

    @DisplayName("проверяется сценарий «data bus client available»")
    @Test
    void dataBusClientAvailable() {
        assertNotNull(dataBusClient);
    }


    @DisplayName("проверяется сценарий «keycloak client available»")
    @Test
    void keycloakClientAvailable() {
        assertNotNull(keyCloackClient);
    }
}