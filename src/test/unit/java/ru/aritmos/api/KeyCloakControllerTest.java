package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import ru.aritmos.keycloack.model.Credentials;
import ru.aritmos.keycloack.service.KeyCloackClient;

class KeyCloakControllerTest {

    @DisplayName("Метод Auth делегирует авторизацию клиенту KeyCloackClient")
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

    @DisplayName("Метод DeleteSession делегирует завершение сессии клиенту KeyCloackClient")
    @Test
    void deleteSessionDelegatesToClient() {
        KeyCloakController controller = new KeyCloakController();
        KeyCloackClient client = mock(KeyCloackClient.class);
        controller.keyCloackClient = client;

        controller.DeleteSession("user", true, "r");
        verify(client).userLogout("user", true, "r");
    }
}
