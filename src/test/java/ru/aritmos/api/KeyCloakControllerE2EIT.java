package ru.aritmos.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.aritmos.keycloack.model.Credentials;
import ru.aritmos.keycloack.service.KeyCloackClient;

/** E2E tests for {@link KeyCloakController}. */
@MicronautTest(environments = {"integration", "local-no-docker"})
@Disabled("Требует запущенное окружение")
class KeyCloakControllerE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    KeyCloackClient keyCloackClient;

    @Test
    void authDelegatesToClient() {
        Credentials credentials = new Credentials();
        when(keyCloackClient.Auth(credentials)).thenReturn(Optional.empty());

        HttpResponse<?> response = client.toBlocking().exchange(HttpRequest.POST("/keycloak", credentials));
        assertEquals(io.micronaut.http.HttpStatus.OK, response.getStatus());
        verify(keyCloackClient).Auth(credentials);
    }

    @Test
    void deleteSessionDelegatesToClient() {
        client.toBlocking().exchange(HttpRequest.POST("/keycloak/users/u1?isForced=true&reason=r", null));
        verify(keyCloackClient).userLogout("u1", true, "r");
    }

    @MockBean(KeyCloackClient.class)
    KeyCloackClient keyCloackClient() {
        return mock(KeyCloackClient.class);
    }
}
