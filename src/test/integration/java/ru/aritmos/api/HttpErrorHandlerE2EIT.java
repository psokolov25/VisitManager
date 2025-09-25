package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Проверка глобального обработчика HTTP-ошибок.
 */
@MicronautTest(environments = "integration")
class HttpErrorHandlerE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    @DisplayName("проверяется сценарий «returns unified error body»")
    @Test
    void returnsUnifiedErrorBody() {
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().retrieve(HttpRequest.GET("/managementinformation/branches/missing"), Map.class));
        assertEquals(404, ex.getStatus().getCode());
        Map body = ex.getResponse().getBody(Map.class).orElse(Map.of());
        assertEquals("Branch not found", body.get("message"));
    }
}
