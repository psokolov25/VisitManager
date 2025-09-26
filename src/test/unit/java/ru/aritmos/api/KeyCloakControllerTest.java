package ru.aritmos.api;

import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import ru.aritmos.keycloack.model.Credentials;
import ru.aritmos.keycloack.service.KeyCloackClient;

class KeyCloakControllerTest {

    @DisplayName("Авторизация делегируется клиенту Keycloak")
    @Test
    void authDelegatesToClient() {
        KeyCloakController controller = new KeyCloakController();
        KeyCloackClient client = mock(KeyCloackClient.class);
        controller.keyCloackClient = client;
        Credentials credentials = new Credentials();
        AuthorizationResponse response = mock(AuthorizationResponse.class);
        Optional<AuthorizationResponse> expected = Optional.of(response);
        when(client.Auth(credentials)).thenReturn(expected);

        assertSame(expected, controller.Auth(credentials));
        verify(client).Auth(credentials);
    }

    @DisplayName("Удаление сессии делегируется клиенту Keycloak")
    @Test
    void deleteSessionDelegatesToClient() {
        KeyCloakController controller = new KeyCloakController();
        KeyCloackClient client = mock(KeyCloackClient.class);
        controller.keyCloackClient = client;

        controller.DeleteSession("user", true, "r");
        verify(client).userLogout("user", true, "r");
    }
}
