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
import ru.aritmos.model.*;
import ru.aritmos.service.Configuration;

/**
 * Сквозной тест для {@link EntrypointController}.
 */
@MicronautTest(environments = "integration")
class EntrypointControllerE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    Configuration configuration;

    /** Проверяет получение списка доступных услуг отделения. */
    @DisplayName("Fetches Available Services")
    @Test
    void fetchesAvailableServices() {
        Branch branch = new Branch("b1", "Branch");
        // Очередь и рабочий профиль
        Queue queue = new Queue("q1", "Queue", "Q", 1);
        branch.getQueues().put("q1", queue);
        WorkProfile wp = new WorkProfile("wp1", "WP");
        wp.getQueueIds().add("q1");
        branch.getWorkProfiles().put("wp1", wp);
        // Пользователь и точка обслуживания
        User user = new User();
        user.setId("u1");
        user.setName("User");
        user.setFirstName("User");
        user.setLastName("Test");
        user.setCurrentWorkProfileId("wp1");
        ServicePoint sp = new ServicePoint("sp1", "SP");
        sp.setUser(user);
        branch.getServicePoints().put("sp1", sp);
        branch.getUsers().put("u1", user);
        // Услуга
        Service service = new Service("s1", "Service", 1, "q1");
        branch.getServices().put("s1", service);

        Map<String, Branch> payload = Map.of("b1", branch);
        configuration.createBranchConfiguration(payload);

        Service[] services = client.toBlocking().retrieve(
                HttpRequest.GET("/entrypoint/branches/b1/services"), Service[].class);
        assertEquals(1, services.length);
        assertEquals("s1", services[0].getId());
        assertTrue(services[0].getIsAvailable());
    }
}
