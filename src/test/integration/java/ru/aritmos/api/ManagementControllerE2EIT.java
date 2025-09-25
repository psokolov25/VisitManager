package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.tiny.TinyClass;
import ru.aritmos.service.Configuration;

/**
 * Сквозные проверки {@link ManagementController}.
 */
@MicronautTest(environments = "integration")
class ManagementControllerE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    Configuration configuration;

    @DisplayName("Возврат отделения по идентификатору")
    @Test
    void returnsBranchById() {
        Branch branch = new Branch("b1", "Branch");
        configuration.createBranchConfiguration(Map.of("b1", branch));

        Branch fetched = client.toBlocking().retrieve(
                HttpRequest.GET("/managementinformation/branches/b1"), Branch.class);
        assertEquals("Branch", fetched.getName());
    }

    @DisplayName("Возврат укороченного списка отделений")
    @Test
    void returnsTinyBranches() {
        Branch branch = new Branch("b2", "B2");
        configuration.createBranchConfiguration(Map.of("b2", branch));

        TinyClass[] tiny = client.toBlocking().retrieve(
                HttpRequest.GET("/managementinformation/branches/tiny"), TinyClass[].class);
        assertTrue(tiny.length >= 1);
        assertTrue(() -> java.util.Arrays.stream(tiny).anyMatch(t -> t.getId().equals("b2")));
    }

}
