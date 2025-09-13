package ru.aritmos.events.clients;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the real {@link DataBusClient} using the "integration" profile.
 */
@MicronautTest(environments = "integration")
class DataBusClientIT {

    @Inject
    DataBusClient dataBusClient;

    @Test
    void realServiceResponds() {
        Map<String, String> response = Mono.from(
                dataBusClient.send("dest", false, "2024-01-01", "vm", "TEST", Map.of())
        ).block(java.time.Duration.ofSeconds(5));

        assertNotNull(response);
        assertNotEquals("stubbed", response.get("status"));
    }
}
