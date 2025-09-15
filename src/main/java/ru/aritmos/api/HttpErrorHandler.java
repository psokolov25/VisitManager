package ru.aritmos.api;


import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.HttpStatusHandler;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Глобальный обработчик HTTP-исключений.
 * Возвращает унифицированный ответ и ведёт журнал ошибок.
 */
@Slf4j
@Singleton

@Replaces(HttpStatusHandler.class)
public class HttpErrorHandler implements ExceptionHandler<HttpStatusException, HttpResponse<?>> {



    @Override
    public HttpResponse<?> handle(HttpRequest request, HttpStatusException exception) {
        log.error("HTTP error: {}", exception.getMessage(), exception);
        return HttpResponse.status(exception.getStatus())
                .body(new ErrorResponse(exception.getStatus().getCode(), exception.getMessage()));
    }

    /** Simple error response with a message field. */
    @Getter
    @AllArgsConstructor
    @Serdeable
    static class ErrorResponse {
        Integer code;
        String message;

    }
}

