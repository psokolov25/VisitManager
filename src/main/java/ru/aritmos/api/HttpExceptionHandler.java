package ru.aritmos.api;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.exceptions.ErrorExceptionHandler;
import io.micronaut.http.server.exceptions.ErrorResponseProcessorExceptionHandler;
import io.micronaut.http.server.exceptions.HttpStatusExceptionHandler;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.exceptions.SystemException;

/**
 * Глобальный обработчик HTTP‑ошибок.
 * Формирует единый ответ с кодом и сообщением об ошибке.
 * BusinessException порождает {@link HttpStatusException} и обрабатывается как клиентская ошибка.
 */
@Slf4j
@Singleton
@Produces
@Replaces({
    HttpStatusExceptionHandler.class,
    ErrorExceptionHandler.class,
    ErrorResponseProcessorExceptionHandler.class
})
public class HttpExceptionHandler
    implements io.micronaut.http.server.exceptions.ExceptionHandler<Throwable, HttpResponse<HttpExceptionHandler.ErrorResponse>> {

    /**
     * Преобразует исключение в HTTP‑ответ.
     *
     * @param request исходный HTTP‑запрос
     * @param exception перехваченное исключение
     * @return ответ с кодом и описанием ошибки
     */
    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, Throwable exception) {
        HttpStatus status;
        String message;

        if (exception instanceof HttpStatusException hse) {
            status = hse.getStatus();
            message = hse.getMessage();
        } else if (exception instanceof HttpClientResponseException hcre) {
            status = hcre.getStatus();
            message = hcre.getMessage();
        } else if (exception instanceof SystemException se) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = se.getMessage();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = exception.getMessage();
        }

        log.warn("HTTP error {} on {}: {}", status, request.getPath(), message);
        return HttpResponse.status(status)
            .body(new ErrorResponse(status.getCode(), message));
    }

    /** Ответ об ошибке с кодом и сообщением. */
    @Getter
    @AllArgsConstructor
    @Serdeable
    public static class ErrorResponse {
        /** Код HTTP‑статуса. */
        Integer statusCode;
        /** Текст ошибки. */
        String message;
    }
}
