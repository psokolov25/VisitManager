package ru.aritmos.api;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.http.server.exceptions.HttpStatusHandler;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Глобальный обработчик HTTP-исключений.
 * Возвращает унифицированный ответ и ведёт журнал ошибок.
 */
@Slf4j
@Singleton
@Replaces(HttpStatusHandler.class)
public class HttpErrorHandler implements ExceptionHandler<HttpStatusException, HttpResponse<?>> {

    private final ErrorResponseProcessor<?> errorResponseProcessor;

    public HttpErrorHandler(ErrorResponseProcessor<?> errorResponseProcessor) {
        this.errorResponseProcessor = errorResponseProcessor;
    }

    @Override
    public HttpResponse<?> handle(HttpRequest request, HttpStatusException exception) {
        log.error("HTTP error: {}", exception.getMessage(), exception);
        Map<String, String> body = Map.of("message", exception.getMessage());
        MutableHttpResponse<Map<String, String>> response =
                HttpResponse.status(exception.getStatus()).body(body);
        return errorResponseProcessor.processResponse(
                ErrorContext.builder(request)
                        .cause(exception)
                        .errorMessage(exception.getMessage())
                        .build(),
                response);
    }
}

