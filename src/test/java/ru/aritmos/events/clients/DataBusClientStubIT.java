package ru.aritmos.events.clients;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import ru.aritmos.events.clients.DataBusClient;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест заглушки клиента DataBus в профиле "local-no-docker".
 */
@MicronautTest(environments = "local-no-docker")
class DataBusClientStubIT {

    @Inject
    DataBusClient dataBusClient;

    /** Заглушка возвращает предопределённый ответ. */
    @Test
    void stubReturnsPredefinedResponse() {
        Map<String, String> response = Mono.from(
                dataBusClient.send("dest", false, "2024-01-01", "vm", "TEST", Map.of())
        ).block();

        assertEquals("stubbed", response.get("status"));
        assertEquals("TEST", response.get("type"));
    }
}
