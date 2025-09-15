package ru.aritmos.api;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.annotation.Produces;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Global handler for HTTP status exceptions producing unified error responses.
 */
@Singleton
@Produces
public class HttpExceptionHandler
    implements io.micronaut.http.server.exceptions.ExceptionHandler<HttpStatusException, HttpResponse<HttpExceptionHandler.ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, HttpStatusException exception) {
        return HttpResponse.status(exception.getStatus())
            .body(new ErrorResponse(exception.getMessage()));
    }

    /** Simple error response with a message field. */
    @Getter
    @AllArgsConstructor
    @Serdeable
    static class ErrorResponse {
        String message;
    }
}
