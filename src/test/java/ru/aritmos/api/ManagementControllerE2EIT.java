package ru.aritmos.api;

import static org.junit.jupiter.api.Assertions.*;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import io.micronaut.test.annotation.MockBean;
import org.mockito.Mockito;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.tiny.TinyClass;

/**
 * Сквозные проверки {@link ManagementController}.
 */
@MicronautTest(environments = {"integration", "local-no-docker"})
@Disabled("Требует запущенное окружение")
class ManagementControllerE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void returnsBranchById() {
        Branch branch = new Branch("b1", "Branch");
        client.toBlocking().exchange(HttpRequest.POST("/configuration/branches", Map.of("b1", branch)));

        Branch fetched = client.toBlocking().retrieve(
                HttpRequest.GET("/managementinformation/branches/b1"), Branch.class);
        assertEquals("Branch", fetched.getName());
    }

    @Test
    void returnsTinyBranches() {
        Branch branch = new Branch("b2", "B2");
        client.toBlocking().exchange(HttpRequest.POST("/configuration/branches", Map.of("b2", branch)));

        TinyClass[] tiny = client.toBlocking().retrieve(
                HttpRequest.GET("/managementinformation/branches/tiny"), TinyClass[].class);
        assertTrue(tiny.length >= 1);
        assertTrue(() -> java.util.Arrays.stream(tiny).anyMatch(t -> t.getId().equals("b2")));
    }

    @MockBean(KeyCloackClient.class)
    KeyCloackClient keyCloackClient() {
        return Mockito.mock(KeyCloackClient.class);
    }
}
