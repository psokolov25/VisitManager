package ru.aritmos.api;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import org.junit.jupiter.api.Test;

class HttpErrorHandlerTest {

    @Test
    void handleВозвращаетКодИТелоОшибки() {
        HttpErrorHandler handler = new HttpErrorHandler();
        HttpRequest<?> request = HttpRequest.GET("/test");
        HttpStatusException exception = new HttpStatusException(HttpStatus.CONFLICT, "Ошибка");

        HttpResponse<?> response = handler.handle(request, exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatus());
        HttpErrorHandler.ErrorResponse body = response.getBody(HttpErrorHandler.ErrorResponse.class)
            .orElseThrow();
        assertEquals(Integer.valueOf(HttpStatus.CONFLICT.getCode()), body.getCode());
        assertEquals("Ошибка", body.getMessage());
    }
}
