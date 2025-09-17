package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.service.Configuration;

/**
 * Сквозной тест эндпоинтов {@link ServicePointController}.
 */
@MicronautTest(environments = "integration")
class ServicePointControllerE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    Configuration configuration;

    /** Проверяет получение свободных точек обслуживания. */
    @Test
    void fetchesFreeServicePoints() {
        Branch branch = new Branch("b1", "Branch");
        branch.getServicePoints().put("sp1", new ServicePoint("sp1", "SP"));
        Map<String, Branch> payload = Map.of("b1", branch);
        configuration.createBranchConfiguration(payload);

        Map<?,?> response = client.toBlocking().retrieve(
                HttpRequest.GET("/servicepoint/branches/b1/servicePoints/getFree"), Map.class);
        assertTrue(response.containsKey("sp1"));
    }
}
