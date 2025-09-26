package ru.aritmos;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.keycloack.service.KeyCloackClient;

@MicronautTest(environments = "integration")
class ExternalServicesIT {

    @Inject DataBusClient dataBusClient;
    @Inject KeyCloackClient keyCloackClient;

    @DisplayName("Клиент шины данных доступен в интеграционном контексте")
    @Test
    void dataBusClientAvailable() {
        assertNotNull(dataBusClient);
    }


    @DisplayName("Клиент Keycloak доступен в интеграционном окружении теста")
    @Test
    void keycloakClientAvailable() {
        assertNotNull(keyCloackClient);
    }
}
