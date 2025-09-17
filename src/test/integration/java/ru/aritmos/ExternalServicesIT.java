package ru.aritmos;

import static org.junit.jupiter.api.Assertions.*;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.keycloack.service.KeyCloackClient;

@MicronautTest(environments = "integration")
class ExternalServicesIT {

    @Inject DataBusClient dataBusClient;
    @Inject KeyCloackClient keyCloackClient;

    @Test
    void dataBusClientAvailable() {
        assertNotNull(dataBusClient);
    }


    @Test
    void keycloakClientAvailable() {
        assertNotNull(keyCloackClient);
    }
}
