package ru.aritmos.api;

import static org.junit.jupiter.api.Assertions.*;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;

/**
 * Сквозной тест {@link ConfigurationController}, проверяющий получение причин перерыва.
 */
@MicronautTest(environments = {"integration", "local-no-docker"})
class ConfigurationControllerE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    /** Проверяет, что причины перерыва возвращаются после обновления конфигурации. */
    @Test
    void returnsBreakReasons() {
        Branch branch = new Branch("b1", "Branch");
        branch.getBreakReasons().put("b1", "Break");
        Map<String, Branch> payload = Map.of("b1", branch);
        client.toBlocking().exchange(HttpRequest.POST("/configuration/branches", payload));

        Map<?, ?> response = client.toBlocking().retrieve(
                HttpRequest.GET("/configuration/branches/b1/break/reasons"), Map.class);
        assertEquals("Break", response.get("b1"));
    }
}
