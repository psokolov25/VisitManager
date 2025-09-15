package ru.aritmos.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.SystemException;

@MicronautTest
@Property(name = "spec.name", value = "HttpExceptionHandlerTest")
class HttpExceptionHandlerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void handlesHttpStatusException() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(
                HttpRequest.GET("/test-handler"),
                HttpExceptionHandler.ErrorResponse.class
            )
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatus());
        HttpExceptionHandler.ErrorResponse body = ex.getResponse()
            .getBody(HttpExceptionHandler.ErrorResponse.class)
            .orElseThrow();
        assertEquals(HttpStatus.BAD_REQUEST.getCode(), body.getStatusCode());
        assertEquals("boom", body.getMessage());
    }

    @Test
    void handlesSystemException() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(
                HttpRequest.GET("/test-handler/system"),
                HttpExceptionHandler.ErrorResponse.class
            )
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getResponse().getStatus());
        HttpExceptionHandler.ErrorResponse body = ex.getResponse()
            .getBody(HttpExceptionHandler.ErrorResponse.class)
            .orElseThrow();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), body.getStatusCode());
        assertEquals("sysboom", body.getMessage());
    }

    @Requires(property = "spec.name", value = "HttpExceptionHandlerTest")
    @Controller("/test-handler")
    static class FailingController {
        @Get
        void trigger() {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "boom");
        }

        @Get("/system")
        void triggerSystem() throws SystemException {
            throw new SystemException("sysboom", mock(EventService.class));
        }
    }
}
