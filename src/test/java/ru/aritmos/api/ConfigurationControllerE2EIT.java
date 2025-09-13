package ru.aritmos.api;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end test of configuration update and retrieval flow. */
@MicronautTest(environments = {"integration", "local-no-docker"})
class ConfigurationControllerE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testUpdateAndFetchBreakReasons() {
        Branch branch = new Branch("b1", "Test Branch");
        branch.getBreakReasons().put("r1", "Lunch");
        Map<String, Branch> payload = Map.of("b1", branch);

        client.toBlocking().exchange(HttpRequest.POST("/configuration/branches", payload));

        Map<?,?> reasons = client.toBlocking().retrieve(
                HttpRequest.GET("/configuration/branches/b1/break/reasons"), Map.class);
        assertEquals("Lunch", reasons.get("r1"));
    }
}
